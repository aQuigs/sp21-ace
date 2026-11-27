package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.drill.AnswerCounter
import com.aquigs.sp21ace.domain.drill.AnswerTimes
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.TallyCounter
import com.aquigs.sp21ace.domain.history.counter
import java.time.Instant

/** A square of a shoe's heatmap: cards with [decksLeft] whose right answer on the pad was [answer]. */
data class TrueCountSquare(val decksLeft: Decks, val answer: Int)

/** A shoe's answers over a period, and how often they were right square by square, a square without answers missing. */
data class ShoeStats(val shoe: Int, val tally: Tally, val times: AnswerTimes, val bySquare: Map<TrueCountSquare, Tally>) {
    /** The decks left that came up, from the fullest shoe down. */
    val decksLeft: List<Decks> = bySquare.keys.map { it.decksLeft }.distinct().sortedDescending()
}

/**
 * Each shoe answered within [period] of [now], the smallest first. An answer counts under the rounding it was given, so a square
 * can hold answers given under several.
 */
fun List<TrueCountAnswer>.trueCountStats(period: Period, now: Instant): List<ShoeStats> {
    val counts = period.counter(now)

    return filter { counts(it.answeredAt) }.groupBy { it.card.shoe }.toSortedMap().map { (shoe, answers) ->
        val every = AnswerCounter()
        val bySquare = TallyCounter<TrueCountSquare>()
        for (answer in answers) {
            every.add(answer.isCorrect, answer.time)
            bySquare.add(TrueCountSquare(answer.card.decksLeft, answer.card.answer), answer.isCorrect)
        }

        ShoeStats(shoe, every.tally, every.times, bySquare.toMap())
    }
}
