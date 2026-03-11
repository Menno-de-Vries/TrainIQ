package com.trainiq.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF126B50),
    onPrimary = Color.White,
    secondary = Color(0xFF3B6B91),
    tertiary = Color(0xFF9B5B22),
    background = Color(0xFFF4F7F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2ECE5),
    onSurface = Color(0xFF17211D),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF67D4AA),
    secondary = Color(0xFF9BC9F3),
    tertiary = Color(0xFFFFB782),
    background = Color(0xFF0E1512),
    surface = Color(0xFF14201B),
    surfaceVariant = Color(0xFF22322C),
    onSurface = Color(0xFFEAF3ED),
)

@Composable
fun TrainIqTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
