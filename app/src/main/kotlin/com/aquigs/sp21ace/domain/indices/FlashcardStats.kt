package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.drill.AnswerCounter
import com.aquigs.sp21ace.domain.drill.AnswerTimes
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.counter
import java.time.Instant
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The answers to one [index], or when null to every hand that has none, over a period: how often they were right, and how long the
 * timed ones took.
 */
data class IndexStats(val index: PlayIndex?, val enabled: Boolean, val tally: Tally, val times: AnswerTimes) {
    val answers: Int get() = tally.total
}

// So a fast player's tenths of a second aren't blown up across the whole width to look like a spread
private val AXIS_SHORTEST: Duration = 2.seconds

// So one index left on screen while the player looked away doesn't squash every other row's box into a few pixels
private val AXIS_LONGEST: Duration = 10.seconds

/**
 * A period's flashcard figures for a preset: the [overall] tally and [times] of every answer counted, then each index's own, in
 * the preset's order, and those of the hands with none.
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
 * row, answered or not, and a switched-off one only while it has answers in the period. The hands with none share one row while
 * they have answers in it. One pass, with times sorted once as plain numbers, since it runs again on every change of period, preset
 * or history.
 */
fun List<FlashcardAnswer>.flashcardStats(preset: IndexPreset, period: Period, now: Instant): FlashcardStats {
    val counts = period.counter(now)
    val byIndex = HashMap<IndexFact, AnswerCounter>()
    for (entry in preset.entries) byIndex[entry.index] = AnswerCounter()
    val noIndex = AnswerCounter()

    for (answer in this) {
        if (answer.game != preset.game || !counts(answer.answeredAt)) continue
        val answers = byIndex[answer.fact] ?: noIndex.takeIf { preset.hasNone(answer.fact) } ?: continue
        answers.add(answer.isCorrect, answer.time)
    }

    val rows = preset.entries.mapNotNull { entry ->
        val answers = byIndex.getValue(entry.index)
        if (!entry.enabled && answers.tally.total == 0) null else IndexStats(entry.index, entry.enabled, answers.tally, answers.times)
    }
    val noIndexRow = IndexStats(index = null, enabled = true, noIndex.tally, noIndex.times).takeIf { it.answers > 0 }
    val every = byIndex.values + noIndex
    val overall = every.map { it.tally }.fold(Tally(correct = 0, incorrect = 0), Tally::plus)
    return FlashcardStats(overall, AnswerTimes.of(mergeSorted(every.map { it.sortedTimes })), rows + listOfNotNull(noIndexRow))
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
