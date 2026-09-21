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

/**
 * Whether a record stamped at a given time counts in the period as of [now]. One stamped after [now], as when the device's clock
 * was set ahead and then put back, counts only under All Time.
 */
fun Period.counter(now: Instant): (Instant) -> Boolean {
    val start = start(now) ?: return { true }
    return { at -> at > start && at <= now }
}

/**
 * The hands a tab counts: the ones filed under one chart table, or every hand [doubled] or every one not, as the chart splits
 * its tables. [moves] are the correct moves those hands call for under any rule set, in Blackjack Ace's order.
 */
enum class HandFilter(val table: ChartTable?, val moves: List<Move>, val doubled: Boolean = table?.afterDoubling == true) {
    HARD(ChartTable.HARD, listOf(Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    SOFT(ChartTable.SOFT, listOf(Move.HIT, Move.DOUBLE, Move.STAND)),
    PAIRS(ChartTable.PAIRS, listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    NOT_DOUBLED(null, listOf(Move.SPLIT, Move.HIT, Move.DOUBLE, Move.STAND, Move.SURRENDER)),
    AFTER_DOUBLE_HARD(ChartTable.AFTER_DOUBLE_HARD, listOf(Move.REDOUBLE, Move.STAND, Move.RESCUE)),
    AFTER_DOUBLE_SOFT(ChartTable.AFTER_DOUBLE_SOFT, listOf(Move.REDOUBLE, Move.STAND)),
    DOUBLED(null, listOf(Move.REDOUBLE, Move.STAND, Move.RESCUE), doubled = true),
    ;

    /** Whether the tab counts the hands read from [table]: those of its group, and of its own table if it has one. */
    fun counts(table: ChartTable): Boolean = table.afterDoubling == doubled && (this.table == null || this.table == table)
}

data class Tally(val correct: Int, val incorrect: Int) {
    val total: Int get() = correct + incorrect

    /** In tenths of a percent, rounded down so 100.0% means none wrong. Null with nothing answered, which is no data rather than none right. */
    val accuracyPermille: Int? get() = total.takeIf { it > 0 }?.let { (correct * 1000L / it).toInt() }

    operator fun plus(other: Tally): Tally = Tally(correct + other.correct, incorrect + other.incorrect)
}

/** Tallies answers under keys of the caller's choosing, holding only the keys added. */
class TallyCounter<K> {
    private val tallies = HashMap<K, Tally>()

    fun add(key: K, isCorrect: Boolean) {
        tallies.merge(key, if (isCorrect) Tally(correct = 1, incorrect = 0) else Tally(correct = 0, incorrect = 1), Tally::plus)
    }

    fun toMap(): Map<K, Tally> = tallies.toMap()
}

/**
 * A tab's figures over a period: a tally for every move the hands called for, and for every chart square answered, which a
 * square without answers is missing from. [longestStreak] is the most right answers in a row over every answer ever given,
 * whatever the period and tab, as Blackjack Ace's Streak card counts.
 */
data class Accuracy(val byMove: Map<Move, Tally>, val bySquare: Map<ChartSquare, Tally>, val longestStreak: Int) {
    val overall: Tally get() = byMove.values.fold(Tally(correct = 0, incorrect = 0), Tally::plus)
}

/**
 * Counts, in one pass and in the order given, the answers to [hands] within [period] of [now]. An answer's square comes from its
 * cards, so it stays in that square whatever rules graded it and whatever rules the chart now shows.
 */
fun List<PracticeAnswer>.accuracy(period: Period, hands: HandFilter, now: Instant): Accuracy {
    val counts = period.counter(now)
    val byMove = TallyCounter<Move>()
    val bySquare = TallyCounter<ChartSquare>()
    var streak = 0
    var longestStreak = 0

    for (answer in this) {
        streak = nextStreak(streak, answer.isCorrect)
        longestStreak = maxOf(longestStreak, streak)

        if (!counts(answer.answeredAt)) continue
        if (!hands.counts(answer.square.row.table)) continue

        byMove.add(answer.correctMove, answer.isCorrect)
        bySquare.add(answer.square, answer.isCorrect)
    }

    // A move no answer called for still gets a tally, so its card reads as no data
    val moves = byMove.toMap()
    return Accuracy(Move.entries.associateWith { moves[it] ?: Tally(correct = 0, incorrect = 0) }, bySquare.toMap(), longestStreak)
}
