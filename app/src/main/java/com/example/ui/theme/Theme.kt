package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = M3Blue,
    onPrimary = M3BlueInner,
    primaryContainer = M3BlueInner,
    onPrimaryContainer = M3Blue,
    secondary = M3Green,
    onSecondary = M3GreenInner,
    tertiary = M3Red,
    onTertiary = M3RedInner,
    background = ImmersiveBg,
    onBackground = ImmersiveText,
    surface = ImmersiveSurface,
    onSurface = ImmersiveText,
    surfaceVariant = ImmersiveCellBg,
    onSurfaceVariant = ImmersiveTextMuted,
    outline = ImmersiveBorder,
    outlineVariant = ImmersiveOutline
  )

private val LightColorScheme = DarkColorScheme // Standardize on luxurious immersive theme

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark Theme for Immersive UI feel
  dynamicColor: Boolean = false, // Disable system dynamic color overrides
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
