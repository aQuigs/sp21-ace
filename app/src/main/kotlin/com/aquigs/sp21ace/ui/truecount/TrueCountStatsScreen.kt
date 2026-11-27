package com.aquigs.sp21ace.ui.truecount

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.start
import com.aquigs.sp21ace.domain.truecount.Decks
import com.aquigs.sp21ace.domain.truecount.ShoeStats
import com.aquigs.sp21ace.domain.truecount.TRUE_COUNT_PAD
import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import com.aquigs.sp21ace.domain.truecount.TrueCountSquare
import com.aquigs.sp21ace.domain.truecount.trueCountStats
import com.aquigs.sp21ace.ui.components.AccuracyCard
import com.aquigs.sp21ace.ui.components.HEAT_SCALE
import com.aquigs.sp21ace.ui.components.HeatScale
import com.aquigs.sp21ace.ui.components.HeatSquare
import com.aquigs.sp21ace.ui.components.LabeledGrid
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.SectionHeading
import com.aquigs.sp21ace.ui.components.StatCards
import com.aquigs.sp21ace.ui.components.StatPage
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.rememberNow
import com.aquigs.sp21ace.ui.components.rememberStats
import com.aquigs.sp21ace.ui.flashcards.SpeedCard
import com.aquigs.sp21ace.ui.flashcards.StatNote
import java.time.Instant

/**
 * How the true count flashcards went over a period, a section for each shoe answered: how often they were right, square by square
 * too, and how long the timed ones took. [now] is read again when the screen resumes and every minute, so answers age out of a
 * period while it's open.
 */
@Composable
fun TrueCountStatsScreen(history: List<TrueCountAnswer>, onBack: () -> Unit, modifier: Modifier = Modifier, now: () -> Instant = Instant::now) {
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    val asOf = rememberNow(now)
    val stats = rememberStats(period, asOf, history) { history.trueCountStats(period, asOf) }

    SubPage(title = stringResource(R.string.true_count_stats), onBack = onBack, modifier = modifier) { padding ->
        StatPage(period = period, onPeriod = { period = it }, modifier = Modifier.fillMaxSize().padding(padding)) {
            if (stats.isEmpty()) StatCards { StatNote(stringResource(R.string.no_true_count_answers)) }

            stats.forEach { shoe ->
                StatCards {
                    SectionHeading(deckCount(shoe.shoe))
                    AccuracyCard(title = stringResource(R.string.accuracy), tally = shoe.tally)
                }
                // Wider than the cards, as the trainer's heatmap is, so its squares stay as large as they can be
                TrueCountHeatmap(
                    shoe,
                    Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp).widthIn(max = MaxContentWidth),
                )
                StatCards { SpeedCard(shoe.times, answered = true) }
            }
        }
    }
}

/** A heatmap's row of cards with [decks] left, [spoken] in words. */
private data class DecksRow(val decks: Decks, val spoken: String)

/** A heatmap's column of cards answered [answer], [label] at its head and [spoken] in words. */
private data class AnswerColumn(val answer: Int, val label: String, val spoken: String)

private val ANSWER_COLUMNS = TRUE_COUNT_PAD.numbers.map { AnswerColumn(it, TRUE_COUNT_PAD.label(it), TRUE_COUNT_PAD.inPlainWords(it)) }

/** [stats]' decks left by the right answer, each square with answers showing how often they were right, over a scale from 0 to 100. */
@Composable
private fun TrueCountHeatmap(stats: ShoeStats, modifier: Modifier = Modifier) {
    LabeledGrid(
        // Worded here rather than in every square
        rows = stats.decksLeft.map { DecksRow(it, decksLeftText(it)) },
        columns = ANSWER_COLUMNS,
        rowLabel = { it.decks.label },
        columnLabel = { it.label },
        rowCaption = stringResource(R.string.decks_left_caption),
        columnCaption = stringResource(R.string.true_count),
        // Its 100 is as wide as a square's percentage gets
        widestCodes = HEAT_SCALE,
        widestRowLabels = stats.decksLeft.map { it.label },
        modifier = modifier,
        square = { row, column, codes, squareModifier ->
            val tally = stats.bySquare[TrueCountSquare(row.decks, column.answer)]
            val percent = tally?.accuracyPermille?.let { it / 10 }
            val description = if (percent == null) {
                stringResource(R.string.true_count_square_no_answers, row.spoken, column.spoken)
            } else {
                stringResource(R.string.true_count_square_accuracy, row.spoken, column.spoken, percent)
            }

            HeatSquare(percent?.toString().orEmpty(), tally, description, codes, squareModifier)
        },
        footer = { swatchSize, gap, codes -> HeatScale(swatchSize, gap, codes) },
    )
}
