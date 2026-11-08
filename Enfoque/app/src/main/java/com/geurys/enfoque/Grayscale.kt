package com.geurys.enfoque

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings

/**
 * Escala de grises con el filtro de daltonismo del sistema (monocromacia).
 * Android solo deja cambiarlo con WRITE_SECURE_SETTINGS, que se concede una vez por ADB:
 *   adb shell pm grant com.geurys.enfoque android.permission.WRITE_SECURE_SETTINGS
 */
object Grayscale {
    const val ADB_COMMAND = "adb shell pm grant com.geurys.enfoque android.permission.WRITE_SECURE_SETTINGS"
    private const val ENABLED = "accessibility_display_daltonizer_enabled"
    private const val MODE = "accessibility_display_daltonizer"

    fun canToggle(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    fun isOn(ctx: Context) = Settings.Secure.getInt(ctx.contentResolver, ENABLED, 0) == 1

    fun set(ctx: Context, on: Boolean): Boolean = runCatching {
        Settings.Secure.putInt(ctx.contentResolver, MODE, 0) // 0 = monocromacia
        Settings.Secure.putInt(ctx.contentResolver, ENABLED, if (on) 1 else 0)
    }.getOrDefault(false)

    /** Enciende los grises al empezar el Enfoque y los apaga al terminar, si el usuario lo activó. */
    fun sync(ctx: Context) {
        val prefs = Prefs(ctx)
        if (!canToggle(ctx)) return
        val focus = prefs.focusActive
        if (prefs.autoGray && focus && !isOn(ctx)) {
            if (set(ctx, true)) prefs.grayByUs = true
        } else if (!focus && prefs.grayByUs) {
            set(ctx, false)
            prefs.grayByUs = false
        }
    }
}
