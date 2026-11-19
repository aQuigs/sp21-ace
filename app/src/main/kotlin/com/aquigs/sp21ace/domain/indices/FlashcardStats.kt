package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.counter
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
 * Answer times counted into fixed buckets, the same for every index so their shapes compare: [BUCKETS] of half a second from 0,
 * then one for 5 s or more. Recalling a number learned by heart from a card takes a second or two, so half seconds tell a fluent
 * answer from a hesitant one. Past 5 s the number was worked out or the eyes were elsewhere, and how far past no longer matters.
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

/** One index's answers over a period: how often they were right, and how long the timed ones took. */
data class IndexStats(val index: PlayIndex, val enabled: Boolean, val tally: Tally, val times: AnswerTimes) {
    val answers: Int get() = tally.total
}

// So a fast player's tenths of a second aren't blown up across the whole width to look like a spread
private val AXIS_SHORTEST: Duration = 2.seconds

// So one index left on screen while the player looked away doesn't squash every other row's box into a few pixels
private val AXIS_LONGEST: Duration = 10.seconds

/**
 * A period's flashcard figures for a preset: the [overall] tally and [times] of every answer counted, then each index's own, in
 * the preset's order.
 */
data class FlashcardStats(val overall: Tally, val times: AnswerTimes, val indices: List<IndexStats>) {
    /**
     * Where the time axis every index's box plot shares ends: the longest 90th percentile of any index, up to a whole second, from
     * 2 s to 10 s. A whisker that runs past it is cut at the end.
     */
    val axisEnd: Duration = ceil((indices.maxOfOrNull { it.times.spread?.p90 ?: Duration.ZERO } ?: Duration.ZERO) / 1.seconds).seconds
        .coerceIn(AXIS_SHORTEST, AXIS_LONGEST)
}

/**
 * [preset]'s figures over [period] as of [now]. An answer counts when it was to one of the preset's indices, number and direction
 * included, so an index whose number was edited starts afresh, as Prioritize worse counts it. Every answer counts for accuracy,
 * and only a timed one for speed: an untimed one had its card interrupted, so its time says nothing. Each index switched on gets a
 * row, answered or not, and a switched-off one only while it has answers in the period. One pass, with times sorted once as plain
 * numbers, since it runs again on every change of period, preset or history.
 */
fun List<FlashcardAnswer>.flashcardStats(preset: IndexPreset, period: Period, now: Instant): FlashcardStats {
    val counts = period.counter(now)
    val byIndex = HashMap<PlayIndex, Answers>()
    for (entry in preset.entries) byIndex[entry.index] = Answers()

    for (answer in this) {
        if (answer.game != preset.game) continue
        val answers = byIndex[answer.index] ?: continue
        if (counts(answer.answeredAt)) answers.add(answer)
    }

    val rows = preset.entries.mapNotNull { entry ->
        val answers = byIndex.getValue(entry.index)
        if (!entry.enabled && answers.tally.total == 0) null else IndexStats(entry.index, entry.enabled, answers.tally, AnswerTimes.of(answers.sortedTimes))
    }
    val every = byIndex.values
    val overall = every.map { it.tally }.fold(Tally(correct = 0, incorrect = 0), Tally::plus)
    return FlashcardStats(overall, AnswerTimes.of(mergeSorted(every.map { it.sortedTimes })), rows)
}

/** [sorted] arrays merged into one sorted array, two at a time, so the times sorted for each index aren't sorted again. */
internal fun mergeSorted(sorted: List<LongArray>): LongArray {
    var arrays = sorted.ifEmpty { return LongArray(0) }
    while (arrays.size > 1) arrays = arrays.chunked(2) { it.singleOrNull() ?: merge(it[0], it[1]) }
    return arrays.single()
}

private fun merge(a: LongArray, b: LongArray): LongArray {
    val merged = LongArray(a.size + b.size)
    var i = 0
    var j = 0
    for (k in merged.indices) merged[k] = if (j == b.size || (i < a.size && a[i] <= b[j])) a[i++] else b[j++]
    return merged
}

/** One index's answers as the pass counts them: right and wrong, and the times of the timed ones in milliseconds. */
private class Answers {
    private var correct = 0
    private var incorrect = 0
    private var times = LongArray(16)
    private var timed = 0

    val tally: Tally get() = Tally(correct, incorrect)

    val sortedTimes: LongArray by lazy { times.copyOf(timed).apply { sort() } }

    fun add(answer: FlashcardAnswer) {
        if (answer.isCorrect) correct++ else incorrect++
        val time = answer.time ?: return
        if (timed == times.size) times = times.copyOf(timed * 2)
        times[timed++] = time.inWholeMilliseconds
    }
}

/** How the index rows are ordered, each putting the troublemakers at the top. */
enum class IndexOrder { LEAST_ACCURATE, SLOWEST }

private val byAccuracy = Comparator<IndexStats> { a, b -> (a.tally.correct.toLong() * b.answers).compareTo(b.tally.correct.toLong() * a.answers) }

private val unansweredLast = compareBy<IndexStats> { it.answers == 0 }

/**
 * The rows in [order]. Least accurate: lowest accuracy first, and at the same accuracy the more answers first, since they make
 * it the surer trouble. Slowest: longest median first, then longest 90th percentile; rows with no timed answer follow, then
 * those with no answer at all. Rows still tied keep the preset's order, and so do unanswered ones.
 */
fun List<IndexStats>.sortedBy(order: IndexOrder): List<IndexStats> = sortedWith(
    when (order) {
        IndexOrder.LEAST_ACCURATE -> unansweredLast.then(byAccuracy).thenByDescending { it.answers }
        // A null median, with no timed answer, sorts last when descending
        IndexOrder.SLOWEST -> unansweredLast.thenByDescending { it.times.spread?.median }
            .thenByDescending { it.times.spread?.p90 }
    },
)
