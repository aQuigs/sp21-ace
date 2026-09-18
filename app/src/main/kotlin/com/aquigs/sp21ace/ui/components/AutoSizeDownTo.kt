package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * Shrinks text to fit a box that doesn't grow with the font, from [maxFontSize] down to [minSize]. The smallest size is in dp,
 * because one in sp grows with the font too, and at a large font size leaves long words no size that fits.
 */
@Composable
@ReadOnlyComposable
internal fun autoSizeDownTo(minSize: Dp, maxFontSize: TextUnit): TextAutoSize =
    TextAutoSize.StepBased(minFontSize = with(LocalDensity.current) { minSize.toSp() }, maxFontSize = maxFontSize)
