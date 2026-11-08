package com.geurys.enfoque

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF475569), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0), onSecondaryContainer = Color(0xFF0F172A),
    background = Color(0xFFF8FAFC), onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC), onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0), onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8), outlineVariant = Color(0xFFCBD5E1),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFE9EEF4), surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFDCE3EB),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF), onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFF94A3B8), onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B), onSecondaryContainer = Color(0xFFE2E8F0),
    background = Color(0xFF0B1220), onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0B1220), onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B), onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B), outlineVariant = Color(0xFF334155),
    surfaceContainerLowest = Color(0xFF080D18), surfaceContainerLow = Color(0xFF111A2B),
    surfaceContainer = Color(0xFF162033), surfaceContainerHigh = Color(0xFF1C2940),
    surfaceContainerHighest = Color(0xFF22314B),
)

@Composable
fun EnfoqueTheme(forceDark: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (forceDark || isSystemInDarkTheme()) DarkScheme else LightScheme,
        content = content
    )
}
