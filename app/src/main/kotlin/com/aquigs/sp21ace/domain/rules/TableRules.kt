package com.aquigs.sp21ace.domain.rules

import com.aquigs.sp21ace.domain.strategy.RuleSet

/**
 * The rules the player sets, each combination picking one published chart. Casinos only offer redoubling where the dealer
 * hits soft 17, so standing ignores it, but keeps it for when the dealer hits again.
 */
data class TableRules(val dealerHitsSoft17: Boolean = false, val redoubling: Boolean = false) {
    val ruleSet: RuleSet
        get() = when {
            !dealerHitsSoft17 -> RuleSet.S17
            redoubling -> RuleSet.H17_REDOUBLE
            else -> RuleSet.H17
        }
}
