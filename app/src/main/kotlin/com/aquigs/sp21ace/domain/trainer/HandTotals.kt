package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.upcard

/**
 * The player's total as the chart's rows write it: "16" for a hard hand or a pair, "A-7" for soft 18, and "A-A" for the
 * soft 12 only a pair of aces makes.
 */
val TrainerHand.playerTotal: String
    get() {
        val total = player.total()

        return when {
            !total.soft -> "${total.value}"
            total.value == 12 -> "A-A"
            else -> "A-${total.value - 11}"
        }
    }

/** The dealer's upcard as the chart's columns write it: "10" for any ten-value card and "A" for an ace. */
val TrainerHand.dealerTotal: String get() = upcard.upcard.label
