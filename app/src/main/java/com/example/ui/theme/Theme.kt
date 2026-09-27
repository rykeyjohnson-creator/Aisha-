package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CosmicColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF04101A),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = NeonCyan,
    secondary = NeonViolet,
    onSecondary = Color(0xFF1E0A3C),
    secondaryContainer = Color(0xFF381E72),
    onSecondaryContainer = NeonViolet,
    tertiary = NeonMagenta,
    onTertiary = Color.White,
    background = CosmicDeepBlack,
    onBackground = TextPrimary,
    surface = CosmicDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = CosmicCardBackground,
    onSurfaceVariant = TextSecondary,
    outline = CosmicCardBorder
)

@Composable
fun AishaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicColorScheme,
        typography = Typography,
        content = content
    )
}

// Keep alias for backwards compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    AishaTheme(content = content)
}
