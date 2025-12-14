package com.example.teste2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightCard,
    onSurfaceVariant = NightOnBackground,
    onBackground = NightOnBackground,
    onSurface = NightOnBackground,
    outline = NightOutline,
    outlineVariant = NightOutline,
    error = Color(0xFFFF6B6B)
)

@Composable
fun Teste2Theme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
