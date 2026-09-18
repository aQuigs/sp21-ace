package com.aquigs.sp21ace.ui.settings

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onRoot

/** Whether the page on screen shows the dark theme, read from its margin, clear of any row's text. */
fun SemanticsNodeInteractionsProvider.pageIsDark(): Boolean =
    onRoot().captureToImage().toPixelMap().let { it[4, it.height * 3 / 4].luminance() < 0.5f }
