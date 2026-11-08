package com.geurys.enfoque

import android.content.Context

/** Preferencias simples: límites diarios por app y sesión de enfoque activa. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("enfoque", Context.MODE_PRIVATE)

    /** Momento (epoch ms) en que termina el modo Enfoque; 0 si no hay sesión. */
    var focusEndMillis: Long
        get() = sp.getLong("focus_end", 0L)
        set(value) = sp.edit().putLong("focus_end", value).apply()

    val focusActive: Boolean get() = focusEndMillis > System.currentTimeMillis()

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
