package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SkyBlueLight,
    onPrimary = NavyPrimary,
    primaryContainer = NavyLight,
    onPrimaryContainer = SurfaceWhite,
    secondary = GoldWing,
    onSecondary = NavyPrimary,
    secondaryContainer = GoldDark,
    onSecondaryContainer = SurfaceWhite,
    tertiary = ExamReviewPurple,
    background = BgDark,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    surfaceVariant = SurfaceDarkVariant,
    onSurfaceVariant = TextMutedDark,
    error = ExamDangerRed,
    outline = Color(0xFF334E68)
)

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = NavyLight,
    onPrimaryContainer = SurfaceWhite,
    secondary = GoldDark,
    onSecondary = SurfaceWhite,
    secondaryContainer = GoldWing,
    onSecondaryContainer = NavyPrimary,
    tertiary = AirForceBlue,
    background = BgLight,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = TextMuted,
    error = ExamDangerRed,
    outline = BorderLight
)

@Composable
fun FutureAirmenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
