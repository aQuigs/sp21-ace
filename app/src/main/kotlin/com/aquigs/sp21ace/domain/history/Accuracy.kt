package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
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

fun List<PracticeAnswer>.inPeriod(period: Period, clock: Clock): List<PracticeAnswer> {
    val start = period.start(clock) ?: return this
    return filter { it.answeredAt >= start }
}

/** The hands a tab counts: one chart table's, or every hand. [moves] are the correct moves those hands call for under any rule set, in Blackjack Ace's order. */
enum class HandFilter(val table: ChartTable?, val moves: List<Move>) {
    HARD(ChartTable.HARD, listOf(Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    SOFT(ChartTable.SOFT, listOf(Move.HIT, Move.DOUBLE, Move.STAND)),
    PAIRS(ChartTable.PAIRS, listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    ALL(null, listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
}

fun List<PracticeAnswer>.ofHands(hands: HandFilter): List<PracticeAnswer> = hands.table?.let { table -> filter { it.row.table == table } } ?: this

data class Tally(val correct: Int, val incorrect: Int) {
    /** Null with nothing answered, which is no data rather than none right. */
    val accuracy: Double? get() = (correct + incorrect).takeIf { it > 0 }?.let { correct.toDouble() / it }
}

fun List<PracticeAnswer>.tally(): Tally = count { it.isCorrect }.let { Tally(correct = it, incorrect = size - it) }

/** A tally for every move, those no answer called for included. */
fun List<PracticeAnswer>.tallyByCorrectMove(): Map<Move, Tally> {
    val byMove = groupBy { it.correctMove }
    return Move.entries.associateWith { byMove[it].orEmpty().tally() }
}

/** The most right answers in a row, in the order they were given. */
fun List<PracticeAnswer>.longestStreak(): Int = runningFold(0) { run, answer -> if (answer.isCorrect) run + 1 else 0 }.max()
