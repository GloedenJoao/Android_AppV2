package com.example.teste2.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Ink,
    secondary = Mint,
    onSecondary = Ink,
    tertiary = Amber,
    background = Midnight,
    surface = Gunmetal,
    onSurface = ColorText,
    onBackground = ColorText,
    error = Coral,
    onError = Ink
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
