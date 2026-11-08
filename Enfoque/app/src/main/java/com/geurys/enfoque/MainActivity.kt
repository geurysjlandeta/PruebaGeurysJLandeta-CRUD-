@file:OptIn(ExperimentalMaterial3Api::class)

package com.geurys.enfoque

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val startTab = intent.getIntExtra("tab", 0)
        setContent { EnfoqueTheme { App(startTab) } }
    }
}

// ───────────────────────── Estructura ─────────────────────────

@Composable
private fun App(startTab: Int) {
    var tab by remember { mutableIntStateOf(startTab) }
    // Sube al volver a la app para refrescar permisos y uso.
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val items: List<Pair<String, ImageVector>> = listOf(
        "Hoy" to Icons.Filled.Home,
        "Apps" to Icons.AutoMirrored.Filled.List,
        "Enfoque" to Icons.Filled.Lock,
        "Mensajes" to Icons.Filled.Email,
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                items.forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).statusBarsPadding().padding(horizontal = 20.dp)) {
            when (tab) {
                0 -> TodayTab(refresh)
                1 -> AppsTab(refresh)
                2 -> FocusTab()
                else -> MessagesTab(refresh)
            }
        }
    }
}

// ───────────────────────── Hoy ─────────────────────────

@Composable
private fun SetupCard(refresh: Int) {
    val ctx = LocalContext.current
    val usage = remember(refresh) { Usage.hasUsageAccess(ctx) }
    val blocker = remember(refresh) { Usage.isBlockerEnabled(ctx) }
    if (usage && blocker) return
    AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
        Text("Configura Enfoque", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Dos permisos y listo. Todo se queda en tu celular.",
            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        PermissionRow(
            done = usage, title = "Acceso de uso",
            desc = "Para medir cuánto usas cada app. En la lista, elige Enfoque y actívalo.",
            onHelp = { helpRestricted(ctx) }
        ) { Usage.startSafely(ctx, Usage.usageSettingsIntent(ctx), Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        Spacer(Modifier.height(8.dp))
        PermissionRow(
            done = blocker, title = "Bloqueador",
            desc = "Solo detecta qué app se abre para frenarla. No lee tu pantalla. En Accesibilidad, busca Enfoque en apps descargadas.",
            onHelp = { helpRestricted(ctx) }
        ) { Usage.startSafely(ctx, Usage.accessibilitySettingsIntent()) }
    }
}

private fun helpRestricted(ctx: android.content.Context) {
    Toast.makeText(ctx, "Toca ⋮ arriba a la derecha → Permitir ajustes restringidos", Toast.LENGTH_LONG).show()
    Usage.startSafely(ctx, Usage.appDetailsIntent(ctx))
}

@Composable
private fun PermissionRow(done: Boolean, title: String, desc: String, onHelp: (() -> Unit)? = null, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { if (done) Icon(Icons.Filled.Check, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimary) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodySmall)
            if (!done && onHelp != null) {
                TextButton(onClick = onHelp, contentPadding = PaddingValues(0.dp)) { Text("¿No te deja activarlo?") }
            }
        }
        if (!done) {
            Spacer(Modifier.width(8.dp))
            Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 16.dp)) { Text("Activar") }
        }
    }
}

