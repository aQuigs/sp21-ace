package com.aquigs.sp21ace.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Lays out [content] at the default font size, whatever the system's. Text inside a control that keeps its size, as Blackjack
 * Ace's answer buttons and chart tile do, has no room to grow, so a larger font would only cut it short.
 */
@Composable
internal fun ProvideDefaultFontScale(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 1f), content = content)
}
