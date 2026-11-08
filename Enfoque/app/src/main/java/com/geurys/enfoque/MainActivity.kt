package com.geurys.enfoque

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { App() } }
    }
}

private data class AppInfo(val pkg: String, val label: String)

private fun installedApps(ctx: Context): List<AppInfo> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .distinctBy { it.pkg }
        .filter { it.pkg != ctx.packageName }
        .sortedBy { it.label.lowercase() }
}

private fun fmt(ms: Long): String {
    val m = ms / 60_000
    return if (m >= 60) "${m / 60} h ${m % 60} min" else "$m min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App() {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    // Se incrementa al volver a la app para refrescar permisos y uso.
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val tabs = listOf("Hoy", "Apps", "Enfoque")
    Scaffold(
        topBar = { TopAppBar(title = { Text("Enfoque") }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(listOf("📊", "📱", "🎯")[i]) }, label = { Text(t) }
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            PermissionsCard(refresh)
            when (tab) {
                0 -> TodayTab(refresh)
                1 -> AppsTab(refresh)
                else -> FocusTab()
            }
        }
    }
}

@Composable
private fun PermissionsCard(refresh: Int) {
    val ctx = LocalContext.current
    val usage = remember(refresh) { Usage.hasUsageAccess(ctx) }
    val blocker = remember(refresh) { Usage.isBlockerEnabled(ctx) }
    if (usage && blocker) return
    Card(Modifier.fillMaxWidth().padding(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Faltan permisos", style = MaterialTheme.typography.titleMedium)
            if (!usage) {
                Text("Acceso de uso: para medir cuánto usas cada app.")
                Button(onClick = { ctx.startActivity(Usage.usageSettingsIntent()) }) { Text("Dar acceso de uso") }
            }
            if (!blocker) {
                Text("Accesibilidad: solo para saber qué app se abre y bloquearla. No lee tu pantalla.")
                Button(onClick = { ctx.startActivity(Usage.accessibilitySettingsIntent()) }) { Text("Activar bloqueador") }
            }
        }
    }
}

@Composable
private fun TodayTab(refresh: Int) {
    val ctx = LocalContext.current
    val apps = remember(refresh) { installedApps(ctx).associate { it.pkg to it.label } }
    val usage = remember(refresh) { if (Usage.hasUsageAccess(ctx)) Usage.todayMillis(ctx) else emptyMap() }
    val shown = usage.filterKeys { it in apps }.entries.sortedByDescending { it.value }
    val total = shown.sumOf { it.value }
    val max = shown.firstOrNull()?.value ?: 1L

    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Tiempo en pantalla hoy", style = MaterialTheme.typography.labelLarge)
            Text(fmt(total), style = MaterialTheme.typography.displaySmall)
            Text("${Prefs(ctx).unlocksToday()} desbloqueos hoy", style = MaterialTheme.typography.bodyMedium)
        }
        items(shown.take(10)) { (pkg, ms) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(apps[pkg] ?: pkg); Text(fmt(ms))
                }
                LinearProgressIndicator(progress = { ms.toFloat() / max }, Modifier.fillMaxWidth())
            }
        }
        if (shown.isEmpty()) item { Text("Aún no hay datos. Da el acceso de uso y vuelve en un rato.") }
    }
}

@Composable
private fun AppsTab(refresh: Int) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val apps = remember(refresh) { installedApps(ctx) }
    var limits by remember { mutableStateOf(prefs.limits()) }
    val usage = remember(refresh) { if (Usage.hasUsageAccess(ctx)) Usage.todayMillis(ctx) else emptyMap() }

    LazyColumn(Modifier.padding(horizontal = 16.dp)) {
        item {
            Text(
                "Marca las apps que te distraen y ponles un límite diario. Al pasarlo, se bloquean hasta mañana.",
                Modifier.padding(bottom = 8.dp)
            )
        }
        items(apps, key = { it.pkg }) { app ->
            val limit = limits[app.pkg]
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = limit != null,
                    onCheckedChange = { on ->
                        prefs.setLimit(app.pkg, if (on) 30 else null); limits = prefs.limits()
                    }
                )
                Column(Modifier.weight(1f)) {
                    Text(app.label)
                    Text("Hoy: ${fmt(usage[app.pkg] ?: 0)}", style = MaterialTheme.typography.bodySmall)
                }
                if (limit != null) {
                    TextButton(onClick = {
                        prefs.setLimit(app.pkg, (limit - 15).coerceAtLeast(5)); limits = prefs.limits()
                    }) { Text("−") }
                    Text("$limit min")
                    TextButton(onClick = {
                        prefs.setLimit(app.pkg, (limit + 15).coerceAtMost(480)); limits = prefs.limits()
                    }) { Text("+") }
                }
            }
        }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        TextButton(onClick = { onChange(hour - 1) }) { Text("−") }
        Text("%02d:00".format(hour))
        TextButton(onClick = { onChange(hour + 1) }) { Text("+") }
    }
}

