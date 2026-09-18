package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.strategy.handClass
import com.aquigs.sp21ace.domain.strategy.upcard
import java.io.Serializable

/** A trainer question: the player's two cards against the dealer's upcard. The hole card stays face down, so it is never drawn. */
data class TrainerHand(val player: List<Card>, val upcard: Card) : Serializable {
    val matchup: String get() = "${handClass(player)} vs ${upcard.upcard.label}"
}
