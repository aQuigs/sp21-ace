package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.TimeHistogram
import com.aquigs.sp21ace.domain.indices.TimeSpread
import com.aquigs.sp21ace.ui.components.readingUp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

private val HistogramHeight = 120.dp

private val StripHeight = 24.dp

private val BoxHeight = 14.dp

// Whole seconds between the time axis' labels, the fewest that keep them apart at the font size in force
private val LABEL_STEPS = listOf(1, 2, 5, 10)

internal val Duration.inSeconds: Double get() = toDouble(DurationUnit.SECONDS)

/** "2.1 s". */
@Composable
internal fun secondsText(time: Duration): String = stringResource(R.string.seconds_value, time.inSeconds)

/**
 * A bar for each of [histogram]'s buckets, its count over it where every count fits over its bar and otherwise over the tallest
 * alone, on an axis of seconds whose last bar holds every time past it. A screen reader hears each bucket's count instead.
 */
@Composable
fun TimeHistogramChart(histogram: TimeHistogram, modifier: Modifier = Modifier) {
    val description = histogramDescription(histogram)
    val bar = MaterialTheme.colorScheme.primary
    val axis = MaterialTheme.colorScheme.outline
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    val measurer = rememberTextMeasurer()
    val labelHeight = with(LocalDensity.current) { measurer.measure("0", labelStyle).size.height.toDp() }
    val overflowLabel = "${TimeHistogram.OVERFLOW_FROM.inWholeSeconds}+"

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.answers_axis),
                modifier = Modifier.align(Alignment.CenterVertically).readingUp(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(
                modifier = Modifier.weight(1f).height(HistogramHeight + labelHeight * 2).padding(start = 8.dp).drawWithCache {
                    val stroke = 1.dp.toPx()
                    val gap = 2.dp.toPx()
                    val slot = size.width / histogram.counts.size
                    val highest = histogram.counts.max().coerceAtLeast(1)
                    val counts = histogram.counts.map { measurer.measure(it.toString(), labelStyle) }
                    val bottom = size.height - labelHeight.toPx() - gap
                    val top = labelHeight.toPx() + gap
                    // A count of 1 among hundreds still shows as a bar
                    fun barHeight(count: Int) = if (count == 0) 0f else maxOf(2.dp.toPx(), (bottom - top) * count / highest)

                    val everyCountFits = counts.all { it.size.width <= slot }
                    val tallest = histogram.counts.indexOf(histogram.counts.max())
                    val countLabels = counts.withIndex().filter { (bucket, _) ->
                        histogram.counts[bucket] > 0 && (everyCountFits || bucket == tallest)
                    }

                    val bucketsPerSecond = (1.seconds / TimeHistogram.BUCKET_WIDTH).toInt()
                    val seconds = (0 until TimeHistogram.OVERFLOW_FROM.inWholeSeconds.toInt()).map { measurer.measure(it.toString(), labelStyle) }
                    val step = labelStep(slot * bucketsPerSecond, seconds.maxOf { it.size.width } + 4.dp.toPx())
                    val secondLabels = seconds.withIndex().filter { (second, _) -> second % step == 0 }
                        .map { (second, label) -> label to second * bucketsPerSecond * slot }
                    val overflow = measurer.measure(overflowLabel, labelStyle) to (TimeHistogram.BUCKETS + 0.5f) * slot
                    val axisLabels = clearOfEachOther(secondLabels + overflow, size.width, 4.dp.toPx())

                    onDrawBehind {
                        histogram.counts.forEachIndexed { bucket, count ->
                            val height = barHeight(count)
                            drawRect(bar, topLeft = Offset(bucket * slot + gap / 2, bottom - height), size = Size(slot - gap, height))
                        }
                        countLabels.forEach { (bucket, label) ->
                            val x = (bucket + 0.5f) * slot - label.size.width / 2f
                            drawText(label, topLeft = Offset(x.coerceIn(0f, size.width - label.size.width), bottom - barHeight(histogram.counts[bucket]) - label.size.height - gap))
                        }
                        drawLine(axis, Offset(0f, bottom), Offset(size.width, bottom), stroke)
                        drawLine(axis, Offset(0f, top), Offset(0f, bottom), stroke)
                        axisLabels.forEach { (label, left) -> drawText(label, topLeft = Offset(left, bottom + gap)) }
                    }
                },
            )
        }
        Text(
            text = stringResource(R.string.seconds_axis),
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun histogramDescription(histogram: TimeHistogram): String {
    val width = TimeHistogram.BUCKET_WIDTH.inSeconds
    val buckets = histogram.counts.withIndex().filter { it.value > 0 }.map { (bucket, count) ->
        val start = TimeHistogram.start(bucket).inSeconds
        when (bucket) {
            0 -> pluralStringResource(R.plurals.bucket_under, count, count, width)
            TimeHistogram.BUCKETS -> pluralStringResource(R.plurals.bucket_overflow, count, count, start)
            else -> pluralStringResource(R.plurals.bucket_between, count, count, start, start + width)
        }
    }
    return pluralStringResource(R.plurals.histogram_description, histogram.total, histogram.total, buckets.joinToString(", "))
}

/**
 * The seconds of a time axis running from 0 to [end], labeled as often as they fit, over the [TimeSpreadStrip]s drawn to it. For
 * the eye only, since each strip tells a screen reader its times.
 */
@Composable
fun TimeAxis(end: Duration, modifier: Modifier = Modifier) {
    val labelStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val labelHeight = with(LocalDensity.current) { measurer.measure("0", labelStyle).size.height.toDp() }
    val endSeconds = end.inWholeSeconds.toInt()

    Column(modifier = modifier.clearAndSetSemantics {}) {
        Text(text = stringResource(R.string.seconds_axis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(
            modifier = Modifier.fillMaxWidth().height(labelHeight).drawWithCache {
                val labels = (0..endSeconds).map { measurer.measure(it.toString(), labelStyle) }
                val step = labelStep(size.width / endSeconds, labels.maxOf { it.size.width } + 4.dp.toPx())
                val shown = labels.withIndex().filter { (second, _) -> second % step == 0 || second == endSeconds }
                    .map { (second, label) -> label to size.width * second / endSeconds }
                val placed = clearOfEachOther(shown, size.width, 4.dp.toPx())

                onDrawBehind { placed.forEach { (label, left) -> drawText(label, topLeft = Offset(left, 0f)) } }
            },
        )
    }
}

/**
 * A box plot of [spread] on an axis from 0 to [axisEnd], with a faint line each second: a whisker from the 10th to the 90th
 * percentile, a box over the middle half and a tick at the median. A whisker past the axis' end runs off it as an arrow.
 */
@Composable
fun TimeSpreadStrip(spread: TimeSpread, axisEnd: Duration, modifier: Modifier = Modifier) {
    val description = stringResource(
        R.string.box_plot_description,
        spread.median.inSeconds,
        spread.p25.inSeconds,
        spread.p75.inSeconds,
        spread.p10.inSeconds,
        spread.p90.inSeconds,
    )
    val grid = MaterialTheme.colorScheme.outlineVariant
    val whisker = MaterialTheme.colorScheme.onSurfaceVariant
    val box = MaterialTheme.colorScheme.primary
    val median = MaterialTheme.colorScheme.onSurface

    Spacer(
        modifier = modifier.fillMaxWidth().height(StripHeight).clearAndSetSemantics { contentDescription = description }.drawWithCache {
            fun x(time: Duration) = (time / axisEnd).toFloat().coerceIn(0f, 1f) * size.width
            val middle = size.height / 2
            val boxTop = middle - BoxHeight.toPx() / 2
            val left = x(spread.p25)
            val boxWidth = maxOf(x(spread.p75) - left, 2.dp.toPx())
            val cap = 4.dp.toPx()
            val runsOff = spread.p90 > axisEnd
            val arrow = Path().apply {
                moveTo(size.width - cap * 1.5f, middle - cap)
                lineTo(size.width, middle)
                lineTo(size.width - cap * 1.5f, middle + cap)
            }

            onDrawBehind {
                for (second in 0..axisEnd.inWholeSeconds) {
                    val at = x(second.seconds)
                    drawLine(grid, Offset(at, 0f), Offset(at, size.height), 1.dp.toPx())
                }

                drawLine(whisker, Offset(x(spread.p10), middle), Offset(x(spread.p90), middle), 2.dp.toPx())
                drawLine(whisker, Offset(x(spread.p10), middle - cap), Offset(x(spread.p10), middle + cap), 2.dp.toPx())
                if (runsOff) {
                    drawPath(arrow, whisker, style = Stroke(width = 2.dp.toPx()))
                } else {
                    drawLine(whisker, Offset(x(spread.p90), middle - cap), Offset(x(spread.p90), middle + cap), 2.dp.toPx())
                }

                drawRect(box.copy(alpha = 0.3f), topLeft = Offset(left, boxTop), size = Size(boxWidth, BoxHeight.toPx()))
                drawRect(box, topLeft = Offset(left, boxTop), size = Size(boxWidth, BoxHeight.toPx()), style = Stroke(width = 1.5.dp.toPx()))
                drawLine(median, Offset(x(spread.median), boxTop - 3.dp.toPx()), Offset(x(spread.median), boxTop + BoxHeight.toPx() + 3.dp.toPx()), 3.dp.toPx())
            }
        },
    )
}

private fun labelStep(perSecond: Float, labelWidth: Float): Int = LABEL_STEPS.firstOrNull { it * perSecond >= labelWidth } ?: LABEL_STEPS.last()

/**
 * Where to draw each of [labels], given as a label and the x it marks, so none overlaps another: centered on its x but kept within
 * [width], so the first and last aren't cut in half, and dropped where it would come within [gap] of the one before or of the
 * last. The last always stays, so the axis' end is labeled.
 */
private fun clearOfEachOther(labels: List<Pair<TextLayoutResult, Float>>, width: Float, gap: Float): List<Pair<TextLayoutResult, Float>> {
    val placed = labels.map { (label, x) -> label to (x - label.size.width / 2f).coerceIn(0f, width - label.size.width) }
    val last = placed.lastOrNull() ?: return emptyList()
    var right = Float.NEGATIVE_INFINITY

    return placed.dropLast(1).filter { (label, left) ->
        (left >= right + gap && left + label.size.width + gap <= last.second).also { if (it) right = left + label.size.width }
    } + last
}
