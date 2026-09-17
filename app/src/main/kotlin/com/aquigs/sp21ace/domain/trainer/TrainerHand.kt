package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import kotlin.random.Random

/** A trainer question: the player's two cards against the dealer's upcard. The hole card stays face down, so it is never drawn. */
data class TrainerHand(val player: List<Card>, val upcard: Card)

// Six decks, the shoe size every chart is published for
private val SHOE = spanishShoe(decks = 6)

/** Draws three cards from a full shoe, redealing a player blackjack because it leaves nothing to decide. */
fun dealTrainerHand(random: Random = Random.Default): TrainerHand =
    generateSequence {
        val (first, up, second) = generateSequence { random.nextInt(SHOE.size) }.distinct().take(3).map(SHOE::get).toList()
        TrainerHand(listOf(first, second), up)
    }.first { !it.player.isBlackjack() }
