package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.totalRow
import com.aquigs.sp21ace.domain.strategy.upcard

/**
 * The player's total as the chart's rows write it: "16" for a hard hand or any pair but aces, and "A-7" for soft 18. A pair of
 * aces reads "A-A", because no soft row prints the soft 12 it makes.
 */
val TrainerHand.playerTotal: String
    get() = if (player.size == 2 && player.all { it.rank == Rank.ACE }) chartRow(player).hand else totalRow(player).hand

/** The dealer's upcard as the chart's columns write it: "10" for any ten-value card and "A" for an ace. */
val TrainerHand.dealerTotal: String get() = upcard.upcard.label
