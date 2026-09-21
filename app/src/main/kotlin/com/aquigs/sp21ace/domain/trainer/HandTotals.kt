package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.row
import com.aquigs.sp21ace.domain.strategy.upcard

/**
 * A hand's total as the chart's rows write it: "16" for a hard hand or any pair but aces, "A-7" for soft 18 and "A-A" for the soft
 * 12 of a pair of aces. A bust, which no row prints, reads as its total.
 */
fun totalLabel(cards: List<Card>): String = cards.total().let { if (it.value > 21) "${it.value}" else it.row.hand }

val TrainerHand.playerTotal: String get() = totalLabel(player)

/** The dealer's upcard as the chart's columns write it: "10" for any ten-value card and "A" for an ace. */
val TrainerHand.dealerTotal: String get() = upcard.upcard.label
