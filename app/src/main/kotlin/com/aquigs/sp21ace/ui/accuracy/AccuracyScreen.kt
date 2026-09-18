package com.aquigs.sp21ace.ui.accuracy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Accuracy
import com.aquigs.sp21ace.domain.history.HandFilter
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.accuracy
import com.aquigs.sp21ace.ui.chart.title
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.PageTabRow
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.displayName
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * How often the trainer's answers were right over a period, for one kind of hand or all, and by the move each hand called
 * for. [now] is read again when the screen resumes and every minute, so answers age out of a period while it's open.
 */
@Composable
fun AccuracyScreen(
    history: List<PracticeAnswer>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> Instant = Instant::now,
) {
    var hands by rememberSaveable { mutableStateOf(HandFilter.HARD) }
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    val currentNow by rememberUpdatedState(now)
    var asOf by remember { mutableStateOf(now()) }
    val accuracy = remember(history, asOf, period, hands) { history.accuracy(period, hands, asOf) }

    // Nothing else changes with time, so without these a screen left open, or reopened hours later, keeps counting answers
    // that have aged out of the period
    LifecycleResumeEffect(Unit) {
        asOf = currentNow()
        onPauseOrDispose {}
    }
    LaunchedEffect(asOf) {
        delay(1.minutes)
        asOf = currentNow()
    }

    SubPage(title = stringResource(R.string.accuracy), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PageTabRow(tabs = HandFilter.entries, selected = hands, onSelect = { hands = it }, title = { it.table?.title ?: R.string.all_hands })

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PeriodChips(selected = period, onSelect = { period = it })
                Cards(accuracy, hands, Modifier.widthIn(max = MaxContentWidth).fillMaxWidth().padding(top = 32.dp, bottom = 16.dp))
            }
        }
    }
}

@Composable
private fun PeriodChips(selected: Period, onSelect: (Period) -> Unit) {
    // Wraps rather than running off a narrow screen at a large font size
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        Period.entries.forEach { period ->
            FilterChip(selected = period == selected, onClick = { onSelect(period) }, label = { Text(stringResource(period.title)) })
        }
    }
}

@Composable
private fun Cards(accuracy: Accuracy, hands: HandFilter, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(48.dp)) {
        // Only All has the Streak card, as in Blackjack Ace, since the streak runs over every answer whatever the tab or period
        if (hands == HandFilter.ALL) StreakCard(longest = accuracy.longestStreak)
        AccuracyCard(title = stringResource(R.string.overall), tally = accuracy.overall)
        hands.moves.forEach { AccuracyCard(title = stringResource(it.displayName), tally = accuracy.byMove.getValue(it)) }
    }
}

/** A titled block of figures, which a screen reader reads as one item, each figure before its label. */
@Composable
private fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        content()
    }
}

@Composable
private fun StreakCard(longest: Int) {
    StatCard(title = stringResource(R.string.streak)) {
        Figure(value = longest.toString(), label = stringResource(R.string.longest_streak), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AccuracyCard(title: String, tally: Tally) {
    StatCard(title = title) {
        Figure(
            value = tally.accuracyPermille?.let { stringResource(R.string.percentage, it / 10.0) },
            label = stringResource(R.string.accuracy),
            color = MaterialTheme.colorScheme.primary,
        )
        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Count(count = tally.correct, label = stringResource(R.string.correct), modifier = Modifier.weight(1f))
            Count(count = tally.incorrect, label = stringResource(R.string.incorrect), modifier = Modifier.weight(1f))
        }
    }
}

/** A large figure over its label, or "--" in plain text when there is nothing to count. */
@Composable
private fun Figure(value: String?, label: String, color: Color) {
    Text(
        text = value ?: stringResource(R.string.no_data),
        color = if (value == null) MaterialTheme.colorScheme.onSurface else color,
        style = MaterialTheme.typography.displaySmall,
    )
    Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Count(count: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        Text(text = count.toString(), style = MaterialTheme.typography.titleMedium)
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}

private val Period.title: Int
    get() = when (this) {
        Period.TODAY -> R.string.today
        Period.WEEK -> R.string.week
        Period.MONTH -> R.string.month
        Period.ALL_TIME -> R.string.all_time
    }
