package com.geurys.enfoque

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Lee las notificaciones de WhatsApp (solo texto) para poder responder desde la pestaña Mensajes. */
class NotificationService : NotificationListenerService() {

    @Suppress("DEPRECATION")
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in WHATSAPP) return
        val n = sbn.notification
        if (n.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT) != 0) return

        val extras = n.extras
        val convo = (extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString()?.takeIf { it.isNotBlank() } ?: return

        val raw = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (raw != null) {
            Notification.MessagingStyle.Message.getMessagesFromBundleArray(raw).forEach { m ->
                val sender = m.sender?.toString().orEmpty()
                val text = m.text?.toString().orEmpty()
                // Sin remitente = mensaje propio; ya lo registramos al responder.
                if (sender.isNotBlank() && text.isNotBlank()) {
                    Inbox.add(this, Msg(convo, sender, text, m.timestamp, false))
                }
            }
        } else {
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (text.isNotBlank()) Inbox.add(this, Msg(convo, convo, text, sbn.postTime, false))
        }

        // Acción "Responder" (con campo de texto) para contestar sin abrir WhatsApp.
        val actions = n.actions.orEmpty().toList() + Notification.WearableExtender(n).actions
        for (a in actions) {
            val input = a.remoteInputs?.firstOrNull { it.allowFreeFormInput } ?: continue
            Inbox.setTarget(convo, Inbox.Target(sbn.key, a, input))
            break
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName in WHATSAPP) Inbox.dropTarget(sbn.key)
    }

    companion object {
        val WHATSAPP = setOf("com.whatsapp", "com.whatsapp.w4b")
    }
}