@Composable
private fun TodayTab(refresh: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var apps by remember { mutableStateOf(AppCache.cached()) }
    var usage by remember { mutableStateOf<Map<String, Long>?>(null) }
    LaunchedEffect(refresh) {
        apps = AppCache.load(ctx)
        usage = if (Usage.hasUsageAccess(ctx)) Usage.todayAsync(ctx) else emptyMap()
    }
    val labels = remember(apps) { apps.associate { it.pkg to it.label } }
    val data = usage

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeader("Hoy", "Así va tu día con el celular") }
        item { SetupCard(refresh) }
        if (data == null) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
        } else {
            val shown = data.filterKeys { it in labels }.entries.sortedByDescending { it.value }
            val total = shown.sumOf { it.value }
            val over = prefs.limits().count { (p, m) -> (data[p] ?: 0L) >= m * 60_000L }
            item {
                AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Tiempo en pantalla", style = MaterialTheme.typography.labelLarge)
                    Text(fmt(total), fontSize = 44.sp, fontWeight = FontWeight.Bold)
                    if (prefs.focusActive) {
                        Text("● Enfoque activo", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Stat("${prefs.unlocksToday()}", "desbloqueos", Modifier.weight(1f))
                        Stat("$over", "límites superados", Modifier.weight(1f))
                    }
                }
            }
            item { SectionTitle("Más usadas") }
            if (shown.isEmpty()) {
                item {
                    AppCard {
                        Text(
                            if (Usage.hasUsageAccess(ctx)) "Todavía no hay datos de hoy." else "Da el acceso de uso para ver tus estadísticas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val max = shown.first().value.coerceAtLeast(1L)
                item {
                    AppCard {
                        shown.take(8).forEachIndexed { i, (pkg, ms) ->
                            if (i > 0) Spacer(Modifier.height(14.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppIcon(pkg, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(labels[pkg] ?: pkg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(fmt(ms), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { ms.toFloat() / max },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)).padding(14.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

// ───────────────────────── Apps ─────────────────────────

@Composable
private fun AppsTab(refresh: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var apps by remember { mutableStateOf(AppCache.cached()) }
    var usage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    var limits by remember { mutableStateOf(prefs.limits()) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(refresh) {
        apps = AppCache.load(ctx)
        usage = if (Usage.hasUsageAccess(ctx)) Usage.todayAsync(ctx) else emptyMap()
        limits = prefs.limits()
    }
    // Las apps con límite van primero; el orden solo se recalcula al buscar o recargar (no salta al activar un interruptor).
    val list = remember(apps, query) {
        val lim = prefs.limits()
        apps.filter { query.isBlank() || it.label.contains(query.trim(), ignoreCase = true) }
            .sortedWith(compareByDescending<AppInfo> { it.pkg in lim }.thenBy { it.label.lowercase() })
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ScreenHeader("Apps distractoras", "Activa las que te quitan tiempo y ponles un límite diario. Al pasarlo se bloquean hasta mañana.") }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                singleLine = true, shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Buscar app") }
            )
        }
        if (apps.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
        }
        items(list, key = { it.pkg }) { app ->
            AppRow(app, limits[app.pkg], usage[app.pkg] ?: 0L) { v ->
                prefs.setLimit(app.pkg, v); limits = prefs.limits()
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, limit: Int?, used: Long, onLimit: (Int?) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.pkg, 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Hoy: ${fmt(used)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = limit != null, onCheckedChange = { onLimit(if (it) 30 else null) })
        }
        if (limit != null) {
            val over = used >= limit * 60_000L
            Row(Modifier.padding(start = 58.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { (used.toFloat() / (limit * 60_000f)).coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Spacer(Modifier.width(12.dp))
                FilledTonalIconButton(onClick = { onLimit((limit - 15).coerceAtLeast(5)) }, modifier = Modifier.size(32.dp)) { Text("−") }
                Text("$limit min", Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Medium)
                FilledTonalIconButton(onClick = { onLimit((limit + 15).coerceAtMost(480)) }, modifier = Modifier.size(32.dp)) { Text("+") }
            }
        }
    }
}

// ───────────────────────── Enfoque ─────────────────────────

@Composable
private fun TimerRing(progress: Float, label: String, sub: String) {
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val color = MaterialTheme.colorScheme.primary
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (progress > 0f) {
                drawArc(color, -90f, 360f * progress, false, topLeft = topLeft, size = arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text(sub, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.size(36.dp)) { Text("−") }
        Text(value, Modifier.padding(horizontal = 14.dp), fontWeight = FontWeight.SemiBold)
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.size(36.dp)) { Text("+") }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) =
    StepperRow(label, "%02d:00".format(hour), { onChange(hour - 1) }, { onChange(hour + 1) })

/** Diálogo con espera obligatoria: dar marcha atrás debe costar un poco. */
@Composable
private fun FrictionDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    var left by remember { mutableIntStateOf(15) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) }, text = { Text(text) },
        confirmButton = {
            TextButton(enabled = left == 0, onClick = onConfirm) { Text(if (left > 0) "$confirm ($left)" else confirm) }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Seguir enfocado") } }
    )
}

@Composable
private fun SwitchRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun FocusTab() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var end by remember { mutableLongStateOf(prefs.focusEndMillis) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selected by remember { mutableIntStateOf(25) }
    LaunchedEffect(end) {
        while (end > System.currentTimeMillis()) { now = System.currentTimeMillis(); delay(1000) }
        now = System.currentTimeMillis()
    }
    val remaining = (end - now).coerceAtLeast(0)
    val active = remaining > 0
    val total = prefs.focusTotalMillis.coerceAtLeast(1L)

    var schedOn by remember { mutableStateOf(prefs.scheduleEnabled) }
    var start by remember { mutableIntStateOf(prefs.scheduleStart) }
    var stop by remember { mutableIntStateOf(prefs.scheduleEnd) }
    var gray by remember { mutableStateOf(Grayscale.isOn(ctx)) }
    var autoGray by remember { mutableStateOf(prefs.autoGray) }
    var nightOn by remember { mutableStateOf(prefs.nightEnabled) }
    var nightStart by remember { mutableIntStateOf(prefs.nightStart) }
    var nightEnd by remember { mutableIntStateOf(prefs.nightEnd) }
    var pauseOn by remember { mutableStateOf(prefs.pauseEnabled) }
    var pauseSecs by remember { mutableIntStateOf(prefs.pauseSeconds) }
    var confirmEnd by remember { mutableStateOf(false) }
    var confirmSchedOff by remember { mutableStateOf(false) }
    var confirmNightOff by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    if (confirmEnd) FrictionDialog(
        "¿Terminar el Enfoque?", "Respira un momento. ¿De verdad necesitas esas apps ahora?", "Sí, terminar",
        onConfirm = {
            prefs.focusEndMillis = 0; end = 0
            Grayscale.sync(ctx); gray = Grayscale.isOn(ctx)
            confirmEnd = false
        },
        onDismiss = { confirmEnd = false }
    )
    if (confirmSchedOff) FrictionDialog(
        "¿Desactivar el horario?", "Ahora mismo estás dentro de tu horario de enfoque.", "Sí, desactivar",
        onConfirm = { schedOn = false; prefs.scheduleEnabled = false; confirmSchedOff = false },
        onDismiss = { confirmSchedOff = false }
    )
    if (confirmNightOff) FrictionDialog(
        "¿Desactivar el modo noche?", "Es hora de descansar. ¿Seguro?", "Sí, desactivar",
        onConfirm = { nightOn = false; prefs.nightEnabled = false; confirmNightOff = false },
        onDismiss = { confirmNightOff = false }
    )

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ScreenHeader("Enfoque", "Bloquea tus apps distractoras por un rato") }
        item {
            AppCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (active) {
                        TimerRing(
                            progress = (remaining.toFloat() / total).coerceIn(0f, 1f),
                            label = "%02d:%02d".format(remaining / 60_000, remaining / 1000 % 60),
                            sub = "enfocado"
                        )
                        Text(
                            "Tus apps distractoras están bloqueadas.",
                            Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { confirmEnd = true }) { Text("Terminar antes") }
                    } else {
                        TimerRing(progress = 1f, label = "%02d:00".format(selected), sub = "minutos")
                        Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45, 60).forEach { m ->
                                FilterChip(selected = selected == m, onClick = { selected = m }, label = { Text("$m min") })
                            }
                        }
                        Button(
                            onClick = {
                                val millis = selected * 60_000L
                                end = System.currentTimeMillis() + millis
                                prefs.focusTotalMillis = millis
                                prefs.focusEndMillis = end
                                Grayscale.sync(ctx); gray = Grayscale.isOn(ctx)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) { Text("Empezar enfoque", fontSize = 16.sp) }
                    }
                }
            }
        }
        item {
            AppCard {
                SwitchRow("Horario automático", "El Enfoque se activa solo, todos los días.", schedOn) { on ->
                    if (!on && prefs.inSchedule()) confirmSchedOff = true
                    else { schedOn = on; prefs.scheduleEnabled = on }
                }
                if (schedOn) {
                    HourStepper("Desde", start) { start = it.mod(24); prefs.scheduleStart = start }
                    HourStepper("Hasta", stop) { stop = it.mod(24); prefs.scheduleEnd = stop }
                }
            }
        }
        item {
            AppCard {
                SwitchRow("Modo noche", "Bloquea tus apps y baja los colores para dormir mejor.", nightOn) { on ->
                    if (!on && prefs.inNight()) confirmNightOff = true
                    else { nightOn = on; prefs.nightEnabled = on }
                }
                if (nightOn) {
                    HourStepper("Desde", nightStart) { nightStart = it.mod(24); prefs.nightStart = nightStart }
                    HourStepper("Hasta", nightEnd) { nightEnd = it.mod(24); prefs.nightEnd = nightEnd }
                }
            }
        }
        item {
            AppCard {
                SwitchRow("Pausa consciente", "Unos segundos para pensar antes de abrir una app distractora.", pauseOn) {
                    pauseOn = it; prefs.pauseEnabled = it
                }
                if (pauseOn) {
                    StepperRow("Espera", "$pauseSecs s", { pauseSecs = (pauseSecs - 5).coerceAtLeast(5); prefs.pauseSeconds = pauseSecs },
                        { pauseSecs = (pauseSecs + 5).coerceAtMost(30); prefs.pauseSeconds = pauseSecs })
                }
            }
        }
        item {
            AppCard {
                SwitchRow("Escala de grises", "Sin colores, el celular engancha menos.", gray) { on ->
                    if (Grayscale.canToggle(ctx) && Grayscale.set(ctx, on)) gray = on
                    else Toast.makeText(ctx, "Falta un permiso (ver abajo)", Toast.LENGTH_SHORT).show()
                }
                Spacer(Modifier.height(12.dp))
                SwitchRow("Automática en Enfoque", "Se enciende y apaga con tus sesiones.", autoGray) { on ->
                    autoGray = on; prefs.autoGray = on
                    Grayscale.sync(ctx); gray = Grayscale.isOn(ctx)
                }
                if (!Grayscale.canToggle(ctx)) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Android pide un permiso especial. Una sola vez, conecta el celular a una PC y ejecuta:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(Grayscale.ADB_COMMAND, Modifier.padding(12.dp), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(Grayscale.ADB_COMMAND))
                        Toast.makeText(ctx, "Comando copiado", Toast.LENGTH_SHORT).show()
                    }) { Text("Copiar comando") }
                }
            }
        }
        item {
            AppCard {
                Text("Launcher minimalista", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Usa Enfoque como tu pantalla de inicio: solo reloj, favoritas y búsqueda.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )
                FilledTonalButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) {
                    Text("Elegir como launcher")
                }
            }
        }
    }
}

// ───────────────────────── Mensajes ─────────────────────────

@Composable
private fun MessagesTab(refresh: Int) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { Inbox.load(ctx) }
        Inbox.version.intValue++
    }
    val version = Inbox.version.intValue
    var open by remember { mutableStateOf<String?>(null) }
    BackHandler(open != null) { open = null }
    val listenerOk = remember(refresh) { Usage.hasNotificationAccess(ctx) }
    val accOk = remember(refresh) { Usage.isBlockerEnabled(ctx) }
    val hasAccess = listenerOk || accOk
    LaunchedEffect(refresh) {
        if (listenerOk && !NotificationService.connected) NotificationService.rebind(ctx)
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val convo = open
    if (convo != null) {
        Chat(convo, version) { open = null }
        return
    }
    val convos = remember(version) { Inbox.conversations() }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            ScreenHeader("Mensajes", "WhatsApp en modo mínimo: solo texto, sin stickers ni estados.")
        }
        if (!hasAccess) {
            item {
                AppCard(container = MaterialTheme.colorScheme.primaryContainer) {
                    Text("Falta un permiso", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Para leer tus mensajes de WhatsApp activa el Bloqueador (pestaña Hoy) o el acceso a notificaciones.",
                        Modifier.padding(top = 2.dp, bottom = 12.dp), style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = { ctx.startActivity(Usage.notificationAccessIntent()) }) { Text("Dar acceso a notificaciones") }
                }
            }
        }
        if (hasAccess) {
            item {
                AppCard {
                    Text(notifStatusText(ctx), style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { refreshNotifications(ctx) }) { Text("Actualizar") }
                        TextButton(onClick = {
                            Usage.startSafely(ctx, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }) { Text("Ahorro de batería") }
                    }
                    Text(
                        "Si no llegan mensajes: en Ajustes pon la batería de Enfoque en \"Sin restricciones\" y activa el inicio automático.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
        item { SectionTitle("Tip: bloquea WhatsApp en la pestaña Apps y responde solo desde aquí.") }
        if (convos.isEmpty()) {
            item {
                AppCard {
                    Text("Aún no hay mensajes.", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Aparecerán cuando te escriban en WhatsApp.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        items(convos, key = { it.convo }) { m ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { open = m.convo }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        m.convo.first().uppercase(), fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(m.convo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        (if (m.mine) "Tú: " else "") + m.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    timeFmt.format(Date(m.time)), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Chat(convo: String, version: Int, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val msgs = remember(version) { Inbox.messages(convo).reversed() }
    val canReply = remember(version) { Inbox.canReply(convo) }
    var text by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
            Text(convo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LazyColumn(Modifier.weight(1f), reverseLayout = true, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(msgs) { m ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = if (m.mine) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (m.mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        val who = if (!m.mine && m.sender.isNotBlank() && m.sender != convo) "${m.sender}: " else ""
                        Text(
                            who + m.text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = if (m.mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        if (!canReply) {
            Text(
                "Desde aquí solo puedes leer. Para responder, usa «Responder» en la notificación de WhatsApp.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                modifier = Modifier.weight(1f), enabled = canReply,
                shape = RoundedCornerShape(24.dp), placeholder = { Text("Mensaje") }, maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                enabled = canReply && text.isNotBlank(),
                onClick = {
                    if (Inbox.reply(ctx, convo, text.trim())) text = ""
                    else Toast.makeText(ctx, "Ya no se puede responder; espera un mensaje nuevo", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(52.dp)
            ) { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar") }
        }
    }
}
