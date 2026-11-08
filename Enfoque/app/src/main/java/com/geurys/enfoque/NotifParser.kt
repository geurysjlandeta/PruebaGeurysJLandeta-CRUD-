package com.geurys.enfoque

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.Person

/**
 * Convierte una notificación de WhatsApp en mensajes de la bandeja. Lo usan las dos fuentes:
 * el servicio de accesibilidad (no necesita permisos extra) y el acceso a notificaciones (opcional).
 */
object NotifParser {
    @Volatile var lastSeenAt = 0L
    @Volatile var lastInfo = ""
    @Volatile var lastSource = ""

    /** Títulos de avisos internos de WhatsApp ("Checking for new messages", resúmenes, etc.), no son chats. */
    val SYSTEM_TITLES = setOf("whatsapp", "whatsapp business")

    fun process(ctx: Context, n: Notification, key: String, source: String) {
        if (n.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_ONGOING_EVENT) != 0) return

        val postTime = n.`when`.takeIf { it > 0 } ?: System.currentTimeMillis()
        val extras = n.extras ?: Bundle()
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val convo = (style?.conversationTitle
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE))?.toString()?.takeIf { it.isNotBlank() }
        if (convo == null) {
            lastSeenAt = System.currentTimeMillis(); lastSource = source
            lastInfo = "sin título (no se pudo leer)"
            Inbox.version.intValue++
            return
        }
        if (convo.trim().lowercase() in SYSTEM_TITLES) return

        lastSeenAt = System.currentTimeMillis()
        lastSource = source

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
                        Inbox.add(ctx, Msg(convo, m.person?.name?.toString().orEmpty(), text, m.timestamp, false))
                    ) added++
                }
            } else {
                // WhatsApp no siempre manda el remitente: si nada parece ajeno, tómalos como recibidos
                // (salvo el eco de lo que acabamos de enviar nosotros).
                for (m in style.messages) {
                    val text = m.text?.toString().orEmpty()
                    if (text.isNotBlank() && !Inbox.isOwnEcho(convo, text, m.timestamp) &&
                        Inbox.add(ctx, Msg(convo, convo, text, m.timestamp, false))
                    ) added++
                }
            }
        } else {
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (text.isNotBlank() && !Inbox.isOwnEcho(convo, text, postTime) &&
                Inbox.add(ctx, Msg(convo, convo, text, postTime, false))
            ) added++
        }

        // Acción "Responder" (con campo de texto) para contestar sin abrir WhatsApp.
        var canReply = false
        val actions = n.actions.orEmpty().toList() + Notification.WearableExtender(n).actions
        for (a in actions) {
            val input = a.remoteInputs?.firstOrNull { it.allowFreeFormInput } ?: continue
            Inbox.setTarget(convo, Inbox.Target(key, a, input))
            canReply = true
            break
        }
        lastInfo = "$convo · $added nuevos · responder: ${if (canReply) "sí" else "no"}"
        Inbox.version.intValue++
    }
}
