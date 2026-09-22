package com.aquigs.sp21ace.domain.strategy

/**
 * The rules the player sets, each combination of the dealer's soft 17 and redoubling picking one published chart. Redoubling is
 * remembered while it isn't offered. [insurance] changes no chart, only whether the table offers the bet.
 */
data class TableRules(val dealerHitsSoft17: Boolean = false, val redoubling: Boolean = false, val insurance: Boolean = false) {
    /** Casinos only offer redoubling where the dealer hits soft 17. */
    val offersRedoubling: Boolean get() = dealerHitsSoft17

    val ruleSet: RuleSet
        get() = when {
            offersRedoubling && redoubling -> RuleSet.H17_REDOUBLE
            dealerHitsSoft17 -> RuleSet.H17
            else -> RuleSet.S17
        }
}
