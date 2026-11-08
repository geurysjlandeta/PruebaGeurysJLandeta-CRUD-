package com.geurys.enfoque

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class BlockedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PKG).orEmpty()
        val focus = intent.getBooleanExtra(EXTRA_FOCUS, false)
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val isWa = pkg in NotificationService.WHATSAPP
        setContent {
            MaterialTheme {
                BlockedScreen(label, focus, isWa,
                    onMessages = {
                        startActivity(Intent(this, MainActivity::class.java).putExtra("tab", 3))
                        finish()
                    },
                    onClose = { finish() })
            }
        }
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val EXTRA_FOCUS = "focus"
    }
}

@Composable
private fun BlockedScreen(app: String, focus: Boolean, isWhatsApp: Boolean, onMessages: () -> Unit, onClose: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Respira 🌿", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                if (focus) "$app está bloqueada mientras estás en modo Enfoque."
                else "Llegaste a tu límite de hoy en $app.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Lo que estabas por hacer puede esperar.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            if (isWhatsApp) {
                Button(onClick = onMessages) { Text("Solo responder mensajes") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onClose) { Text("Entendido") }
            } else {
                Button(onClick = onClose) { Text("Entendido") }
            }
        }
    }
}
