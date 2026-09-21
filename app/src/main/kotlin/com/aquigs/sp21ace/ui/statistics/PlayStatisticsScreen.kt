package com.aquigs.sp21ace.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.PlayStats
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.history.playStats
import com.aquigs.sp21ace.ui.components.Count
import com.aquigs.sp21ace.ui.components.Figure
import com.aquigs.sp21ace.ui.components.MaxContentWidth
import com.aquigs.sp21ace.ui.components.PeriodChips
import com.aquigs.sp21ace.ui.components.StatCard
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.rememberNow
import com.aquigs.sp21ace.ui.play.chipsText
import com.aquigs.sp21ace.ui.play.profitText
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import java.time.Instant
import kotlin.math.roundToInt

private val TextInset = 32.dp
private val ChartHeight = 200.dp

// Two by two, in Blackjack Ace's order
private val GRADES = listOf(
    StrategyGrade.CORRECT to R.string.correct,
    StrategyGrade.CORRECT_WITH_HINTS to R.string.correct_with_hints,
    StrategyGrade.INCORRECT to R.string.incorrect,
    StrategyGrade.NO_ACTION_REQUIRED to R.string.no_action_required,
)

/**
 * Blackjack Ace's Play Statistics over a period: the hands won, pushed and lost, the profit or loss with a chart of it hand by
 * hand, and how closely the hands followed the chart. [now] is read again when the screen resumes and every minute.
 */
@Composable
fun PlayStatisticsScreen(history: List<PlayedHand>, onBack: () -> Unit, modifier: Modifier = Modifier, now: () -> Instant = Instant::now) {
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    val asOf = rememberNow(now)
    val stats = remember(history, asOf, period) { history.playStats(period, asOf) }

    SubPage(title = stringResource(R.string.statistics), onBack = onBack, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PeriodChips(selected = period, onSelect = { period = it }, modifier = Modifier.padding(horizontal = TextInset))
            Column(
                modifier = Modifier
                    .padding(start = TextInset, top = 32.dp, end = TextInset, bottom = 16.dp)
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(48.dp),
            ) {
                HandsCard(stats)
                BankrollCard(stats)
                StrategyCard(stats)
            }
        }
    }
}

