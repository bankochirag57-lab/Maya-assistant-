package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = KingPrimary,
    onPrimary = NavyBackground,
    primaryContainer = NavySurfaceVariant,
    onPrimaryContainer = KingPrimary,
    secondary = KingSecondary,
    onSecondary = NavyBackground,
    secondaryContainer = NavySurfaceVariant,
    onSecondaryContainer = KingSecondary,
    tertiary = MayaPrimary,
    onTertiary = Color.White,
    background = NavyBackground,
    onBackground = HudTextPrimary,
    surface = NavySurface,
    onSurface = HudTextPrimary,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = HudTextSecondary,
    outline = GlassBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Preserve futuristic JARVIS palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
