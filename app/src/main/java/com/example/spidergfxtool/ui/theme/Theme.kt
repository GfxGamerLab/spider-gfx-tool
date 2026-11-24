package com.example.spidergfxtool.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFFF1744),
    tertiary = Color(0xFF00E5FF),
    background = Color(0xFF060308),
    surface = Color(0xFF060308),
    surfaceVariant = Color(0xFF120808),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun SpiderGFXToolTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberDarkColorScheme,
        typography = Typography,
        content = content
    )
}
