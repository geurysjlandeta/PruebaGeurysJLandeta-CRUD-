package com.geurys.enfoque

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Pantalla de inicio minimalista: reloj, tus favoritas y búsqueda. Sin iconos ni colores que distraigan. */
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Home() } }
    }
}

private data class Entry(val pkg: String, val label: String)

private fun launchables(ctx: Context): List<Entry> {
    val pm = ctx.packageManager
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(i, 0)
        .map { Entry(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.pkg }
        .filter { it.pkg != ctx.packageName }
        .sortedBy { it.label.lowercase() }
}

@OptIn(ExperimentalFoundationApi::class)
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

    val apps = remember(refresh) { launchables(ctx) }
    val usage = remember(refresh) { if (Usage.hasUsageAccess(ctx)) Usage.todayMillis(ctx) else emptyMap() }
    var favs by remember(refresh) { mutableStateOf(prefs.favorites()) }
    var query by remember { mutableStateOf("") }
    val shown = if (query.isBlank()) apps.filter { it.pkg in favs }
    else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }

    fun open(app: Entry) {
        if (prefs.isBlocked(ctx, app.pkg, usage)) {
            Toast.makeText(ctx, "${app.label} está bloqueada ahora", Toast.LENGTH_SHORT).show()
        } else ctx.packageManager.getLaunchIntentForPackage(app.pkg)?.let(ctx::startActivity)
    }

    Surface(Modifier.fillMaxSize(), color = Color.Black) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { ctx.startActivity(Intent(ctx, MainActivity::class.java)) }) {
                    Text("Ajustes", color = Color.Gray)
                }
            }
            Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(now), style = MaterialTheme.typography.displayLarge)
            Text(
                SimpleDateFormat("EEEE d 'de' MMMM", Locale.getDefault()).format(now),
                style = MaterialTheme.typography.titleMedium, color = Color.Gray
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${prefs.unlocksToday()} desbloqueos hoy" + if (prefs.focusActive) " · Enfoque activo" else "",
                style = MaterialTheme.typography.bodySmall, color = Color.Gray
            )
            Spacer(Modifier.height(24.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(shown, key = { it.pkg }) { app ->
                    val blocked = prefs.isBlocked(ctx, app.pkg, usage)
                    Text(
                        app.label + if (blocked) "  🔒" else "",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (blocked) Color.DarkGray else Color.White,
                        modifier = Modifier.fillMaxWidth().combinedClickable(
                            onClick = { open(app) },
                            onLongClick = {
                                prefs.toggleFavorite(app.pkg); favs = prefs.favorites()
                                Toast.makeText(
                                    ctx,
                                    if (app.pkg in favs) "Añadida a favoritas" else "Quitada de favoritas",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ).padding(vertical = 8.dp)
                    )
                }
                if (shown.isEmpty()) item {
                    Text(
                        if (query.isBlank()) "Busca una app abajo y mantenla pulsada para fijarla aquí."
                        else "Sin resultados",
                        color = Color.Gray
                    )
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("Buscar app…") }
            )
        }
    }
}
