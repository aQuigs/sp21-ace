package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.strategy.Upcard
import kotlin.random.Random

private val TEN_VALUES = listOf(Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING)

/**
 * Deals the flashcards of [preset]'s switched-on indices, built once per change of preset, so a deal only picks. Every index is as
 * likely as the next, and never the same one twice in a row while more than one is on. Every hand is two cards: a hard total is
 * never a pair, a ten-value card is a 10, J, Q or K at random, and insurance shows the dealer's ace alone.
 */
class FlashcardDealer(preset: IndexPreset) {
    val game: Game = preset.game

    /** The indices dealt, in the preset's order. */
    val indices: Set<PlayIndex> = preset.enabledIndices.toSet()

    private val picks = indices.toList()

    /**
     * A card for an index other than [previous]'s while there is another to deal, and never the very card [previous] showed, so
     * a tap on the last card can't grade this one. Null while no index is switched on.
     */
    fun deal(previous: Flashcard?, random: Random = Random.Default): Flashcard? {
        val index = picks.filter { picks.size == 1 || it != previous?.index }.randomOrNull(random) ?: return null
        // Every hand comes in more than one suit, so a redraw soon finds another
        return generateSequence { cardsFor(index, random) }.first { it != previous }
    }

    private fun cardsFor(index: PlayIndex, random: Random): Flashcard = when (val deviation = index.deviation) {
        Deviation.Insurance -> Flashcard(index, player = emptyList(), upcard = card(1, random))
        is Deviation.OnHand -> Flashcard(index, twoCards(deviation.hand, random), upcard(deviation.upcard, random))
    }

    private fun twoCards(hand: IndexHand, random: Random): List<Card> {
        val values = when (hand.kind) {
            HandKind.PAIR -> listOf(hand.value, hand.value)
            HandKind.SOFT -> listOf(1, hand.value - 11)
            // Two different cards from 2 to 10, so never a pair and never an ace, which would make the total soft
            HandKind.HARD -> (2..10).filter { it < hand.value - it && hand.value - it <= 10 }.random(random).let { listOf(it, hand.value - it) }
        }
        return values.shuffled(random).map { card(it, random) }
    }

    private fun upcard(upcard: Upcard, random: Random): Card = card(if (upcard == Upcard.ACE) 1 else upcard.label.toInt(), random)

    private fun card(value: Int, random: Random): Card {
        val rank = if (value == 10) TEN_VALUES.random(random) else Rank.entries.first { it.value == value }
        return Card(rank, Suit.entries.random(random))
    }
}
