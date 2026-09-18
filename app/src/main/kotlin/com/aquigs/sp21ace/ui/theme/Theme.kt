package com.aquigs.sp21ace.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/**
 * Colours with no Material role: dark theme's primary is a light tint, but the app bar stays a deep navy, answer
 * feedback turns it green or red, the recap of the previous hand takes a tint of the same green or red, each chart
 * action has a fill of its own, and the accuracy heatmap runs from that red through [heatmapMiddle] to that green.
 */
@Immutable
data class Sp21AceColors(
    val appBar: Color,
    val onAppBar: Color,
    val correct: Color,
    val wrong: Color,
    val chart: ChartColors,
    private val surface: Color,
    private val tintFraction: Float,
    private val heatmapMiddle: Color,
    private val heatmapReach: Float,
) {
    val correctTint: Color = lerp(surface, correct, tintFraction)
    val wrongTint: Color = lerp(surface, wrong, tintFraction)
    val heatmap: HeatmapColors = HeatmapColors(lerp(heatmapMiddle, wrong, heatmapReach), heatmapMiddle, lerp(heatmapMiddle, correct, heatmapReach))
}

@Immutable
data class ChartColors(val hit: Color, val stand: Color, val double: Color, val split: Color, val surrender: Color)

@Immutable
data class HeatmapColors(val noneRight: Color, val halfRight: Color, val allRight: Color) {
    /** The fill for a [fraction] of answers right, from 0 to 1. */
    fun at(fraction: Float): Color =
        if (fraction <= 0.5f) lerp(noneRight, halfRight, fraction * 2) else lerp(halfRight, allRight, fraction * 2 - 1)
}

// The brand saffron #D99A1E only reaches about 2.3:1 on the light surfaces, so text-bearing roles use a darker tone.
private val LightColors = lightColorScheme(
    primary = Color(0xFF1F3A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E3F7),
    onPrimaryContainer = Color(0xFF0A1E36),
    inversePrimary = Color(0xFFA9C7EE),
    secondary = Color(0xFF8A5A00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7DB9F),
    onSecondaryContainer = Color(0xFF2B1C00),
    tertiary = Color(0xFFD99A1E),
    onTertiary = Color(0xFF2B1C00),
    tertiaryContainer = Color(0xFFF7DB9F),
    onTertiaryContainer = Color(0xFF2B1C00),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFAF7F2),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEDE6DA),
    onSurfaceVariant = Color(0xFF4E4639),
    inverseSurface = Color(0xFF34302A),
    inverseOnSurface = Color(0xFFF7F0E7),
    outline = Color(0xFF7F7667),
    outlineVariant = Color(0xFFD1C6B5),
    surfaceBright = Color(0xFFFAF7F2),
    surfaceDim = Color(0xFFDDD8D0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F1EA),
    surfaceContainer = Color(0xFFEFEAE2),
    surfaceContainerHigh = Color(0xFFE9E4DC),
    surfaceContainerHighest = Color(0xFFE3DED6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9C7EE),
    onPrimary = Color(0xFF0F2A4A),
    primaryContainer = Color(0xFF28486F),
    onPrimaryContainer = Color(0xFFD4E3F7),
    inversePrimary = Color(0xFF1F3A5F),
    secondary = Color(0xFFEBB04A),
    onSecondary = Color(0xFF402B00),
    secondaryContainer = Color(0xFF5B420F),
    onSecondaryContainer = Color(0xFFFFDFA6),
    tertiary = Color(0xFFEBB04A),
    onTertiary = Color(0xFF402B00),
    tertiaryContainer = Color(0xFF5B420F),
    onTertiaryContainer = Color(0xFFFFDFA6),
    background = Color(0xFF161513),
    onBackground = Color(0xFFEAE3D9),
    surface = Color(0xFF161513),
    onSurface = Color(0xFFEAE3D9),
    surfaceVariant = Color(0xFF4D4639),
    onSurfaceVariant = Color(0xFFD1C6B5),
    inverseSurface = Color(0xFFEAE3D9),
    inverseOnSurface = Color(0xFF34302A),
    outline = Color(0xFF9A9082),
    outlineVariant = Color(0xFF4D4639),
    surfaceBright = Color(0xFF3C3A36),
    surfaceDim = Color(0xFF161513),
    surfaceContainerLowest = Color(0xFF100F0D),
    surfaceContainerLow = Color(0xFF1E1D1A),
    surfaceContainer = Color(0xFF22211E),
    surfaceContainerHigh = Color(0xFF2C2B28),
    surfaceContainerHighest = Color(0xFF373632),
)

internal val LightSp21AceColors = Sp21AceColors(
    appBar = LightColors.primary,
    onAppBar = LightColors.onPrimary,
    correct = Color(0xFF2E7D32),
    wrong = LightColors.error,
    chart = ChartColors(
        hit = Color(0xFFF7C5B8),
        stand = Color(0xFFCFE4C6),
        double = Color(0xFFF5DC9C),
        split = Color(0xFFC3D7F0),
        surrender = Color(0xFFE6CCE3),
    ),
    surface = LightColors.surface,
    tintFraction = 0.15f,
    // The heatmap's ends stop halfway from white to the feedback red and green, so the dark code reads on every step
    heatmapMiddle = Color.White,
    heatmapReach = 0.5f,
)

// Deeper than the light theme's tones so a full-width bar doesn't glare against charcoal, and a stronger tint, because
// charcoal swallows a faint one. The chart fills go deep rather than pastel, so the dark theme's light text reads on them,
// and the heatmap runs all the way to the deep feedback red and green, through a warm grey where white would glare.
internal val DarkSp21AceColors = Sp21AceColors(
    appBar = Color(0xFF1B2C42),
    onAppBar = DarkColors.onSurface,
    correct = Color(0xFF2F6F3A),
    wrong = Color(0xFF9E2A24),
    chart = ChartColors(
        hit = Color(0xFF74402F),
        stand = Color(0xFF3E5A3A),
        double = Color(0xFF6B5320),
        split = Color(0xFF2F4C6E),
        surrender = Color(0xFF5E4260),
    ),
    surface = DarkColors.surface,
    tintFraction = 0.3f,
    heatmapMiddle = Color(0xFF57534C),
    heatmapReach = 1f,
)

private val LocalSp21AceColors = staticCompositionLocalOf { LightSp21AceColors }

@Composable
fun Sp21AceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSp21AceColors provides if (darkTheme) DarkSp21AceColors else LightSp21AceColors) {
        MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
    }
}

object Sp21AceTheme {
    val colors: Sp21AceColors
        @Composable @ReadOnlyComposable get() = LocalSp21AceColors.current
}
