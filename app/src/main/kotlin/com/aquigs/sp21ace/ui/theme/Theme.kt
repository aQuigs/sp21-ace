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

/** Colours with no Material role: dark theme's primary is a light tint, but the app bar stays a deep navy. */
@Immutable
data class Sp21AceColors(
    val appBar: Color,
    val onAppBar: Color,
)

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

private val LightSp21AceColors = Sp21AceColors(
    appBar = LightColors.primary,
    onAppBar = LightColors.onPrimary,
)

// Deeper than the light theme's navy so a full-width bar doesn't glare against charcoal.
private val DarkSp21AceColors = Sp21AceColors(
    appBar = Color(0xFF1B2C42),
    onAppBar = DarkColors.onSurface,
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
