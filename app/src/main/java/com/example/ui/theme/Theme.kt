package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberKioskColorScheme =
  darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = NeonGreen,
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF005328),
    onSecondaryContainer = Color(0xFF70FF9C),
    tertiary = NeonCoral,
    onTertiary = Color(0xFF5F001B),
    background = CyberDarkBackground,
    onBackground = TextPrimary,
    surface = CyberCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = CyberCardBorder,
    error = NeonCoral,
    onError = Color.White
  )

@Composable
fun SmartVendingTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CyberKioskColorScheme,
    typography = Typography,
    content = content
  )
}

