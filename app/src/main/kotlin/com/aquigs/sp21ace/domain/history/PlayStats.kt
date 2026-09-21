package com.aquigs.sp21ace.domain.history

import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.StrategyGrade
import java.time.Instant

/**
 * Blackjack Ace's Play Statistics over a period, in cents: how many hands were won, pushed and lost, what the wins came to and
 * the losses cost, and how many hands earned each strategy grade, which leaves out a grade none earned. [profits] is the profit after each hand in the order played,
 * from the 0 before the first, which the chart draws.
 */
data class PlayStats(
    val won: Int,
    val pushed: Int,
    val lost: Int,
    val amountWon: Long,
    val amountLost: Long,
    val profits: List<Long>,
    val grades: Map<StrategyGrade, Int>,
) {
    val hands: Int get() = won + pushed + lost
    val profit: Long get() = amountWon - amountLost
}

/** Counts the hands played within [period] of [now], in the order given. */
fun List<PlayedHand>.playStats(period: Period, now: Instant): PlayStats {
    val counts = period.counter(now)
    val hands = filter { counts(it.playedAt) }
    val outcomes = hands.groupingBy { it.outcome }.eachCount()

    return PlayStats(
        won = outcomes[Outcome.WIN] ?: 0,
        pushed = outcomes[Outcome.PUSH] ?: 0,
        lost = outcomes[Outcome.LOSE] ?: 0,
        amountWon = hands.sumOf { maxOf(it.net, 0) },
        amountLost = hands.sumOf { maxOf(-it.net, 0) },
        profits = hands.runningFold(0L) { profit, hand -> profit + hand.net },
        grades = hands.groupingBy { it.grade }.eachCount(),
    )
}
