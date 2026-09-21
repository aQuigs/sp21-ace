package com.aquigs.sp21ace.domain.game

import java.io.Serializable

/** How a hand's play measured up to the chart, as Blackjack Ace's Play Statistics count it. */
enum class StrategyGrade { CORRECT, CORRECT_WITH_HINTS, INCORRECT, NO_ACTION_REQUIRED }

/**
 * How a hand was played against the chart: whether any decision was made on it, whether any went against the chart, and whether
 * any had help, the hint shown or a warning backed out of.
 */
data class StrategyRecord(val decided: Boolean = false, val incorrect: Boolean = false, val helped: Boolean = false) : Serializable {
    fun withDecision(correct: Boolean): StrategyRecord = copy(decided = true, incorrect = incorrect || !correct)

    // A wrong decision outweighs any help, as in Blackjack Ace
    val grade: StrategyGrade
        get() = when {
            !decided -> StrategyGrade.NO_ACTION_REQUIRED
            incorrect -> StrategyGrade.INCORRECT
            helped -> StrategyGrade.CORRECT_WITH_HINTS
            else -> StrategyGrade.CORRECT
        }
}
