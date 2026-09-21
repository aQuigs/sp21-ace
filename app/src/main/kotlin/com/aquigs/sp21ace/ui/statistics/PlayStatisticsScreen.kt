package com.aquigs.sp21ace.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.PlayStats
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.history.playStats
import com.aquigs.sp21ace.domain.history.start
import com.aquigs.sp21ace.ui.components.Count
import com.aquigs.sp21ace.ui.components.CountRow
import com.aquigs.sp21ace.ui.components.Figure
import com.aquigs.sp21ace.ui.components.StatCard
import com.aquigs.sp21ace.ui.components.StatCards
import com.aquigs.sp21ace.ui.components.StatPage
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.chipsText
import com.aquigs.sp21ace.ui.components.profitText
import com.aquigs.sp21ace.ui.components.readingUp
import com.aquigs.sp21ace.ui.components.rememberNow
import com.aquigs.sp21ace.ui.components.shareText
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * hand, and how closely the hands followed the chart.
 */
@Composable
fun PlayStatisticsScreen(history: List<PlayedHand>, onBack: () -> Unit, modifier: Modifier = Modifier, now: () -> Instant = Instant::now) {
    var period by rememberSaveable { mutableStateOf(Period.TODAY) }
    val asOf = rememberNow(now)
    // Keyed on where the period starts rather than the time, so All Time, which has no start, isn't counted again every minute
    val stats = remember(history, period, period.start(asOf)) { history.playStats(period, asOf) }

    SubPage(title = stringResource(R.string.statistics), onBack = onBack, modifier = modifier) { padding ->
        StatPage(period = period, onPeriod = { period = it }, modifier = Modifier.fillMaxSize().padding(padding)) {
            StatCards {
                HandsCard(stats)
                BankrollCard(stats)
                StrategyCard(stats)
            }
        }
    }
}

@Composable
private fun HandsCard(stats: PlayStats) {
    val won = shareText(stats.won, stats.hands)
    val pushed = shareText(stats.pushed, stats.hands)
    val lost = shareText(stats.lost, stats.hands)
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

/**
 * Won at the start, lost at the end, and pushed as near [pushCentre] of the way across as fits between them, or on a line of its
 * own where it doesn't, as at a large font size.
 */
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
        val between = start.width + gap to width - end.width - gap - middle.width
        val fits = between.first <= between.second
        val (lowest, highest) = if (fits) between else 0 to maxOf(0, width - middle.width)
        val x = (pushCentre * width - middle.width / 2f).roundToInt().coerceIn(lowest, highest)
        val ends = maxOf(start.height, end.height)

        layout(width, if (fits) maxOf(ends, middle.height) else ends + middle.height) {
            start.place(0, 0)
            middle.place(x, if (fits) 0 else ends)
            end.place(width - end.width, 0)
        }
    }
}

