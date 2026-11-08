package com.geurys.enfoque

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat

/**
 * Se entera de qué app pasa al frente (solo el nombre del paquete, no lee contenido)
 * y la bloquea si es una app distractora que superó su límite o si hay un Enfoque activo.
 */
class BlockerService : AccessibilityService() {
    private var lastBlock = 0L
    private val lastWarn = mutableMapOf<String, Long>()

    // El uso se consulta como mucho cada 10 s (la consulta al sistema es pesada).
    private var usageCache: Map<String, Long> = emptyMap()
    private var usageAt = -60_000L
    private fun usedToday(pkg: String): Long {
        val now = SystemClock.elapsedRealtime()
        if (now - usageAt > 10_000) { usageCache = Usage.todayMillis(this); usageAt = now }
        return usageCache[pkg] ?: 0L
    }

    // USER_PRESENT no se puede declarar en el manifest; el servicio vive siempre, así que lo registra aquí.
    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = Prefs(context).addUnlock()
    }

    // Revisa cada 15 s si empezó o terminó el Enfoque (sesión manual u horario) para ajustar los grises.
    private val handler = Handler(Looper.getMainLooper())
    private val grayTick = object : Runnable {
        override fun run() {
            Grayscale.sync(this@BlockerService)
            handler.postDelayed(this, 15_000)
        }
    }

    override fun onServiceConnected() {
        handler.removeCallbacks(grayTick)
        handler.post(grayTick)
        ContextCompat.registerReceiver(
            this, unlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        handler.removeCallbacks(grayTick)
        runCatching { unregisterReceiver(unlockReceiver) }
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val prefs = Prefs(this)
        val limitMinutes = prefs.limits()[pkg] ?: return // no es una app distractora

        val focus = prefs.focusActive
        if (!focus) { // en Enfoque se bloquea directo, sin consultar el uso
            val usedMs = usedToday(pkg)
            val limitMs = limitMinutes * 60_000L
            if (usedMs < limitMs) {
                warnIfClose(pkg, usedMs, limitMs)
                return
            }
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastBlock < 1500) return // evita bucles de bloqueo
        lastBlock = now

        performGlobalAction(GLOBAL_ACTION_HOME)
        startActivity(
            Intent(this, BlockedActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockedActivity.EXTRA_PKG, pkg)
                .putExtra(BlockedActivity.EXTRA_FOCUS, focus)
        )
    }

    /** Aviso al abrir una app cuando ya gastaste el 80 % de su límite. */
    private fun warnIfClose(pkg: String, usedMs: Long, limitMs: Long) {
        if (usedMs < limitMs * 0.8) return
        val now = SystemClock.elapsedRealtime()
        if (now - (lastWarn[pkg] ?: 0L) < 60_000) return
        lastWarn[pkg] = now
        val left = ((limitMs - usedMs) / 60_000L).coerceAtLeast(1)
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        Toast.makeText(this, "Te quedan $left min en $label hoy", Toast.LENGTH_LONG).show()
    }

    override fun onInterrupt() = Unit
}
