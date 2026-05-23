package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BizoColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = GraySurface,
    onSecondary = Black,
    background = White,
    onBackground = Black,
    surface = White,
    onSurface = Black,
    surfaceVariant = GraySurface,
    onSurfaceVariant = Black,
    outline = GrayBorder,
    outlineVariant = GrayText
)

@Composable
fun BizoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BizoColorScheme,
        typography = Typography,
        content = content
    )
}
