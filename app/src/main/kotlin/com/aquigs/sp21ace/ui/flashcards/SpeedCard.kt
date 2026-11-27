package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.AnswerTimes
import com.aquigs.sp21ace.ui.components.Count
import com.aquigs.sp21ace.ui.components.CountRow
import com.aquigs.sp21ace.ui.components.Figure
import com.aquigs.sp21ace.ui.components.StatCard

/**
 * How long the timed ones of some answers took, or with none [answered], nothing but dashes. An interrupted answer has no time.
 */
@Composable
internal fun SpeedCard(times: AnswerTimes, answered: Boolean) {
    val spread = times.spread
    // Said in plain words, since the figure is where the slowest tenth begins, not how long it took
    val p90Spoken = spread?.let { stringResource(R.string.percentile_90_spoken, it.p90.inSeconds) }

    StatCard(title = stringResource(R.string.speed)) {
        Figure(value = spread?.median?.let { secondsText(it) }, label = stringResource(R.string.median_time), color = MaterialTheme.colorScheme.primary)
        CountRow {
            Count(
                value = spread?.p90?.let { secondsText(it) } ?: stringResource(R.string.no_data),
                label = stringResource(R.string.percentile_90),
                modifier = Modifier.weight(1f).then(if (p90Spoken == null) Modifier else Modifier.clearAndSetSemantics { contentDescription = p90Spoken }),
            )
            Count(value = times.timed.toString(), label = stringResource(R.string.timed_answers), modifier = Modifier.weight(1f))
        }
        when {
            spread != null -> TimeHistogramChart(times.histogram, Modifier.padding(top = 32.dp).fillMaxWidth())
            answered -> StatNote(stringResource(R.string.none_timed), Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
internal fun StatNote(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
}
