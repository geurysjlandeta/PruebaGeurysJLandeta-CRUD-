package com.geurys.enfoque

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Fuente opcional: acceso a notificaciones del sistema. Por defecto se usa el servicio de accesibilidad. */
class NotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        connected = true
        syncActive()
        Inbox.version.intValue++
    }

    override fun onListenerDisconnected() {
        connected = false
        instance = null
        Inbox.version.intValue++
        rebind(this)
    }

    override fun onDestroy() {
        connected = false
        instance = null
        super.onDestroy()
    }

    /** Vuelve a leer las notificaciones activas de WhatsApp. */
    fun syncActive() {
        runCatching { activeNotifications?.forEach { handle(it) } }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = handle(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName in WHATSAPP) Inbox.dropTarget(sbn.key)
    }

    private fun handle(sbn: StatusBarNotification) {
        if (sbn.packageName !in WHATSAPP) return
        NotifParser.process(this, sbn.notification, sbn.key, "notificaciones")
    }

    companion object {
        val WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")

        @Volatile var connected = false
        @Volatile var instance: NotificationService? = null

        /** Pide al sistema que vuelva a conectar el servicio. */
        fun rebind(ctx: Context) {
            runCatching {
                NotificationListenerService.requestRebind(ComponentName(ctx, NotificationService::class.java))
            }
        }
    }
}
