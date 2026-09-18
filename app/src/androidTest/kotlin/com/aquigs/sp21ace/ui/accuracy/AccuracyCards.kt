package com.aquigs.sp21ace.ui.accuracy

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasText
import com.aquigs.sp21ace.R

/** The texts of the card holding every one of [holding]. A card is one item for a screen reader, so they come in order: the title, then each figure before its label. */
fun SemanticsNodeInteractionsProvider.cardTexts(vararg holding: String): List<String> =
    onNode(holding.map(::hasText).reduce(SemanticsMatcher::and)).fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

/** The texts an accuracy card titled [title] reads when it shows [accuracy], [correct] and [incorrect]. */
fun Context.accuracyCardTexts(title: Int, accuracy: String, correct: Int, incorrect: Int): List<String> =
    listOf(getString(title), accuracy, getString(R.string.accuracy), "$correct", getString(R.string.correct), "$incorrect", getString(R.string.incorrect))
