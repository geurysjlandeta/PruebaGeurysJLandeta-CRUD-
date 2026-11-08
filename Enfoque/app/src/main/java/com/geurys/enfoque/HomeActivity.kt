package com.geurys.enfoque

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pantalla de inicio minimalista: reloj, tus favoritas y búsqueda. Sin iconos ni colores que distraigan. */
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent { EnfoqueTheme(forceDark = true) { Home() } }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun Home() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    BackHandler(true) {} // un launcher no se cierra con "atrás"

    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }

    // La lista sale de la caché (instantánea) y se actualiza en segundo plano; nunca bloquea el dibujo.
    var apps by remember { mutableStateOf(AppCache.cached()) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    LaunchedEffect(refresh) {
        apps = AppCache.load(ctx)
        usage = if (Usage.hasUsageAccess(ctx)) Usage.todayAsync(ctx) else emptyMap()
    }
    var favs by remember { mutableStateOf(prefs.favorites()) }
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, favs, query) {
        if (query.isBlank()) apps.filter { it.pkg in favs }
        else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
    }
    val unlocks = remember(refresh) { prefs.unlocksToday() }
    val focus = remember(refresh) { prefs.focusActive }

    fun open(app: AppInfo) {
        if (prefs.isBlocked(ctx, app.pkg, usage)) {
            Toast.makeText(ctx, "${app.label} está bloqueada ahora", Toast.LENGTH_SHORT).show()
        } else ctx.packageManager.getLaunchIntentForPackage(app.pkg)?.let(ctx::startActivity)
    }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 28.dp)
        ) {
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
                fontSize = 76.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 36.dp)
            )
            Text(
                SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(now)
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium, color = Color(0xFF94A3B8)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("$unlocks desbloqueos hoy", Color(0xFF1E293B), Color(0xFFCBD5E1))
                if (focus) Pill("Enfoque activo", Color(0xFF115E59), Color(0xFFCCFBF1))
            }
            Spacer(Modifier.height(24.dp))

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (query.isBlank()) {
                    item { MessagesPanel() }
                }
                items(shown, key = { it.pkg }) { app ->
                    val blocked = prefs.isBlocked(ctx, app.pkg, usage)
                    Row(
                        Modifier.fillMaxWidth().combinedClickable(
                            onClick = { open(app) },
                            onLongClick = {
                                prefs.toggleFavorite(app.pkg); favs = prefs.favorites()
                                Toast.makeText(
                                    ctx,
                                    if (app.pkg in favs) "Añadida a favoritas" else "Quitada de favoritas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ).padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            app.label, style = MaterialTheme.typography.headlineSmall,
                            color = if (blocked) Color(0xFF475569) else Color.White
                        )
                        if (blocked) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.Lock, null, Modifier.size(16.dp), tint = Color(0xFF475569))
                        }
                    }
                }
                if (shown.isEmpty()) item {
                    Text(
                        when {
                            query.isNotBlank() -> "Sin resultados"
                            apps.isEmpty() -> "Cargando…"
                            else -> "Busca una app abajo y mantenla pulsada para fijarla aquí."
                        },
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.weight(1f), singleLine = true,
                    shape = RoundedCornerShape(28.dp), placeholder = { Text("Buscar app…") }
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalIconButton(
                    onClick = { ctx.startActivity(Intent(ctx, MainActivity::class.java)) },
                    modifier = Modifier.size(52.dp)
                ) { Text("⚙", fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun Pill(text: String, bg: Color, fg: Color) {
    Surface(shape = CircleShape, color = bg) {
        Text(text, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = fg, style = MaterialTheme.typography.labelMedium)
    }
}

/** Desplegable de mensajes en la pantalla de inicio: ver y responder sin salir del launcher. */
@Composable
private fun MessagesPanel() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { Inbox.load(ctx) }
        Inbox.version.intValue++
    }
    val version = Inbox.version.intValue
    var open by rememberSaveable { mutableStateOf(false) }
    var convo by rememberSaveable { mutableStateOf<String?>(null) }
    var seenAt by remember { mutableLongStateOf(prefs.msgsSeenAt) }
    // Mientras está desplegado, lo que llega se considera visto.
    LaunchedEffect(open, version) {
        if (open) { seenAt = System.currentTimeMillis(); prefs.msgsSeenAt = seenAt }
    }
    val unread = remember(version, seenAt) { Inbox.unreadSince(seenAt) }
    val accent = MaterialTheme.colorScheme.primary
    val hasAccess = remember(open) { Usage.hasNotificationAccess(ctx) }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable { open = !open; if (!open) convo = null }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mensajes", style = MaterialTheme.typography.headlineSmall, color = accent)
            if (unread > 0) {
                Spacer(Modifier.width(10.dp))
                Surface(shape = CircleShape, color = accent) {
                    Text(
                        "$unread", Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (open) "Cerrar mensajes" else "Abrir mensajes", tint = accent
            )
        }
        AnimatedVisibility(open) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF111A2B), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    val c = convo
                    if (c == null) {
                        val convos = remember(version) { Inbox.conversations().take(6) }
                        if (!hasAccess) {
                            Text("Para ver tus mensajes, da acceso a las notificaciones.", color = Color(0xFF94A3B8))
                            TextButton(onClick = { Usage.startSafely(ctx, Usage.notificationAccessIntent()) }) {
                                Text("Dar acceso")
                            }
                        } else if (convos.isEmpty()) {
                            Text("Aún no hay mensajes. Aparecerán aquí cuando te escriban.", color = Color(0xFF94A3B8))
                        }
                        convos.forEach { m ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { convo = m.convo }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF115E59), modifier = Modifier.size(38.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(m.convo.first().uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFFCCFBF1))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(m.convo, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        (if (m.mine) "Tú: " else "") + m.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8)
                                    )
                                }
                                Text(timeFmt.format(Date(m.time)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            }
                        }
                    } else {
                        InlineChat(c, version) { convo = null }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineChat(convo: String, version: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val msgs = remember(version, convo) { Inbox.messages(convo).takeLast(6) }
    val canReply = remember(version, convo) { Inbox.canReply(convo) }
    var text by remember { mutableStateOf("") }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
        Text(convo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        msgs.forEach { m ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (m.mine) MaterialTheme.colorScheme.primary else Color(0xFF1E293B),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    val who = if (!m.mine && m.sender.isNotBlank() && m.sender != convo) "${m.sender}: " else ""
                    Text(
                        who + m.text, Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = if (m.mine) MaterialTheme.colorScheme.onPrimary else Color.White
                    )
                }
            }
        }
    }
    if (!canReply) {
        Text(
            "Para responder, espera a que llegue un mensaje nuevo de este chat.",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8)
        )
    }
    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text, onValueChange = { text = it },
            modifier = Modifier.weight(1f), enabled = canReply,
            shape = RoundedCornerShape(24.dp), placeholder = { Text("Mensaje") }, maxLines = 3
        )
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            enabled = canReply && text.isNotBlank(),
            onClick = {
                if (Inbox.reply(ctx, convo, text.trim())) text = ""
                else Toast.makeText(ctx, "Ya no se puede responder; espera un mensaje nuevo", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(48.dp)
        ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar") }
    }
}