@Composable
private fun FocusTab() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    var end by remember { mutableLongStateOf(prefs.focusEndMillis) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(end) {
        while (end > System.currentTimeMillis()) { now = System.currentTimeMillis(); delay(1000) }
        now = System.currentTimeMillis()
    }
    val remaining = (end - now).coerceAtLeast(0)

    var schedOn by remember { mutableStateOf(prefs.scheduleEnabled) }
    var start by remember { mutableIntStateOf(prefs.scheduleStart) }
    var stop by remember { mutableIntStateOf(prefs.scheduleEnd) }
    var gray by remember { mutableStateOf(Grayscale.isOn(ctx)) }
    var autoGray by remember { mutableStateOf(prefs.autoGray) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (remaining > 0) {
            Text("Enfocado", style = MaterialTheme.typography.titleMedium)
            Text(
                "%02d:%02d".format(remaining / 60_000, remaining / 1000 % 60),
                style = MaterialTheme.typography.displayLarge
            )
            Text("Tus apps distractoras están bloqueadas.")
            OutlinedButton(onClick = {
                prefs.focusEndMillis = 0; end = 0
                Grayscale.sync(ctx); gray = Grayscale.isOn(ctx)
            }) { Text("Terminar antes") }
        } else {
            Text("¿Cuánto tiempo quieres enfocarte?", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15, 25, 45, 60).forEach { min ->
                    Button(onClick = {
                        end = System.currentTimeMillis() + min * 60_000L
                        prefs.focusEndMillis = end
                        Grayscale.sync(ctx)
                        gray = Grayscale.isOn(ctx)
                    }) { Text("$min") }
                }
            }
            Text("minutos", style = MaterialTheme.typography.bodySmall)
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Horario automático", style = MaterialTheme.typography.titleSmall)
                    Text("Enfoque activo todos los días en este horario.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = schedOn, onCheckedChange = { schedOn = it; prefs.scheduleEnabled = it })
            }
            if (schedOn) {
                HourStepper("Desde", start) { start = it.mod(24); prefs.scheduleStart = start }
                HourStepper("Hasta", stop) { stop = it.mod(24); prefs.scheduleEnd = stop }
            }
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Escala de grises", style = MaterialTheme.typography.titleSmall)
                    Text("Sin colores, el celular es menos adictivo.", style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = gray, onCheckedChange = { on ->
                    if (Grayscale.canToggle(ctx) && Grayscale.set(ctx, on)) gray = on
                    else Toast.makeText(ctx, "Falta un permiso (ver abajo)", Toast.LENGTH_SHORT).show()
                })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activarla sola durante el Enfoque", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = autoGray, onCheckedChange = { on ->
                    autoGray = on; prefs.autoGray = on
                    Grayscale.sync(ctx); gray = Grayscale.isOn(ctx)
                })
            }
            if (!Grayscale.canToggle(ctx)) {
                Text(
                    "Android pide un permiso especial. Conecta el celular a una PC y ejecuta una sola vez:\n${Grayscale.ADB_COMMAND}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text("Launcher minimalista", style = MaterialTheme.typography.titleSmall)
        Text("Usa Enfoque como tu pantalla de inicio: solo reloj, favoritas y búsqueda.", style = MaterialTheme.typography.bodySmall)
        Button(onClick = { ctx.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }) {
            Text("Elegir como launcher")
        }
    }
}
