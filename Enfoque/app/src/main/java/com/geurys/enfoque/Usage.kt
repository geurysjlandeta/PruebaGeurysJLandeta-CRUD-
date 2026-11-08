package com.geurys.enfoque

import android.app.AppOpsManager
import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Usage {
    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        )
        // En algunos celulares (Xiaomi, etc.) el sistema responde MODE_DEFAULT aunque el permiso esté dado.
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            ctx.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else mode == AppOpsManager.MODE_ALLOWED
    }

    fun isBlockerEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        // El sistema puede guardar el nombre completo ("pkg/pkg.Clase") o el corto ("pkg/.Clase"): comparar como componente.
        val me = ComponentName(ctx, BlockerService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun hasNotificationAccess(ctx: Context) =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)

    fun notificationAccessIntent() = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    /** Milisegundos en primer plano hoy (desde las 00:00), por paquete. */
    fun todayMillis(ctx: Context): Map<String, Long> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return usm.queryAndAggregateUsageStats(start, System.currentTimeMillis())
            .mapValues { it.value.totalTimeInForeground }
            .filterValues { it > 0 }
    }

    /** Igual que [todayMillis] pero fuera del hilo principal. */
    suspend fun todayAsync(ctx: Context): Map<String, Long> = withContext(Dispatchers.IO) { todayMillis(ctx) }

    /** Abre directo la entrada de Enfoque (Android 10+); si el celular no lo soporta, abre la lista general. */
    fun usageSettingsIntent(ctx: Context) =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    /** Ficha de la app: ahí está "Permitir ajustes restringidos" (⋮) cuando el interruptor sale gris. */
    fun appDetailsIntent(ctx: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))

    fun startSafely(ctx: Context, intent: Intent, fallback: Intent? = null) {
        try { ctx.startActivity(intent) }
        catch (e: Exception) { runCatching { ctx.startActivity(fallback ?: appDetailsIntent(ctx)) } }
    }
    fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
