package com.aquigs.sp21ace.ui.accuracy

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import com.aquigs.sp21ace.R

/** The texts an accuracy card titled [title] reads when it shows [accuracy], [correct] and [incorrect]. */
fun Context.accuracyCardTexts(title: Int, accuracy: String, correct: Int, incorrect: Int): List<String> =
    listOf(getString(title), accuracy, getString(R.string.accuracy), "$correct", getString(R.string.correct), "$incorrect", getString(R.string.incorrect))

/** The heatmap square that reads [description] and prints [code], scrolled into view. Many squares print the same code, so the words find it. */
fun SemanticsNodeInteractionsProvider.square(description: String, code: String): SemanticsNodeInteraction =
    onNode(hasContentDescription(description) and hasText(code)).performScrollTo()

/** The colour a square is filled with, read near a corner, clear of the code in the middle and the outline at the edge. */
fun SemanticsNodeInteraction.fill(): Color = captureToImage().toPixelMap().let { it[it.width / 8, it.height / 8] }
