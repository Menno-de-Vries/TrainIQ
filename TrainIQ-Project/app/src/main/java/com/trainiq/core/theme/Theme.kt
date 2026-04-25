package com.trainiq.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF45DCAF),
    onPrimary = Color(0xFF031A18),
    secondary = Color(0xFF5B9DFF),
    onSecondary = Color(0xFF071527),
    tertiary = Color(0xFFFFBE55),
    onTertiary = Color(0xFF261600),
    background = Color(0xFFF4F7F9),
    onBackground = Color(0xFF111820),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111820),
    surfaceVariant = Color(0xFFE9EEF4),
    onSurfaceVariant = Color(0xFF566270),
    primaryContainer = Color(0xFFD7FAEC),
    onPrimaryContainer = Color(0xFF0A2D26),
    secondaryContainer = Color(0xFFD9E8FF),
    onSecondaryContainer = Color(0xFF0B2547),
    tertiaryContainer = Color(0xFFFFE9BE),
    onTertiaryContainer = Color(0xFF3A2300),
    outline = Color(0xFFBBC5D1),
    outlineVariant = Color(0xFFD9E0E8),
    error = Color(0xFFFF6B7A),
    errorContainer = Color(0xFFFFD7DC),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF64E1B5),
    onPrimary = Color(0xFF061712),
    secondary = Color(0xFF5FA2FF),
    onSecondary = Color(0xFF071322),
    tertiary = Color(0xFFFFBE55),
    onTertiary = Color(0xFF201300),
    background = Color(0xFF080D12),
    onBackground = Color(0xFFF2F6FA),
    surface = Color(0xFF0B1016),
    onSurface = Color(0xFFF2F6FA),
    surfaceVariant = Color(0xFF151C25),
    onSurfaceVariant = Color(0xFFA8B2C0),
    primaryContainer = Color(0xFF203F3B),
    onPrimaryContainer = Color(0xFF8BF4CE),
    secondaryContainer = Color(0xFF182D48),
    onSecondaryContainer = Color(0xFF90BEFF),
    tertiaryContainer = Color(0xFF3F3020),
    onTertiaryContainer = Color(0xFFFFCC74),
    outline = Color(0xFF384554),
    outlineVariant = Color(0xFF24303C),
    error = Color(0xFFFF7480),
    errorContainer = Color(0xFF4C1F27),
)

private val TrainIqTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.25.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
)

@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val compact: Dp = 12.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
)

@Immutable
data class TrainIqRadii(
    val chip: Dp = 18.dp,
    val card: Dp = 22.dp,
    val button: Dp = 24.dp,
    val nav: Dp = 30.dp,
    val sheet: Dp = 28.dp,
)

@Immutable
data class TrainIqColors(
    val appBackground: Color = Color(0xFF080D12),
    val backgroundGlow: Color = Color(0xFF031E25),
    val card: Color = Color(0xFF151C25),
    val cardElevated: Color = Color(0xFF1B2430),
    val cardBorder: Color = Color(0xFF3A4656),
    val mutedText: Color = Color(0xFFA8B2C0),
    val track: Color = Color(0xFF313A46),
    val mint: Color = Color(0xFF64E1B5),
    val blue: Color = Color(0xFF5FA2FF),
    val amber: Color = Color(0xFFFFBE55),
    val purple: Color = Color(0xFFB978FF),
    val cyan: Color = Color(0xFF69D6FF),
)

private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalRadii = staticCompositionLocalOf { TrainIqRadii() }
private val LocalTrainIqColors = staticCompositionLocalOf { TrainIqColors() }

val MaterialTheme.spacing: Spacing
    @Composable
    get() = LocalSpacing.current

val MaterialTheme.radii: TrainIqRadii
    @Composable
    get() = LocalRadii.current

val MaterialTheme.trainIqColors: TrainIqColors
    @Composable
    get() = LocalTrainIqColors.current

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
    val colorScheme = when {
        darkTheme -> DarkColors
        else -> LightColors
    }
    val designColors = if (darkTheme) {
        TrainIqColors()
    } else {
        TrainIqColors(
            appBackground = Color(0xFFF4F7F9),
            backgroundGlow = Color(0xFFD9F4F3),
            card = Color.White,
            cardElevated = Color(0xFFF8FAFC),
            cardBorder = Color(0xFFD7DFE8),
            mutedText = Color(0xFF566270),
            track = Color(0xFFD8E0EA),
            mint = Color(0xFF12A982),
            blue = Color(0xFF2B7BEA),
            amber = Color(0xFFD98918),
            purple = Color(0xFF8F51E8),
            cyan = Color(0xFF168CBF),
        )
    }
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalRadii provides TrainIqRadii(),
        LocalTrainIqColors provides designColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TrainIqTypography,
            content = content,
        )
    }
}
