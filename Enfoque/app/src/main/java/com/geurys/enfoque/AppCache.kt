package com.geurys.enfoque

import android.content.Context
import android.content.Intent
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val pkg: String, val label: String)

/** Caché en memoria de apps instaladas e íconos, para no consultar el sistema en el hilo principal. */
object AppCache {
    @Volatile private var apps: List<AppInfo> = emptyList()
    @Volatile private var loadedAt = 0L
    private val icons = LruCache<String, ImageBitmap>(80)

    fun cached(): List<AppInfo> = apps

    suspend fun load(ctx: Context, maxAgeMs: Long = 30_000): List<AppInfo> = withContext(Dispatchers.IO) {
        if (apps.isNotEmpty() && System.currentTimeMillis() - loadedAt < maxAgeMs) return@withContext apps
        val pm = ctx.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(intent, 0)
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.pkg }
            .filter { it.pkg != ctx.packageName }
            .sortedBy { it.label.lowercase() }
        apps = list
        loadedAt = System.currentTimeMillis()
        list
    }

    fun iconCached(pkg: String): ImageBitmap? = icons.get(pkg)

    suspend fun icon(ctx: Context, pkg: String): ImageBitmap? = icons.get(pkg) ?: withContext(Dispatchers.IO) {
        runCatching { ctx.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }
            .getOrNull()?.also { icons.put(pkg, it) }
    }
}
