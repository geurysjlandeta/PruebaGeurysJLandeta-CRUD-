package com.geurys.enfoque

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Se entera de qué app pasa al frente (solo el nombre del paquete, no lee contenido)
 * y la bloquea si es una app distractora que superó su límite o si hay un Enfoque activo.
 */
class BlockerService : AccessibilityService() {
    private var lastBlock = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val prefs = Prefs(this)
        val limitMinutes = prefs.limits()[pkg] ?: return // no es una app distractora

        val focus = prefs.focusActive
        val overLimit = (Usage.todayMillis(this)[pkg] ?: 0L) >= limitMinutes * 60_000L
        if (!focus && !overLimit) return

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

    override fun onInterrupt() = Unit
}
