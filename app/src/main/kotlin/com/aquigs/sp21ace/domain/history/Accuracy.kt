package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.strategy.ChartSquare
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.trainer.nextStreak
import java.time.Duration
import java.time.Instant

enum class Period { TODAY, WEEK, MONTH, ALL_TIME }

/**
 * How far back from [now] the period reaches, or null when it counts every answer. Periods roll, as Blackjack Ace's do, so
 * no midnight or start of the week resets them and no time zone moves them. An answer counts once it's newer than this, so
 * moving the clock on by a period's length drops it. Every boundary lives here, so how a period counts changes in one place.
 */
fun Period.start(now: Instant): Instant? = when (this) {
    Period.TODAY -> now.minus(Duration.ofHours(24))
    Period.WEEK -> now.minus(Duration.ofDays(7))
    Period.MONTH -> now.minus(Duration.ofDays(28))
    Period.ALL_TIME -> null
}

/** The hands a tab counts: one chart table's, or every hand. [moves] are the correct moves those hands call for under any rule set, in Blackjack Ace's order. */
enum class HandFilter(val table: ChartTable?, val moves: List<Move>) {
    HARD(ChartTable.HARD, listOf(Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    SOFT(ChartTable.SOFT, listOf(Move.HIT, Move.DOUBLE, Move.STAND)),
    PAIRS(ChartTable.PAIRS, listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    ALL(null, listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
}

data class Tally(val correct: Int, val incorrect: Int) {
    val total: Int get() = correct + incorrect

    /** In tenths of a percent, rounded down so 100.0% means none wrong. Null with nothing answered, which is no data rather than none right. */
    val accuracyPermille: Int? get() = total.takeIf { it > 0 }?.let { (correct * 1000L / it).toInt() }
}

/**
 * A tab's figures over a period: a tally for every move the hands called for, and for every chart square answered, which a
 * square without answers is missing from. [longestStreak] is the most right answers in a row over every answer ever given,
 * whatever the period and tab, as Blackjack Ace's Streak card counts.
 */
data class Accuracy(val byMove: Map<Move, Tally>, val bySquare: Map<ChartSquare, Tally>, val longestStreak: Int) {
    val overall: Tally get() = Tally(byMove.values.sumOf { it.correct }, byMove.values.sumOf { it.incorrect })
}

/**
 * Counts, in one pass and in the order given, the answers to [hands] within [period] of [now]. An answer stamped after [now],
 * as when the device's clock was set ahead and then put back, counts only under All Time. An answer's square comes from its
 * cards, so it stays in that square whatever rules graded it and whatever rules the chart now shows.
 */
fun List<PracticeAnswer>.accuracy(period: Period, hands: HandFilter, now: Instant): Accuracy {
    val start = period.start(now)
    val correct = IntArray(Move.entries.size)
    val incorrect = IntArray(Move.entries.size)
    val bySquare = HashMap<ChartSquare, Tally>()
    var streak = 0
    var longestStreak = 0

    for (answer in this) {
        streak = nextStreak(streak, answer.isCorrect)
        longestStreak = maxOf(longestStreak, streak)

        if (start != null && (answer.answeredAt <= start || answer.answeredAt > now)) continue
        val square = answer.square
        if (hands.table != null && square.row.table != hands.table) continue

        val counts = if (answer.isCorrect) correct else incorrect
        counts[answer.correctMove.ordinal]++

        val tally = bySquare[square] ?: Tally(correct = 0, incorrect = 0)
        bySquare[square] = if (answer.isCorrect) tally.copy(correct = tally.correct + 1) else tally.copy(incorrect = tally.incorrect + 1)
    }

    return Accuracy(Move.entries.associateWith { Tally(correct[it.ordinal], incorrect[it.ordinal]) }, bySquare, longestStreak)
}
