package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.totalRow
import com.aquigs.sp21ace.domain.strategy.upcard

/**
 * A hand's total as the chart's rows write it: "16" for a hard hand or any pair but aces, and "A-7" for soft 18. A pair of aces
 * reads "A-A", because no soft row prints the soft 12 it makes, and a bust, which no row prints either, reads as its total.
 */
fun totalLabel(cards: List<Card>): String = when {
    cards.size == 2 && cards.all { it.rank == Rank.ACE } -> chartRow(cards).hand
    cards.total().value > 21 -> "${cards.total().value}"
    else -> totalRow(cards).hand
}

val TrainerHand.playerTotal: String get() = totalLabel(player)

/** The dealer's upcard as the chart's columns write it: "10" for any ten-value card and "A" for an ace. */
val TrainerHand.dealerTotal: String get() = upcard.upcard.label
