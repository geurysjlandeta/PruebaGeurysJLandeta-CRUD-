package com.geurys.enfoque

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject

data class Msg(val convo: String, val sender: String, val text: String, val time: Long, val mine: Boolean)

/**
 * Bandeja de mensajes de WhatsApp tomados de las notificaciones. Solo texto.
 * Los mensajes se guardan solo en este celular (últimos 300).
 */
object Inbox {
    /** Sube cada vez que cambia algo, para que la pantalla se refresque. */
    val version = mutableIntStateOf(0)

    class Target(val key: String, val action: Notification.Action, val input: RemoteInput)

    private val msgs = mutableListOf<Msg>()
    private val seen = HashSet<String>()
    private val targets = HashMap<String, Target>() // conversación -> acción "Responder" viva
    private var loaded = false

    @Synchronized
    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        val raw = ctx.getSharedPreferences("inbox", Context.MODE_PRIVATE).getString("msgs", null) ?: return
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val m = Msg(o.getString("c"), o.optString("s"), o.getString("x"), o.getLong("t"), o.optBoolean("m"))
                if (m.convo.trim().lowercase() in NotifParser.SYSTEM_TITLES) continue
                if (seen.add(keyOf(m))) msgs.add(m)
            }
        }
    }

    private fun keyOf(m: Msg) = "${m.convo}|${m.time}|${m.text}"

    @Synchronized
    fun add(ctx: Context, m: Msg): Boolean {
        load(ctx)
        if (!seen.add(keyOf(m))) return false
        msgs.add(m)
        while (msgs.size > 300) seen.remove(keyOf(msgs.removeAt(0)))
        val arr = JSONArray()
        msgs.forEach {
            arr.put(JSONObject().put("c", it.convo).put("s", it.sender).put("x", it.text).put("t", it.time).put("m", it.mine))
        }
        ctx.getSharedPreferences("inbox", Context.MODE_PRIVATE).edit().putString("msgs", arr.toString()).apply()
        version.intValue++
        return true
    }

    /** ¿Este texto es el eco de un mensaje que acabamos de enviar nosotros? */
    @Synchronized
    fun isOwnEcho(convo: String, text: String, time: Long) =
        msgs.any { it.mine && it.convo == convo && it.text == text && kotlin.math.abs(it.time - time) < 180_000 }

    @Synchronized
    fun setTarget(convo: String, t: Target) { targets[convo] = t; version.intValue++ }

    @Synchronized
    fun dropTarget(key: String) {
        if (targets.entries.removeAll { it.value.key == key }) version.intValue++
    }

    @Synchronized
    fun canReply(convo: String) = targets.containsKey(convo)

    /** Conversaciones con su último mensaje, la más reciente primero. */
    @Synchronized
    fun conversations(): List<Msg> =
        msgs.groupBy { it.convo }.map { (_, l) -> l.maxByOrNull { it.time }!! }.sortedByDescending { it.time }

    @Synchronized
    fun unreadSince(t: Long): Int = msgs.count { !it.mine && it.time > t }

    @Synchronized
    fun messages(convo: String): List<Msg> = msgs.filter { it.convo == convo }.sortedBy { it.time }

    /** Envía [text] como respuesta rápida de la notificación. false si ya no se puede responder. */
    @Synchronized
    fun reply(ctx: Context, convo: String, text: String): Boolean {
        val t = targets[convo] ?: return false
        val intent = Intent()
        val results = Bundle().apply { putCharSequence(t.input.resultKey, text) }
        RemoteInput.addResultsToIntent(arrayOf(t.input), intent, results)
        return try {
            t.action.actionIntent.send(ctx, 0, intent)
            add(ctx, Msg(convo, "", text, System.currentTimeMillis(), true))
            true
        } catch (e: PendingIntent.CanceledException) {
            targets.remove(convo); version.intValue++
            false
        }
    }
}
