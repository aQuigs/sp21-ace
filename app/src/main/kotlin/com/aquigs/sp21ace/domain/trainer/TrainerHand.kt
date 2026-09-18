package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import kotlin.random.Random

/** A trainer question: the player's two cards against the dealer's upcard. The hole card stays face down, so it is never drawn. */
data class TrainerHand(val player: List<Card>, val upcard: Card)

// Six decks, the shoe size every chart is published for
private val SHOE = spanishShoe(decks = 6)

/** Deals the top three cards of a freshly shuffled shoe, redealing a player blackjack because it leaves nothing to decide. */
fun dealTrainerHand(random: Random = Random.Default): TrainerHand =
    generateSequence { SHOE.shuffled(random) }
        .map { (first, up, second) -> TrainerHand(listOf(first, second), up) }
        .first { !it.player.isBlackjack() }
