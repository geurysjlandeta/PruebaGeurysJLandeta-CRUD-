package com.geurys.enfoque

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/** Lee las notificaciones de WhatsApp (solo texto) para poder responder desde Mensajes. */
class NotificationService : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        connected = true
        syncActive() // al conectar, lee también lo que ya estaba en la barra de notificaciones
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
        val n = sbn.notification
        lastSeenAt = System.currentTimeMillis()
        if (n.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT) != 0) {
            lastInfo = "resumen ignorado"
            Inbox.version.intValue++
            return
        }

        val extras = n.extras
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val convo = (style?.conversationTitle
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString()?.takeIf { it.isNotBlank() } ?: return

        var added = 0
        if (style != null && style.messages.isNotEmpty()) {
            val me = style.user
            fun isSelf(p: Person?) = p == null ||
                (p.key != null && p.key == me.key) ||
                (p.name != null && p.name.toString() == me.name?.toString())
            val incoming = style.messages.filter { !isSelf(it.person) }
            if (incoming.isNotEmpty()) {
                for (m in incoming) {
                    val text = m.text?.toString().orEmpty()
                    if (text.isNotBlank() &&
                        Inbox.add(this, Msg(convo, m.person?.name?.toString().orEmpty(), text, m.timestamp, false))
                    ) added++
                }
            } else {
                // WhatsApp no siempre manda el remitente: si nada parece ajeno, tómalos como recibidos
                // (salvo el eco de lo que acabamos de enviar nosotros).
                for (m in style.messages) {
                    val text = m.text?.toString().orEmpty()
                    if (text.isNotBlank() && !Inbox.isOwnEcho(convo, text, m.timestamp) &&
                        Inbox.add(this, Msg(convo, convo, text, m.timestamp, false))
                    ) added++
                }
            }
        } else {
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (text.isNotBlank() && !Inbox.isOwnEcho(convo, text, sbn.postTime) &&
                Inbox.add(this, Msg(convo, convo, text, sbn.postTime, false))
            ) added++
        }

        // Acción "Responder" (con campo de texto) para contestar sin abrir WhatsApp.
        var canReply = false
        val actions = n.actions.orEmpty().toList() + Notification.WearableExtender(n).actions
        for (a in actions) {
            val input = a.remoteInputs?.firstOrNull { it.allowFreeFormInput } ?: continue
            Inbox.setTarget(convo, Inbox.Target(sbn.key, a, input))
            canReply = true
            break
        }
        lastInfo = "$convo · $added nuevos · responder: ${if (canReply) "sí" else "no"}"
        Inbox.version.intValue++
    }

    companion object {
        val WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")

        @Volatile var connected = false
        @Volatile var instance: NotificationService? = null
        @Volatile var lastSeenAt = 0L
        @Volatile var lastInfo = ""

        /** Pide al sistema que vuelva a conectar el servicio (útil tras actualizar o si el sistema lo cerró). */
        fun rebind(ctx: Context) {
            runCatching {
                NotificationListenerService.requestRebind(ComponentName(ctx, NotificationService::class.java))
            }
        }
    }
}
