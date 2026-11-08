package com.geurys.enfoque

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Preferencias simples: límites diarios por app y sesión de enfoque activa. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("enfoque", Context.MODE_PRIVATE)

    /** Momento (epoch ms) en que termina el modo Enfoque; 0 si no hay sesión. */
    var focusEndMillis: Long
        get() = sp.getLong("focus_end", 0L)
        set(value) = sp.edit().putLong("focus_end", value).apply()

    /** Enfoque activo: sesión manual en curso o dentro del horario automático. */
    /** Duración total de la sesión manual (para dibujar el progreso). */
    var focusTotalMillis: Long
        get() = sp.getLong("focus_total", 0L)
        set(value) = sp.edit().putLong("focus_total", value).apply()

    val focusActive: Boolean get() = focusEndMillis > System.currentTimeMillis() || inSchedule() || inNight()

    var scheduleEnabled: Boolean
        get() = sp.getBoolean("sched_on", false)
        set(v) = sp.edit().putBoolean("sched_on", v).apply()
    var scheduleStart: Int
        get() = sp.getInt("sched_start", 9)
        set(v) = sp.edit().putInt("sched_start", v.mod(24)).apply()
    var scheduleEnd: Int
        get() = sp.getInt("sched_end", 17)
        set(v) = sp.edit().putInt("sched_end", v.mod(24)).apply()

    private fun hourIn(s: Int, e: Int): Boolean {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (s <= e) h in s until e else h >= s || h < e // soporta horarios que cruzan medianoche
    }

    fun inSchedule(): Boolean = scheduleEnabled && hourIn(scheduleStart, scheduleEnd)

    /** Modo noche: mismo bloqueo que el Enfoque, pensado para dormir mejor. */
    var nightEnabled: Boolean
        get() = sp.getBoolean("night_on", false)
        set(v) = sp.edit().putBoolean("night_on", v).apply()
    var nightStart: Int
        get() = sp.getInt("night_start", 23)
        set(v) = sp.edit().putInt("night_start", v.mod(24)).apply()
    var nightEnd: Int
        get() = sp.getInt("night_end", 7)
        set(v) = sp.edit().putInt("night_end", v.mod(24)).apply()

    fun inNight(): Boolean = nightEnabled && hourIn(nightStart, nightEnd)

    /** Pausa consciente: espera antes de abrir una app distractora. */
    var pauseEnabled: Boolean
        get() = sp.getBoolean("pause_on", true)
        set(v) = sp.edit().putBoolean("pause_on", v).apply()
    var pauseSeconds: Int
        get() = sp.getInt("pause_s", 10)
        set(v) = sp.edit().putInt("pause_s", v.coerceIn(5, 30)).apply()

    /** Poner el celular en grises automáticamente mientras haya Enfoque. */
    var autoGray: Boolean
        get() = sp.getBoolean("auto_gray", false)
        set(v) = sp.edit().putBoolean("auto_gray", v).apply()

    /** Verdadero si fuimos nosotros quienes encendimos los grises (para no apagar los que puso el usuario). */
    var grayByUs: Boolean
        get() = sp.getBoolean("gray_by_us", false)
        set(v) = sp.edit().putBoolean("gray_by_us", v).apply()

    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun unlocksToday(): Int =
        if (sp.getString("unlock_day", "") == today()) sp.getInt("unlock_count", 0) else 0

    fun addUnlock() {
        sp.edit().putString("unlock_day", today()).putInt("unlock_count", unlocksToday() + 1).apply()
    }

    /** Apps favoritas que se ven en el launcher. */
    fun favorites(): Set<String> = sp.getStringSet("favs", emptySet()).orEmpty()
    fun toggleFavorite(pkg: String) {
        val f = favorites().toMutableSet()
        if (!f.add(pkg)) f.remove(pkg)
        sp.edit().putStringSet("favs", f).apply()
    }

    /** ¿Esta app debe estar bloqueada ahora mismo? */
    fun isBlocked(ctx: Context, pkg: String, usageToday: Map<String, Long>): Boolean {
        val limit = limits()[pkg] ?: return false
        return focusActive || (usageToday[pkg] ?: 0L) >= limit * 60_000L
    }

    /** paquete -> minutos permitidos por día. */
    fun limits(): Map<String, Int> {
        val result = mutableMapOf<String, Int>()
        for (entry in sp.getStringSet("limits", emptySet()).orEmpty()) {
            val i = entry.lastIndexOf(':')
            if (i < 0) continue
            val minutes = entry.substring(i + 1).toIntOrNull() ?: continue
            result[entry.substring(0, i)] = minutes
        }
        return result
    }

    /** Fija el límite de una app; `null` la quita de la lista de distractoras. */
    fun setLimit(pkg: String, minutes: Int?) {
        val map = limits().toMutableMap()
        if (minutes == null) map.remove(pkg) else map[pkg] = minutes
        sp.edit()
            .putStringSet("limits", map.map { "${it.key}:${it.value}" }.toSet())
            .apply()
    }
}
