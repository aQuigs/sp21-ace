package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.trainer.nextStreak
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime

enum class Period { TODAY, WEEK, MONTH, ALL_TIME }

/**
 * The first moment the period counts, or null when it counts every answer. Today starts at midnight in [clock]'s time
 * zone, and Week and Month reach back 7 and 30 days there, so a daylight saving change doesn't move them by an hour.
 * Every boundary lives here, so how a period counts changes in one place.
 */
fun Period.start(clock: Clock): Instant? {
    val now = ZonedDateTime.now(clock)

    return when (this) {
        Period.TODAY -> now.toLocalDate().atStartOfDay(now.zone)
        Period.WEEK -> now.minusDays(7)
        Period.MONTH -> now.minusDays(30)
        Period.ALL_TIME -> null
    }?.toInstant()
}

/** When Today next starts over: the coming midnight in [clock]'s time zone. */
fun nextMidnight(clock: Clock): Instant = ZonedDateTime.now(clock).toLocalDate().plusDays(1).atStartOfDay(clock.zone).toInstant()

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

/** A tab's figures over a period: a tally for every move the hands called for, and the most right answers in a row. */
data class Accuracy(val byMove: Map<Move, Tally>, val longestStreak: Int) {
    val overall: Tally get() = Tally(byMove.values.sumOf { it.correct }, byMove.values.sumOf { it.incorrect })
}

/**
 * Counts, in one pass and in the order given, the answers to [hands] since [period] started. An answer stamped after now,
 * as when the device's clock was set ahead and then put back, counts only under All Time.
 */
fun List<PracticeAnswer>.accuracy(period: Period, hands: HandFilter, clock: Clock): Accuracy {
    val start = period.start(clock)
    val now = clock.instant()
    val correct = IntArray(Move.entries.size)
    val incorrect = IntArray(Move.entries.size)
    var streak = 0
    var longestStreak = 0

    for (answer in this) {
        if (start != null && answer.answeredAt !in start..now) continue
        if (hands.table != null && answer.row.table != hands.table) continue

        val counts = if (answer.isCorrect) correct else incorrect
        counts[answer.correctMove.ordinal]++
        streak = nextStreak(streak, answer.isCorrect)
        longestStreak = maxOf(longestStreak, streak)
    }

    return Accuracy(Move.entries.associateWith { Tally(correct[it.ordinal], incorrect[it.ordinal]) }, longestStreak)
}
