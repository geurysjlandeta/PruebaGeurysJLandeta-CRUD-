package com.geurys.enfoque

import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal fun fmt(ms: Long): String {
    val m = ms / 60_000
    return when {
        m >= 60 -> "${m / 60} h ${m % 60} min"
        m < 1 && ms > 0 -> "<1 min"
        else -> "$m min"
    }
}

@Composable
internal fun AppCard(
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) { Column(Modifier.padding(20.dp), content = content) }
}

@Composable
internal fun ScreenHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 20.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
internal fun AppIcon(pkg: String, size: Dp = 40.dp) {
    val ctx = LocalContext.current
    val bmp by produceState<ImageBitmap?>(AppCache.iconCached(pkg), pkg) { value = AppCache.icon(ctx, pkg) }
    Box(Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        bmp?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

/** Texto de diagnóstico: ¿está el lector de notificaciones conectado y viendo mensajes? */
internal fun notifStatusText(): String {
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return when {
        !NotificationService.connected -> "⚠ Sin conexión con las notificaciones"
        NotificationService.lastSeenAt == 0L -> "Conectado · aún no ha llegado ninguna notificación de WhatsApp"
        else -> "Conectado · última de WhatsApp a las ${fmt.format(Date(NotificationService.lastSeenAt))} (${NotificationService.lastInfo})"
    }
}

/** Reconecta el servicio y relee las notificaciones de WhatsApp que ya están en la barra. */
internal fun refreshNotifications(ctx: Context) {
    val s = NotificationService.instance
    if (s != null) s.syncActive() else NotificationService.rebind(ctx)
    Inbox.version.intValue++
    Toast.makeText(ctx, "Actualizando mensajes…", Toast.LENGTH_SHORT).show()
}
