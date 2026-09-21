package com.aquigs.sp21ace.ui.accuracy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Accuracy
import com.aquigs.sp21ace.domain.history.HandFilter
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.accuracy
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.ui.chart.tabTitle
import com.aquigs.sp21ace.ui.components.Count
import com.aquigs.sp21ace.ui.components.CountRow
import com.aquigs.sp21ace.ui.components.DoubledTabbedPages
import com.aquigs.sp21ace.ui.components.Figure
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.StatCard
import com.aquigs.sp21ace.ui.components.StatCards
import com.aquigs.sp21ace.ui.components.StatPage
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.components.percentText
import com.aquigs.sp21ace.ui.components.rememberNow
import java.time.Instant

/**
 * How often the trainer's answers were right over a period, for one kind of hand or all those not yet doubled or all those
 * already doubled, by the move each hand called for, and for one kind of hand square by square over [rules]' chart. [now] is
 * read again when the screen resumes and every minute, so answers age out of a period while it's open.
 */
@Composable
fun AccuracyScreen(
    history: List<PracticeAnswer>,
    rules: RuleSet,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    now: () -> Instant = Instant::now,
) {
    val chart = StrategyCharts.forRules(rules)
    // A tab for every table the chart prints, as the chart has, and an All for each group
    val tabs = remember(chart) { HandFilter.entries.filter { it.table == null || it.table in chart.tables } }
    // One period for every tab, as in Blackjack Ace, though each page has its own chips to slide in with it
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    val asOf = rememberNow(now)

    SubPage(title = stringResource(R.string.accuracy), onBack = onBack, modifier = modifier) { padding ->
        DoubledTabbedPages(
            tabs = tabs,
            doubled = HandFilter::doubled,
            title = { filter -> filter.table?.tabTitle ?: R.string.all_hands },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { hands ->
            val accuracy = remember(history, asOf, period, hands) { history.accuracy(period, hands, asOf) }

            // The grid spreads wider than the chips and cards, as in Blackjack Ace, so its squares stay as large as the chart's
            StatPage(period = period, onPeriod = { period = it }, modifier = Modifier.fillMaxWidth()) {
                hands.table?.let { table ->
                    AccuracyHeatmap(
                        rules = rules,
                        table = table,
                        bySquare = accuracy.bySquare,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp).widthIn(max = MaxContentWidth),
                    )
                }
                Cards(accuracy, hands)
            }
        }
    }
}

@Composable
private fun Cards(accuracy: Accuracy, hands: HandFilter) {
    StatCards {
        // Only All has the Streak card, as in Blackjack Ace, since the streak runs over every answer whatever the tab or period
        if (hands.table == null) StreakCard(longest = accuracy.longestStreak)
        AccuracyCard(title = stringResource(R.string.overall), tally = accuracy.overall)
        hands.moves.forEach { AccuracyCard(title = stringResource(it.displayName), tally = accuracy.byMove.getValue(it)) }
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
            value = tally.percentText(),
            label = stringResource(R.string.accuracy),
            color = MaterialTheme.colorScheme.primary,
        )
        CountRow {
            Count(value = tally.correct.toString(), label = stringResource(R.string.correct), modifier = Modifier.weight(1f))
            Count(value = tally.incorrect.toString(), label = stringResource(R.string.incorrect), modifier = Modifier.weight(1f))
        }
    }
}