/** Blackjack Ace's bar of the hands won, pushed and lost, in proportion: won from the start, lost from the end, pushed between. */
@Composable
private fun HandsBar(stats: PlayStats, modifier: Modifier = Modifier) {
    val colors = Sp21AceTheme.colors
    val pushed = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier.clip(CircleShape)) {
        drawRect(pushed)
        if (stats.hands == 0) return@Canvas

        val lost = size.width * stats.lost / stats.hands
        drawRect(colors.correctTint, size = Size(size.width * stats.won / stats.hands, size.height))
        drawRect(colors.wrongTint, topLeft = Offset(size.width - lost, 0f), size = Size(lost, size.height))
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
        CountRow {
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
    val high = profits.max()
    val low = profits.min()
    val description =
        pluralStringResource(R.plurals.profit_chart_description, profits.lastIndex, profits.lastIndex, profitText(profits.last()), profitText(low), profitText(high))
    val line = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    val zero = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer = rememberTextMeasurer()

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(modifier = Modifier.fillMaxWidth().height(ChartHeight)) {
            Text(
                text = stringResource(R.string.profit_loss),
                modifier = Modifier.align(Alignment.CenterVertically).readingUp(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(
                modifier = Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp).drawWithCache {
                    val stroke = 1.dp.toPx()
                    // Room over the curve for the highest's label
                    val top = measurer.measure("0", labelStyle).size.height + 4.dp.toPx()
                    // A history that never left 0 runs level across the middle rather than along the top, like a peak
                    fun y(profit: Long) = if (high == low) (top + size.height) / 2 else top + (size.height - top) * (high - profit) / (high - low)

                    val curve = if (profits.size > 1) Path().apply { traceProfits(profits, size.width, ::y) } else null
                    val area = curve?.let {
                        Path().apply {
                            addPath(it)
                            lineTo(size.width, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                    }
                    val fill = Brush.verticalGradient(listOf(line.copy(alpha = 0.25f), line.copy(alpha = 0.02f)))
                    val labels = listOfNotNull(0L, high.takeIf { it > 0 }, low.takeIf { it < 0 })
                        .map { measurer.measure(profitText(it), labelStyle) to y(it) }
                        // The highest or lowest too near 0 to print clear of its label goes without
                        .filterIndexed { index, (label, at) -> index == 0 || abs(at - y(0)) >= label.size.height }

                    onDrawBehind {
                        if (curve != null && area != null) {
                            drawPath(area, fill)
                            drawPath(curve, line, style = Stroke(width = 2.dp.toPx()))
                        }
                        drawLine(zero, Offset(0f, y(0)), Offset(size.width, y(0)), stroke)
                        drawLine(axis, Offset(0f, 0f), Offset(0f, size.height), stroke)
                        drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                        labels.forEach { (label, at) -> drawText(label, topLeft = Offset(4.dp.toPx(), at - label.size.height - 2.dp.toPx())) }
                    }
                },
            )
        }
        Text(
            text = stringResource(R.string.hands_played),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Traces [profits] across [width]: a cubic from each hand to the next, level at each as Blackjack Ace's curve is, so a push draws
 * flat. With more hands than pixels across, each pixel instead draws from the highest to the lowest of its hands, which looks
 * the same and keeps a long history's path to a few thousand points.
 */
private fun Path.traceProfits(profits: List<Long>, width: Float, y: (Long) -> Float) {
    val step = width / profits.lastIndex
    moveTo(0f, y(profits.first()))

    if (step >= 1) {
        for (hand in 1..profits.lastIndex) {
            val middle = step * (hand - 0.5f)
            cubicTo(middle, y(profits[hand - 1]), middle, y(profits[hand]), step * hand, y(profits[hand]))
        }
        return
    }

    var column = 0
    var highest = profits.first()
    var lowest = highest
    for (hand in 1..profits.lastIndex) {
        val profit = profits[hand]
        val x = (step * hand).toInt()
        if (x != column) {
            lineTo(column.toFloat(), y(highest))
            lineTo(column.toFloat(), y(lowest))
            column = x
            highest = profit
            lowest = profit
        } else {
            highest = maxOf(highest, profit)
            lowest = minOf(lowest, profit)
        }
    }
    lineTo(column.toFloat(), y(highest))
    lineTo(column.toFloat(), y(lowest))
    lineTo(width, y(profits.last()))
}

@Composable
private fun StrategyCard(stats: PlayStats) {
    StatCard(title = stringResource(R.string.strategy)) {
        Figure(value = stats.hands.toString(), label = stringResource(R.string.hands_played), color = MaterialTheme.colorScheme.onSurface)
        GRADES.chunked(2).forEach { row ->
            CountRow {
                row.forEach { (grade, label) ->
                    val count = stats.grades[grade] ?: 0
                    Count(value = count.toString(), label = stringResource(label), modifier = Modifier.weight(1f), share = shareText(count, stats.hands))
                }
            }
        }
    }
}
