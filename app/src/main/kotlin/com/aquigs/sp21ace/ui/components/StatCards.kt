package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.history.Period
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * [now] as of when the screen last resumed, read again every minute. Nothing else on a stats screen changes with time, so without
 * it a screen left open, or reopened hours later, keeps counting records that have aged out of the period.
 */
@Composable
fun rememberNow(now: () -> Instant): Instant {
    val currentNow by rememberUpdatedState(now)
    var asOf by remember { mutableStateOf(now()) }

    LifecycleResumeEffect(Unit) {
        asOf = currentNow()
        onPauseOrDispose {}
    }
    LaunchedEffect(asOf) {
        delay(1.minutes)
        asOf = currentNow()
    }
    return asOf
}

private val TextInset = 32.dp

/** A stats screen's scrolling page: Blackjack Ace's period chips over [content]. */
@Composable
fun StatPage(period: Period, onPeriod: (Period) -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PeriodChips(selected = period, onSelect = onPeriod, modifier = Modifier.padding(horizontal = TextInset))
        content()
    }
}

/** A stats page's cards, one under another. */
@Composable
fun StatCards(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.padding(start = TextInset, top = 32.dp, end = TextInset, bottom = 16.dp).widthIn(max = MaxContentWidth).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(48.dp),
        content = content,
    )
}

/** Counts side by side under a card's figure. */
@Composable
fun CountRow(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), content = content)
}

/** Blackjack Ace's Today, Week, Month and All Time chips. */
@Composable
private fun PeriodChips(selected: Period, onSelect: (Period) -> Unit, modifier: Modifier = Modifier) {
    // Wraps rather than running off a narrow screen at a large font size
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        Period.entries.forEach { period ->
            FilterChip(selected = period == selected, onClick = { onSelect(period) }, label = { Text(stringResource(period.title)) })
        }
    }
}

private val Period.title: Int
    get() = when (this) {
        Period.TODAY -> R.string.today
        Period.WEEK -> R.string.week
        Period.MONTH -> R.string.month
        Period.ALL_TIME -> R.string.all_time
    }

/** A titled block of figures, which a screen reader reads as one item, each figure before its label. */
@Composable
fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp))
        content()
    }
}

/** A large figure over its label, or "--" in plain text when there is nothing to count. */
@Composable
fun Figure(value: String?, label: String, color: Color) {
    Text(
        text = value ?: stringResource(R.string.no_data),
        color = if (value == null) MaterialTheme.colorScheme.onSurface else color,
        style = MaterialTheme.typography.displaySmall,
    )
    Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}

/** A figure under a rule and over its label, with [share] beside it, such as its percentage of the whole. */
@Composable
fun Count(value: String, label: String, modifier: Modifier = Modifier, share: String? = null) {
    Column(modifier = modifier) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = value, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            share?.let { Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
        }
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
