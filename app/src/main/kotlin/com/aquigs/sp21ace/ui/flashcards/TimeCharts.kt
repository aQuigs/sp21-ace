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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
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

private val LabelGap = 4.dp

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
    val labelHeight = rememberLabelHeight(measurer, labelStyle)
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
                    val tallest = histogram.counts.indices.maxBy { histogram.counts[it] }
                    val highest = histogram.counts[tallest].coerceAtLeast(1)
                    val counts = histogram.counts.map { measurer.measure(it.toString(), labelStyle) }
                    val bottom = size.height - labelHeight.toPx() - gap
                    val top = labelHeight.toPx() + gap
                    // A count of 1 among hundreds still shows as a bar
                    fun barHeight(count: Int) = if (count == 0) 0f else maxOf(2.dp.toPx(), (bottom - top) * count / highest)

                    val everyCountFits = counts.all { it.size.width <= slot }
                    val countLabels = counts.withIndex().filter { (bucket, _) ->
                        histogram.counts[bucket] > 0 && (everyCountFits || bucket == tallest)
                    }
                    val overflow = measurer.measure(overflowLabel, labelStyle) to (TimeHistogram.BUCKETS + 0.5f) * slot
                    val seconds = TimeHistogram.OVERFLOW_FROM.inWholeSeconds.toInt() - 1
                    val axisLabels = secondLabels(measurer, labelStyle, seconds, slot * TimeHistogram.position(1.seconds), overflow)

                    onDrawBehind {
                        histogram.counts.forEachIndexed { bucket, count ->
                            val height = barHeight(count)
                            drawRect(bar, topLeft = Offset(bucket * slot + gap / 2, bottom - height), size = Size(maxOf(0f, slot - gap), height))
                        }
                        countLabels.forEach { (bucket, label) ->
                            val left = centeredWithin((bucket + 0.5f) * slot, label, size.width)
                            drawText(label, topLeft = Offset(left, bottom - barHeight(histogram.counts[bucket]) - label.size.height - gap))
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
    val buckets = histogram.counts.withIndex().filter { it.value > 0 }.map { (bucket, count) ->
        val start = TimeHistogram.start(bucket).inSeconds
        val end = TimeHistogram.end(bucket)?.inSeconds
        when {
            end == null -> pluralStringResource(R.plurals.bucket_overflow, count, count, start)
            start == 0.0 -> pluralStringResource(R.plurals.bucket_under, count, count, end)
            else -> pluralStringResource(R.plurals.bucket_between, count, count, start, end)
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
    val labelHeight = rememberLabelHeight(measurer, labelStyle)
    val endSeconds = end.inWholeSeconds.toInt()

    Column(modifier = modifier.clearAndSetSemantics {}) {
        Text(text = stringResource(R.string.seconds_axis), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(
            modifier = Modifier.fillMaxWidth().height(labelHeight).drawWithCache {
                val labels = secondLabels(measurer, labelStyle, endSeconds, size.width / endSeconds)
                onDrawBehind { labels.forEach { (label, left) -> drawText(label, topLeft = Offset(left, 0f)) } }
            },
        )
    }
}

/**
 * A box plot of [spread] on an axis from 0 to [axisEnd], with a faint line each second: a whisker from the 10th to the 90th
 * percentile, a box over the middle half and a tick at the median. Only what lies on the axis is drawn, so a time past its end never
 * reads as one at the end: a whisker past it runs off as an arrow, a box is cut open there, and a median past it has no tick.
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
    val onAxis = remember(spread, axisEnd) { spread.onAxis(axisEnd) }
    val grid = MaterialTheme.colorScheme.outlineVariant
    val whisker = MaterialTheme.colorScheme.onSurfaceVariant
    val box = MaterialTheme.colorScheme.primary
    val median = MaterialTheme.colorScheme.onSurface

    Spacer(
        modifier = modifier.fillMaxWidth().height(StripHeight).clearAndSetSemantics { contentDescription = description }.drawWithCache {
            fun x(fraction: Float) = fraction * size.width
            val middle = size.height / 2
            val boxHeight = BoxHeight.toPx()
            val boxTop = middle - boxHeight / 2
            val cap = 4.dp.toPx()
            val line = 2.dp.toPx()
            val gridLine = 1.dp.toPx()
            val tick = 3.dp.toPx()
            val whiskerStroke = Stroke(width = line)
            val boxStroke = Stroke(width = 1.5.dp.toPx())
            val arrow = Path().apply {
                moveTo(size.width - cap * 1.5f, middle - cap)
                lineTo(size.width, middle)
                lineTo(size.width - cap * 1.5f, middle + cap)
            }
            // A box cut at the end has no right side, so it reads as running on past it
            val boxOutline = onAxis.box?.let { range ->
                Path().apply {
                    moveTo(x(range.endInclusive), boxTop)
                    lineTo(x(range.start), boxTop)
                    lineTo(x(range.start), boxTop + boxHeight)
                    lineTo(x(range.endInclusive), boxTop + boxHeight)
                    if (!onAxis.boxCut) close()
                }
            }
            fun DrawScope.cap(at: Float) = drawLine(whisker, Offset(x(at), middle - cap), Offset(x(at), middle + cap), line)

            onDrawBehind {
                clipRect {
                    for (second in 0..axisEnd.inWholeSeconds) {
                        val at = x(second / axisEnd.inSeconds.toFloat())
                        drawLine(grid, Offset(at, 0f), Offset(at, size.height), gridLine)
                    }

                    onAxis.whisker?.let {
                        drawLine(whisker, Offset(x(it.start), middle), Offset(x(it.endInclusive), middle), line)
                        cap(it.start)
                        if (!onAxis.whiskerCut) cap(it.endInclusive)
                    }
                    if (onAxis.whiskerCut) drawPath(arrow, whisker, style = whiskerStroke)

                    onAxis.box?.let { range ->
                        drawRect(box.copy(alpha = 0.3f), topLeft = Offset(x(range.start), boxTop), size = Size(x(range.endInclusive - range.start), boxHeight))
                    }
                    boxOutline?.let { drawPath(it, box, style = boxStroke) }
                    onAxis.median?.let { drawLine(median, Offset(x(it), boxTop - tick), Offset(x(it), boxTop + boxHeight + tick), tick) }
                }
            }
        },
    )
}

// Measured once per style, since a chart's own measurer caches too few results to keep it through the labels it draws
@Composable
private fun rememberLabelHeight(measurer: TextMeasurer, style: TextStyle): Dp {
    val density = LocalDensity.current
    return remember(measurer, style, density) { with(density) { measurer.measure("0", style).size.height.toDp() } }
}

/**
 * The labels 0 to [seconds] of a time axis [perSecond] pixels a second long, each with where to draw it, as often as they fit, and
 * then [last] if given, which always stays.
 */
private fun CacheDrawScope.secondLabels(
    measurer: TextMeasurer,
    style: TextStyle,
    seconds: Int,
    perSecond: Float,
    last: Pair<TextLayoutResult, Float>? = null,
): List<Pair<TextLayoutResult, Float>> {
    val labels = (0..seconds).map { measurer.measure(it.toString(), style) }
    val widest = labels.maxOf { it.size.width } + LabelGap.toPx()
    val step = LABEL_STEPS.firstOrNull { it * perSecond >= widest } ?: LABEL_STEPS.last()
    // The axis' own end stays labeled where nothing follows it
    val shown = labels.withIndex().filter { (second, _) -> second % step == 0 || (last == null && second == seconds) }
        .map { (second, label) -> label to second * perSecond }
    return clearOfEachOther(shown + listOfNotNull(last), size.width, LabelGap.toPx())
}

/**
 * Where to draw each of [labels], given as a label and the x it marks, so none overlaps another: centered on its x but kept within
 * [width], so the first and last aren't cut in half, and dropped where it would come within [gap] of the one before or of the
 * last. The last always stays, so the axis' end is labeled.
 */
private fun clearOfEachOther(labels: List<Pair<TextLayoutResult, Float>>, width: Float, gap: Float): List<Pair<TextLayoutResult, Float>> {
    val placed = labels.map { (label, x) -> label to centeredWithin(x, label, width) }
    val last = placed.lastOrNull() ?: return emptyList()
    var right = Float.NEGATIVE_INFINITY

    return placed.dropLast(1).filter { (label, left) ->
        (left >= right + gap && left + label.size.width + gap <= last.second).also { if (it) right = left + label.size.width }
    } + last
}

// Kept inside the chart, or at its start when the chart is narrower than the label, as in a window too small to show it
private fun centeredWithin(x: Float, label: TextLayoutResult, width: Float): Float =
    (x - label.size.width / 2f).coerceIn(0f, maxOf(0f, width - label.size.width))
