package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.model.WorkstationSettings

private fun accentFor(name: String): Color = when (name.lowercase()) {
    "violet" -> Color(0xFF8B5CF6)
    "blue" -> Color(0xFF3B82F6)
    "teal" -> Color(0xFF14B8A6)
    "magenta" -> Color(0xFFD946EF)
    "amber" -> Color(0xFFF59E0B)
    else -> CyberCyan
}

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = ObsidianBg,
    primaryContainer = SurfacePanel,
    onPrimaryContainer = accent,
    secondary = NeuralViolet,
    onSecondary = ObsidianBg,
    secondaryContainer = SurfacePanelHover,
    onSecondaryContainer = NeuralViolet,
    tertiary = TerminalGreen,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = CommandSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfacePanel,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = AlertRed,
    onError = TextPrimary
)

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = Color.White,
    primaryContainer = accent.copy(alpha = 0.14f),
    onPrimaryContainer = accent,
    secondary = DeepViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9E7FF),
    onSecondaryContainer = Color(0xFF312E81),
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8EDF4),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFB91C1C),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    settings: WorkstationSettings = WorkstationSettings(),
    content: @Composable () -> Unit
) {
    val dark = when (settings.themeMode.uppercase()) {
        "LIGHT" -> false
        "SYSTEM" -> isSystemInDarkTheme()
        else -> true
    }
    val accent = accentFor(settings.accentColor)
    val scheme = if (dark) darkScheme(accent) else lightScheme(accent)

    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content
    )
}
