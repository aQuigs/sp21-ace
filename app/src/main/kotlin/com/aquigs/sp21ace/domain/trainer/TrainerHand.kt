package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.handClass
import com.aquigs.sp21ace.domain.strategy.upcard
import java.io.Serializable
import kotlin.random.Random

/** A trainer question: the player's two cards against the dealer's upcard. The hole card stays face down, so it is never drawn. */
data class TrainerHand(val player: List<Card>, val upcard: Card) : Serializable {
    val matchup: String get() = "${handClass(player)} vs ${upcard.upcard.label}"
}

// Six decks, the shoe size every chart is published for
private val SHOE = spanishShoe(decks = 6)

/** Deals the top three cards of a freshly shuffled shoe, redealing a hand the trainer never asks. */
fun dealTrainerHand(random: Random = Random.Default): TrainerHand =
    generateSequence { SHOE.shuffled(random) }
        .map { (first, up, second) -> TrainerHand(listOf(first, second), up) }
        .first { isAsked(it.player) }

/** The chart rows of every hand the trainer deals, read from every two cards of the shoe it asks, so they follow the deal. */
val DEALT_ROWS: Set<ChartRow> = SHOE.distinct().let { cards -> cards.flatMap { first -> cards.map { listOf(first, it) } } }
    .filter(::isAsked)
    .mapTo(mutableSetOf(), ::chartRow)

// A player blackjack leaves nothing to decide
private fun isAsked(player: List<Card>): Boolean = !player.isBlackjack()