@Composable
private fun HandsCard(stats: PlayStats) {
    val won = percentText(stats.won, stats.hands)
    val pushed = percentText(stats.pushed, stats.hands)
    val lost = percentText(stats.lost, stats.hands)
    val description = stringResource(R.string.hands_description, stats.won, won, stats.pushed, pushed, stats.lost, lost)
    // Blackjack Ace sets the push's figures under the middle of its share of the bar
    val pushCentre = if (stats.hands == 0) 0.5f else (stats.won + stats.pushed / 2f) / stats.hands

    StatCard(title = stringResource(R.string.hands)) {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
            Spread(pushCentre, stats.won.toString(), stats.pushed.toString(), stats.lost.toString(), MaterialTheme.typography.titleMedium)
            HandsBar(stats, Modifier.padding(vertical = 8.dp).fillMaxWidth().height(10.dp))
            Spread(
                pushCentre,
                stringResource(R.string.hands_won, won),
                stringResource(R.string.hands_push, pushed),
                stringResource(R.string.hands_lost, lost),
                MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/** Won at the start, lost at the end, and pushed as near [pushCentre] of the way across as fits between them. */
@Composable
private fun Spread(pushCentre: Float, won: String, pushed: String, lost: String, style: TextStyle) {
    val colors = Sp21AceTheme.colors

    Layout(
        content = {
            Text(text = won, color = colors.correct, style = style)
            Text(text = pushed, color = MaterialTheme.colorScheme.onSurfaceVariant, style = style)
            Text(text = lost, color = colors.wrong, style = style)
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val (start, middle, end) = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val width = constraints.maxWidth
        val gap = 8.dp.roundToPx()
        val lowest = start.width + gap
        val highest = width - end.width - gap - middle.width
        val x = (pushCentre * width - middle.width / 2f).roundToInt().let { if (lowest <= highest) it.coerceIn(lowest, highest) else (lowest + highest) / 2 }

        layout(width, maxOf(start.height, middle.height, end.height)) {
            start.place(0, 0)
            middle.place(x, 0)
            end.place(width - end.width, 0)
        }
    }
}

/** Blackjack Ace's bar of the hands won, pushed and lost, in proportion. */
@Composable
private fun HandsBar(stats: PlayStats, modifier: Modifier = Modifier) {
    val colors = Sp21AceTheme.colors
    val pushed = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier.clip(CircleShape)) {
        drawRect(pushed)
        if (stats.hands == 0) return@Canvas

        var x = 0f
        listOf(stats.won to colors.correctTint, stats.pushed to pushed, stats.lost to colors.wrongTint).forEach { (count, color) ->
            val width = size.width * count / stats.hands
            drawRect(color, topLeft = Offset(x, 0f), size = Size(width, size.height))
            x += width
        }
    }
}

@Composable
private fun BankrollCard(stats: PlayStats) {
    val colors = Sp21AceTheme.colors

    StatCard(title = stringResource(R.string.bankroll)) {
        Figure(
            value = profitText(stats.profit),
            label = stringResource(R.string.profit_loss),
            color = when {
                stats.profit < 0 -> colors.wrong
                stats.profit > 0 -> colors.correct
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Count(value = chipsText(stats.amountWon), label = stringResource(R.string.amount_won), modifier = Modifier.weight(1f))
            Count(value = chipsText(stats.amountLost), label = stringResource(R.string.amount_lost), modifier = Modifier.weight(1f))
        }
        ProfitChart(stats.profits, Modifier.padding(top = 32.dp).fillMaxWidth())
    }
}

/**
 * Blackjack Ace's chart of the profit after each hand, from the 0 before the first: a smoothed line over a fading fill, with 0,
 * the highest and the lowest marked.
 */
@Composable
private fun ProfitChart(profits: List<Long>, modifier: Modifier = Modifier) {
    val high = maxOf(0L, profits.max())
    val low = minOf(0L, profits.min())
    val description = stringResource(R.string.profit_chart_description, profits.lastIndex, profitText(profits.last()), profitText(low), profitText(high))
    val labels = listOfNotNull(0L, high.takeIf { it > 0 }, low.takeIf { it < 0 }).associateWith(::profitText)
    val line = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    val zero = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer = rememberTextMeasurer()

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(modifier = Modifier.fillMaxWidth().height(ChartHeight)) {
            Text(
                text = stringResource(R.string.profit_loss),
                modifier = Modifier.align(Alignment.CenterVertically).sideways(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Canvas(modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp)) {
                val stroke = 1.dp.toPx()
                // Room over the curve for the highest's label
                val top = measurer.measure("0", labelStyle).size.height + 4.dp.toPx()
                val range = (high - low).coerceAtLeast(1)
                fun x(hand: Int) = if (profits.size < 2) 0f else size.width * hand / profits.lastIndex
                fun y(profit: Long) = top + (size.height - top) * (high - profit) / range

                if (profits.size > 1) {
                    val curve = Path().apply {
                        moveTo(x(0), y(profits[0]))
                        for (hand in 1..profits.lastIndex) {
                            // Level at each hand, as Blackjack Ace's curve is, so a push draws flat
                            val middle = (x(hand - 1) + x(hand)) / 2
                            cubicTo(middle, y(profits[hand - 1]), middle, y(profits[hand]), x(hand), y(profits[hand]))
                        }
                    }
                    val area = Path().apply {
                        addPath(curve)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(line.copy(alpha = 0.25f), line.copy(alpha = 0.02f))))
                    drawPath(curve, line, style = Stroke(width = 2.dp.toPx()))
                }
                drawLine(zero, Offset(0f, y(0)), Offset(size.width, y(0)), stroke)
                drawLine(axis, Offset(0f, 0f), Offset(0f, size.height), stroke)
                drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                labels.forEach { (profit, text) ->
                    val label = measurer.measure(text, labelStyle)
                    drawText(label, topLeft = Offset(4.dp.toPx(), y(profit) - label.size.height - 2.dp.toPx()))
                }
            }
        }
        Text(
            text = stringResource(R.string.hands_played),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// Reads upwards, as Blackjack Ace's y axis title does, and takes only as much width as it is tall
private fun Modifier.sideways() = layout { measurable, constraints ->
    val placeable = measurable.measure(Constraints(maxWidth = constraints.maxHeight))
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(x = (placeable.height - placeable.width) / 2, y = (placeable.width - placeable.height) / 2) { rotationZ = -90f }
    }
}

@Composable
private fun StrategyCard(stats: PlayStats) {
    StatCard(title = stringResource(R.string.strategy)) {
        Figure(value = stats.hands.toString(), label = stringResource(R.string.hands_played), color = MaterialTheme.colorScheme.onSurface)
        GRADES.chunked(2).forEach { row ->
            Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (grade, label) ->
                    val count = stats.grades.getValue(grade)
                    Count(value = count.toString(), label = stringResource(label), modifier = Modifier.weight(1f), share = percentText(count, stats.hands))
                }
            }
        }
    }
}

// Whole percentages, as Blackjack Ace's Statistics give them, or "--" with nothing played
@Composable
private fun percentText(count: Int, total: Int): String =
    if (total == 0) stringResource(R.string.no_data) else stringResource(R.string.whole_percentage, (count * 100.0 / total).roundToInt())
