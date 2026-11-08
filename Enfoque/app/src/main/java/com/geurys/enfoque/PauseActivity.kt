package com.geurys.enfoque

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/** Apps a las que el usuario ya dijo "continuar": no se vuelve a pausar durante unos minutos. */
object PauseState {
    private const val GRACE_MS = 5 * 60_000L
    private val until = ConcurrentHashMap<String, Long>()
    fun allowed(pkg: String) = (until[pkg] ?: 0L) > SystemClock.elapsedRealtime()
    fun allow(pkg: String) { until[pkg] = SystemClock.elapsedRealtime() + GRACE_MS }
}

/** Pausa consciente: unos segundos para decidir si de verdad quieres abrir la app. */
class PauseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(BlockedActivity.EXTRA_PKG).orEmpty()
        val seconds = intent.getIntExtra(EXTRA_SECONDS, 10)
        val usedMin = intent.getLongExtra(EXTRA_USED_MS, 0L) / 60_000
        val limit = intent.getIntExtra(EXTRA_LIMIT_MIN, 0)
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)

        val goHome = {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
        setContent {
            EnfoqueTheme {
                BackHandler { goHome() } // "atrás" cuenta como "mejor no"
                PauseScreen(label, usedMin, limit, seconds, onNo = goHome, onContinue = { PauseState.allow(pkg); finish() })
            }
        }
    }

    companion object {
        const val EXTRA_SECONDS = "seconds"
        const val EXTRA_USED_MS = "used_ms"
        const val EXTRA_LIMIT_MIN = "limit_min"
    }
}

@Composable
private fun PauseScreen(app: String, usedMin: Long, limit: Int, seconds: Int, onNo: () -> Unit, onContinue: () -> Unit) {
    var left by remember { mutableIntStateOf(seconds) }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { left.toFloat() / seconds.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxSize(), strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
                Text("$left", fontSize = 56.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
            Text("Antes de abrir $app", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("¿Es esto lo que quieres hacer ahora?", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Hoy llevas $usedMin de $limit min en $app.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onNo, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Mejor no") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onContinue, enabled = left == 0) {
                Text(if (left > 0) "Continuar ($left)" else "Continuar")
            }
        }
    }
}
