package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.IndexOrder
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexStats
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.indices.flashcardStats
import com.aquigs.sp21ace.domain.indices.inPlainWords
import com.aquigs.sp21ace.domain.indices.sortedBy
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.ui.components.AccuracyCard
import com.aquigs.sp21ace.ui.components.ExpandIcon
import com.aquigs.sp21ace.ui.components.StatCards
import com.aquigs.sp21ace.ui.components.StatChips
import com.aquigs.sp21ace.ui.components.StatPage
import com.aquigs.sp21ace.ui.components.StatSection
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.percentText
import com.aquigs.sp21ace.ui.components.rememberNow
import com.aquigs.sp21ace.ui.components.rememberStats
import java.time.Instant
import kotlin.time.Duration

private val ORDERS = listOf(IndexOrder.LEAST_ACCURATE to R.string.least_accurate, IndexOrder.SLOWEST to R.string.slowest)

/**
 * How [preset]'s flashcards went over a period: how often they were right, how long the timed ones took, and each index's own
 * figures and spread of times on one axis, the troublemakers first. [now] is read again when the screen resumes and every minute,
 * so answers age out of a period while it's open.
 */
@Composable
fun FlashcardStatsScreen(
    history: List<FlashcardAnswer>,
    preset: IndexPreset,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> Instant = Instant::now,
) {
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    var order by rememberSaveable { mutableStateOf(IndexOrder.LEAST_ACCURATE) }
    // Here rather than with the rows, which a period with no answers takes off the page, so rows opened stay open past it
    var expanded by rememberSaveable { mutableStateOf(emptySet<PlayIndex?>()) }
    val asOf = rememberNow(now)
    val stats = rememberStats(period, asOf, history, preset) { history.flashcardStats(preset, period, asOf) }
    val rows = remember(stats, order) { stats.indices.sortedBy(order) }

    SubPage(title = stringResource(R.string.flashcard_stats), onBack = onBack, modifier = modifier) { padding ->
        StatPage(period = period, onPeriod = { period = it }, modifier = Modifier.fillMaxSize().padding(padding)) {
            StatCards {
                AccuracyCard(title = stringResource(R.string.accuracy), tally = stats.overall)
                SpeedCard(stats.times, answered = stats.overall.total > 0)
                StatSection(title = stringResource(R.string.indices)) {
                    if (stats.overall.total == 0) {
                        StatNote(stringResource(R.string.no_answers_in_period))
                    } else {
                        IndexRows(
                            rows,
                            axisEnd = stats.axisEnd.takeIf { stats.times.timed > 0 },
                            order,
                            onOrder = { order = it },
                            expanded = expanded,
                            onToggle = { index -> expanded = if (index in expanded) expanded - index else expanded + index },
                        )
                    }
                }
            }
        }
    }
}

/** The rows in [order] under its chips, and over them the time axis, which ends at [axisEnd], or none while nothing was timed. */
@Composable
private fun IndexRows(
    rows: List<IndexStats>,
    axisEnd: Duration?,
    order: IndexOrder,
    onOrder: (IndexOrder) -> Unit,
    expanded: Set<PlayIndex?>,
    onToggle: (PlayIndex?) -> Unit,
) {
    // Chips like the period's, which wrap at a large font where a segmented button's halves would squeeze their words
    StatChips(ORDERS, order, onOrder)
    axisEnd?.let { TimeAxis(it, Modifier.padding(top = 16.dp).fillMaxWidth()) }

    rows.forEach { row ->
        IndexRow(row, axisEnd, expanded = row.index in expanded, onToggle = { onToggle(row.index) })
        HorizontalDivider()
    }
}

/**
 * An index's name and number, or that the hands have none, its accuracy, answers and median time, and its spread of times on the
 * axis every row shares. A row with timed answers opens and closes its own histogram, shown by its chevron, and a screen reader
 * hears it as one item.
 */
@Composable
private fun IndexRow(row: IndexStats, axisEnd: Duration?, expanded: Boolean, onToggle: () -> Unit) {
    val spread = row.times.spread
    val title = row.index?.deviation?.title ?: stringResource(R.string.no_index)
    val words = row.index?.inPlainWords ?: stringResource(R.string.hands_with_no_index)
    val toggle = if (spread == null || axisEnd == null) {
        Modifier.semantics(mergeDescendants = true) {}
    } else {
        val state = stringResource(if (expanded) R.string.expanded else R.string.collapsed)
        Modifier
            .clickable(onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand), onClick = onToggle)
            .semantics { stateDescription = state }
    }

    Column(modifier = Modifier.fillMaxWidth().then(toggle).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            if (spread != null) ExpandIcon(expanded)
        }
        Text(
            text = if (row.enabled) words else stringResource(R.string.index_switched_off, words),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )

        if (row.answers == 0) StatNote(stringResource(R.string.no_answers)) else Figures(row, axisEnd, expanded)
    }
}

@Composable
private fun Figures(row: IndexStats, axisEnd: Duration?, expanded: Boolean) {
    val spread = row.times.spread

    // Each figure keeps to one piece, so a large font wraps between them rather than inside one
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = stringResource(R.string.index_accuracy, row.tally.percentText().orEmpty()), style = MaterialTheme.typography.bodyMedium)
        Text(text = pluralStringResource(R.plurals.answer_count, row.answers, row.answers), style = MaterialTheme.typography.bodyMedium)
        spread?.let { Text(text = stringResource(R.string.index_median, secondsText(it.median)), style = MaterialTheme.typography.bodyMedium) }
    }
    if (spread == null || axisEnd == null) {
        StatNote(stringResource(R.string.no_timed_answers))
    } else {
        TimeSpreadStrip(spread, axisEnd, Modifier.padding(top = 4.dp))
        if (expanded) TimeHistogramChart(row.times.histogram, Modifier.padding(top = 16.dp).fillMaxWidth())
    }
}
