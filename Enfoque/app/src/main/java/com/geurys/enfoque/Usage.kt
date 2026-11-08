package com.geurys.enfoque

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

object Usage {
    fun hasUsageAccess(ctx: Context): Boolean {
        val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = ops.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isBlockerEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(
            ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val me = "${ctx.packageName}/${BlockerService::class.java.name}"
        return enabled.split(':').any { it.equals(me, ignoreCase = true) }
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

    fun usageSettingsIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
