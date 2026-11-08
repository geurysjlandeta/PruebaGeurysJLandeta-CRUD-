package com.geurys.enfoque

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
                    item {
                        Text(
                            "Mensajes", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {
                                ctx.startActivity(Intent(ctx, MainActivity::class.java).putExtra("tab", 3))
                            }).padding(vertical = 10.dp)
                        )
                    }
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
