package com.aquigs.sp21ace.domain.drill

import com.aquigs.sp21ace.domain.history.Tally
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The [fraction] percentile of [sorted] times in milliseconds, interpolated linearly between the ranks either side, as spreadsheets'
 * PERCENTILE does: it behaves with one or two answers and never leaves the range of the times. Every figure on the page uses it.
 */
private fun percentile(sorted: LongArray, fraction: Double): Duration {
    val rank = (sorted.size - 1) * fraction
    val below = floor(rank).toInt()
    val above = ceil(rank).toInt()
    return (sorted[below] + (sorted[above] - sorted[below]) * (rank - below)).milliseconds
}

/** The 10th, 25th, 50th, 75th and 90th percentiles of some answer times, as [percentile] works them out: a box plot's figures. */
data class TimeSpread(val p10: Duration, val p25: Duration, val median: Duration, val p75: Duration, val p90: Duration) {
    /** Where the parts of this spread's box plot lie on an axis from 0 to [end]. */
    fun onAxis(end: Duration): SpreadOnAxis {
        fun at(time: Duration) = (time / end).toFloat().coerceAtMost(1f)

        return SpreadOnAxis(
            whisker = if (p10 > end) null else at(p10)..at(p90),
            whiskerCut = p90 > end,
            box = if (p25 > end) null else at(p25)..at(p75),
            boxCut = p75 > end,
            median = median.takeIf { it <= end }?.let(::at),
        )
    }

    companion object {
        /** The spread of [sorted] times in milliseconds, or null with none. */
        internal fun of(sorted: LongArray): TimeSpread? {
            if (sorted.isEmpty()) return null

            fun at(fraction: Double) = percentile(sorted, fraction)
            return TimeSpread(at(0.1), at(0.25), at(0.5), at(0.75), at(0.9))
        }
    }
}

/**
 * The parts of a box plot that lie on an axis, each as a fraction of its length, so nothing past the end is drawn at the end as if it
 * were there: the [whisker], the [box] and the [median]'s tick, each null when wholly past the end, and the whisker and box cut at
 * the end when they run past it.
 */
data class SpreadOnAxis(
    val whisker: ClosedFloatingPointRange<Float>?,
    val whiskerCut: Boolean,
    val box: ClosedFloatingPointRange<Float>?,
    val boxCut: Boolean,
    val median: Float?,
)

/**
 * Answer times counted into fixed buckets, the same everywhere so their shapes compare: [BUCKETS] of half a second from 0,
 * then one for 5 s or more. A number learned by heart, or a count divided fluently, takes a second or two, so half seconds tell a
 * fluent answer from a hesitant one. Past 5 s it wasn't fluent or the eyes were elsewhere, and how far past no longer matters.
 */
data class TimeHistogram(val counts: List<Int>) {
    init {
        require(counts.size == BUCKETS + 1) { "${counts.size} buckets" }
    }

    val total: Int get() = counts.sum()

    companion object {
        val BUCKET_WIDTH: Duration = 500.milliseconds

        /** The buckets before the one for [OVERFLOW_FROM] or more, which is the last. */
        const val BUCKETS = 10

        val OVERFLOW_FROM: Duration = BUCKET_WIDTH * BUCKETS

        /** Where [bucket] starts. */
        fun start(bucket: Int): Duration = BUCKET_WIDTH * bucket

        /** Where [bucket] ends, or null for the last, which holds every time past its start. */
        fun end(bucket: Int): Duration? = if (bucket == BUCKETS) null else start(bucket + 1)

        /** Where [time] lies along the buckets, in buckets: 2 for a second, or the middle of the last for the last's own label. */
        fun position(time: Duration): Float = (time / BUCKET_WIDTH).toFloat()

        /** Times of [millis] counted, each bucket holding its lower edge but not its upper. */
        internal fun of(millis: LongArray): TimeHistogram {
            val counts = IntArray(BUCKETS + 1)
            for (time in millis) counts[minOf(time / BUCKET_WIDTH.inWholeMilliseconds, BUCKETS.toLong()).toInt()]++
            return TimeHistogram(counts.toList())
        }
    }
}

/** How long some answers took: the [spread] and [histogram] of their times, or a null spread when none was timed. */
data class AnswerTimes(val spread: TimeSpread?, val histogram: TimeHistogram) {
    val timed: Int get() = histogram.total

    companion object {
        /** The figures of [sorted] times in milliseconds. */
        internal fun of(sorted: LongArray): AnswerTimes = AnswerTimes(TimeSpread.of(sorted), TimeHistogram.of(sorted))
    }
}

/** Answers as a pass counts them: right and wrong, and the times of the timed ones in milliseconds. */
internal class AnswerCounter {
    private var correct = 0
    private var incorrect = 0
    private var millis = LongArray(16)
    private var timed = 0

    val tally: Tally get() = Tally(correct, incorrect)

    val sortedTimes: LongArray by lazy { millis.copyOf(timed).apply { sort() } }

    val times: AnswerTimes get() = AnswerTimes.of(sortedTimes)

    fun add(isCorrect: Boolean, time: Duration?) {
        if (isCorrect) correct++ else incorrect++
        if (time == null) return
        if (timed == millis.size) millis = millis.copyOf(timed * 2)
        millis[timed++] = time.inWholeMilliseconds
    }
}
