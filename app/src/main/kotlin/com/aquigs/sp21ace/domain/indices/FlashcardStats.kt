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
 * The [fraction] (0 to 1) percentile of [sorted] times, interpolated linearly between the two either side of its rank, as
 * spreadsheets' PERCENTILE and most statistics tools do by default. Every figure on the page uses it, so a median and a box read
 * alike. It behaves with few answers: one is every percentile, two give their mean as the median and a box between them, and no
 * percentile ever falls outside the times given. Null with none.
 */
fun percentile(sorted: List<Duration>, fraction: Double): Duration? {
    require(fraction in 0.0..1.0) { "No percentile at $fraction" }
    if (sorted.isEmpty()) return null

    val rank = (sorted.size - 1) * fraction
    val below = floor(rank).toInt()
    val above = ceil(rank).toInt()
    return sorted[below] + (sorted[above] - sorted[below]) * (rank - below)
}

/** The 10th, 25th, 50th, 75th and 90th percentiles of some answer times, as [percentile] works them out: a box plot's figures. */
data class TimeSpread(val p10: Duration, val p25: Duration, val median: Duration, val p75: Duration, val p90: Duration) {
    companion object {
        /** The spread of [times], in any order, or null with none. */
        fun of(times: List<Duration>): TimeSpread? {
            if (times.isEmpty()) return null

            val sorted = times.sorted()
            fun at(fraction: Double) = requireNotNull(percentile(sorted, fraction))
            return TimeSpread(at(0.1), at(0.25), at(0.5), at(0.75), at(0.9))
        }
    }
}

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

        /** The bucket [time] falls in, each holding its lower edge but not its upper. */
        fun bucketOf(time: Duration): Int = minOf((time / BUCKET_WIDTH).toInt(), BUCKETS)

        /** Where [bucket] starts. */
        fun start(bucket: Int): Duration = BUCKET_WIDTH * bucket

        fun of(times: List<Duration>): TimeHistogram {
            val counts = IntArray(BUCKETS + 1)
            for (time in times) counts[bucketOf(time)]++
            return TimeHistogram(counts.toList())
        }
    }
}

/** How long some answers took: the [spread] and [histogram] of their times, or a null spread when none was timed. */
data class AnswerTimes(val spread: TimeSpread?, val histogram: TimeHistogram) {
    val timed: Int get() = histogram.total

    companion object {
        fun of(times: List<Duration>): AnswerTimes = AnswerTimes(TimeSpread.of(times), TimeHistogram.of(times))
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
    val axisEnd: Duration
        get() {
            val longest = indices.maxOfOrNull { it.times.spread?.p90 ?: Duration.ZERO } ?: Duration.ZERO
            return ceil(longest / 1.seconds).seconds.coerceIn(AXIS_SHORTEST, AXIS_LONGEST)
        }
}

/**
 * [preset]'s figures over [period] as of [now]. An answer counts when it was to one of the preset's indices, number and direction
 * included, so an index whose number was edited starts afresh, as Prioritize worse counts it. Every answer counts for accuracy,
 * and only a timed one for speed: an untimed one had its card interrupted, so its time says nothing. Every index switched on has
 * a row, answered or not, to show what is left to drill; a switched-off one has a row only while it has answers in the period.
 */
fun List<FlashcardAnswer>.flashcardStats(preset: IndexPreset, period: Period, now: Instant): FlashcardStats {
    val counts = period.counter(now)
    val indices = preset.entries.map { it.index }.toSet()
    val byIndex = asSequence().filter { it.game == preset.game && it.index in indices && counts(it.answeredAt) }.groupBy { it.index }
    val counted = byIndex.values.flatten()

    val rows = preset.entries.mapNotNull { entry ->
        val answers = byIndex[entry.index].orEmpty()
        if (!entry.enabled && answers.isEmpty()) return@mapNotNull null
        IndexStats(entry.index, entry.enabled, answers.tally(), AnswerTimes.of(answers.mapNotNull { it.time }))
    }
    return FlashcardStats(counted.tally(), AnswerTimes.of(counted.mapNotNull { it.time }), rows)
}

private fun List<FlashcardAnswer>.tally(): Tally = count { it.isCorrect }.let { Tally(correct = it, incorrect = size - it) }

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
        IndexOrder.LEAST_ACCURATE -> unansweredLast.then { a, b -> if (a.answers == 0) 0 else byAccuracy.compare(a, b) }
            .thenByDescending { it.answers }
        IndexOrder.SLOWEST -> unansweredLast.thenBy { it.times.spread == null }
            .thenByDescending { it.times.spread?.median }
            .thenByDescending { it.times.spread?.p90 }
    },
)
