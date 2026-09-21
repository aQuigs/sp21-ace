package com.aquigs.sp21ace.domain.game

import java.io.Serializable

/** How a hand's play measured up to the chart, as Blackjack Ace's Play Statistics count it. */
enum class StrategyGrade { CORRECT, CORRECT_WITH_HINTS, INCORRECT, NO_ACTION_REQUIRED }

/**
 * How a hand was played against the chart: how many decisions were made on it, whether any went against the chart, and whether
 * any had help, the hint shown or a warning backed out of.
 */
data class StrategyRecord(val decisions: Int = 0, val incorrect: Boolean = false, val helped: Boolean = false) : Serializable {
    fun withDecision(correct: Boolean, helped: Boolean): StrategyRecord = StrategyRecord(decisions + 1, incorrect || !correct, this.helped || helped)

    // A wrong decision outweighs any help, as in Blackjack Ace
    val grade: StrategyGrade
        get() = when {
            decisions == 0 -> StrategyGrade.NO_ACTION_REQUIRED
            incorrect -> StrategyGrade.INCORRECT
            helped -> StrategyGrade.CORRECT_WITH_HINTS
            else -> StrategyGrade.CORRECT
        }
}
