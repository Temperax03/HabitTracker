package com.example.habittracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Világos mód
val LightPurpleScheme = lightColorScheme(
    primary = Color(0xFF6A1B9A),
    onPrimary = Color.White,
    secondary = Color(0xFF9C27B0),
    onSecondary = Color.White,
    background = Color(0xFFF3E5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

// Sötét mód
val DarkPurpleScheme = darkColorScheme(
    primary = Color(0xFFCE93D8),
    onPrimary = Color(0xFF1A1A1A),
    secondary = Color(0xFFBA68C8),
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFEDEDED),
    error = Color(0xFFEF9A9A),
    onError = Color(0xFF1A1A1A)
)
