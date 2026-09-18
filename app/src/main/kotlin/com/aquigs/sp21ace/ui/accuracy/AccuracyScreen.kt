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
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
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
import com.aquigs.sp21ace.domain.history.nextMidnight
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.trainer.displayName
import kotlinx.coroutines.delay
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// As wide as the strategy chart grows, so a tablet doesn't spread a card's figures apart
private val MaxCardWidth = 480.dp

/**
 * How often the trainer's answers were right over a period, for one kind of hand or all, and by the move each hand called
 * for. [now] is read again when the screen resumes and at local midnight, so Today moves on with the calendar and the time zone.
 */
@Composable
fun AccuracyScreen(
    history: List<PracticeAnswer>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> Clock = { Clock.fixed(Instant.now(), ZoneId.systemDefault()) },
) {
    var hands by rememberSaveable { mutableStateOf(HandFilter.HARD) }
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    val currentNow by rememberUpdatedState(now)
    var clock by remember { mutableStateOf(now()) }
    val accuracy = remember(history, clock, period, hands) { history.accuracy(period, hands, clock) }

    // Nothing else changes with time, so without these a screen left open overnight, or reopened after a flight, keeps
    // counting yesterday's answers, or midnight somewhere else, as today's
    LifecycleResumeEffect(Unit) {
        clock = currentNow()
        onPauseOrDispose {}
    }
    LaunchedEffect(clock) {
        delay(Duration.between(clock.instant(), nextMidnight(clock)).toMillis())
        clock = currentNow()
    }

    SubPage(title = stringResource(R.string.accuracy), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            SecondaryTabRow(selectedTabIndex = hands.ordinal) {
                HandFilter.entries.forEach { tab ->
                    Tab(
                        selected = tab == hands,
                        onClick = { hands = tab },
                        text = { Text(stringResource(tab.title)) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PeriodChips(selected = period, onSelect = { period = it })
                Cards(accuracy, hands, Modifier.widthIn(max = MaxCardWidth).fillMaxWidth().padding(top = 32.dp, bottom = 16.dp))
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
    val overall = accuracy.overall

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(48.dp)) {
        // Only All has a streak, as in Blackjack Ace: a run within one kind of hand would count past wrong answers to the others
        if (hands == HandFilter.ALL) StreakCard(longest = accuracy.longestStreak.takeIf { overall.total > 0 })
        AccuracyCard(title = stringResource(R.string.overall), tally = overall)
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
private fun StreakCard(longest: Int?) {
    StatCard(title = stringResource(R.string.streak)) {
        Figure(value = longest?.toString(), label = stringResource(R.string.longest_streak), color = MaterialTheme.colorScheme.onSurface)
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

private val HandFilter.title: Int
    get() = when (this) {
        HandFilter.HARD -> R.string.table_hard
        HandFilter.SOFT -> R.string.table_soft
        HandFilter.PAIRS -> R.string.table_pairs
        HandFilter.ALL -> R.string.all_hands
    }

private val Period.title: Int
    get() = when (this) {
        Period.TODAY -> R.string.today
        Period.WEEK -> R.string.week
        Period.MONTH -> R.string.month
        Period.ALL_TIME -> R.string.all_time
    }
