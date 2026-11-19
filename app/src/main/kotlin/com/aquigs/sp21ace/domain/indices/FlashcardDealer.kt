package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.FULL_DECK
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.upcard
import kotlin.random.Random

// Grouped as a dealer's upcard reads them, so a ten-value card is a 10, J, Q or K, each as often as a deck holds it
private val DECK_BY_VALUE: Map<Upcard, List<Card>> = FULL_DECK.groupBy { it.upcard }

/**
 * Deals the flashcards of [preset]'s switched-on indices, built once per change of preset, so a deal only picks. Every index is as
 * likely as the next, and never the same one twice in a row while more than one is on. Every hand is two cards: a hard total is
 * never a pair, a ten-value card is a 10, J, Q or K at random, and insurance shows the dealer's ace alone.
 */
class FlashcardDealer(preset: IndexPreset) {
    val game: Game = preset.game

    /** The indices dealt, in the preset's order. */
    val indices: Set<PlayIndex> = preset.enabledIndices.toSet()

    /**
     * A card for an index other than [previous]'s while there is another to deal, and never the very card [previous] showed, so
     * a tap on the last card can't grade this one. Null while no index is switched on.
     */
    fun deal(previous: Flashcard?, random: Random = Random.Default): Flashcard? {
        val index = indices.filter { indices.size == 1 || it != previous?.index }.randomOrNull(random) ?: return null
        // Every hand comes in more than one suit, so a redraw soon finds another
        return generateSequence { cardsFor(index, random) }.first { it != previous }
    }

    private fun cardsFor(index: PlayIndex, random: Random): Flashcard = when (val deviation = index.deviation) {
        Deviation.Insurance -> Flashcard(index, player = emptyList(), upcard = draw(Upcard.ACE, random))
        is Deviation.OnHand -> Flashcard(index, twoCards(deviation.hand, random), draw(deviation.upcard, random))
    }

    private fun twoCards(hand: IndexHand, random: Random): List<Card> {
        val values = when (hand.kind) {
            HandKind.PAIR -> listOf(hand.pairCard, hand.pairCard)
            HandKind.SOFT -> listOf(Upcard.ACE, valueOf(hand.value - 11))
            // Two different cards from 2 to 10, so never a pair and never an ace, which would make the total soft
            HandKind.HARD -> (2..10).filter { it < hand.value - it && hand.value - it <= 10 }.random(random).let { listOf(valueOf(it), valueOf(hand.value - it)) }
        }
        return values.shuffled(random).map { draw(it, random) }
    }

    private fun valueOf(points: Int): Upcard = if (points == 10) Upcard.TEN else Upcard.fromLabel("$points")

    private fun draw(value: Upcard, random: Random): Card = DECK_BY_VALUE.getValue(value).random(random)
}
