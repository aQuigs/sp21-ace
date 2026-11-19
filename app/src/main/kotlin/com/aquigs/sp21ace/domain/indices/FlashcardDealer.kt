package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.FULL_DECK
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.dealing.Weighted
import com.aquigs.sp21ace.domain.dealing.weight
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.upcard
import kotlin.random.Random

// Grouped as a dealer's upcard reads them, so a ten-value card is a 10, J, Q or K, each as often as a deck holds it
private val DECK_BY_VALUE: Map<Upcard, List<Card>> = FULL_DECK.groupBy { it.upcard }

// A double, split or surrender is offered only on the first two cards, but a hand of any size stands or hits
private val ANY_SIZE_PLAYS = setOf(DeviationPlay.STAND, DeviationPlay.HIT)

/**
 * Every hand of 3 or 4 cards a basic strategy player could hold against each upcard, by its total, in the order its cards were
 * dealt, an ace as 1: every hand on the way [plainlyHits]. Each total's hands come grouped by their number of cards.
 */
internal val MULTI_CARD_HANDS: Map<Upcard, Map<HandTotal, List<List<List<Int>>>>> by lazy {
    val twoCards = (1..10).flatMap { first -> (1..10).map { listOf(first, it) } }

    Upcard.entries.associateWith { upcard ->
        fun drawOn(hands: List<List<Int>>) = hands.filter { plainlyHits(it, upcard) }.flatMap { hand -> (1..10).map { hand + it } }

        val threeCards = drawOn(twoCards)
        (threeCards + drawOn(threeCards)).groupBy(::totalOf).mapValues { (_, hands) -> hands.groupBy { it.size }.values.toList() }
    }
}

/**
 * Deals the flashcards of [preset]'s switched-on indices as [options] says, built once per change of either, so a deal only
 * picks. Never the same index twice in a row while more than one is on. A two-card hard total is never a pair, a ten-value card
 * is a 10, J, Q or K at random, and insurance shows the dealer's ace alone. With [FlashcardOptions.multiCardHands] on, a stand
 * or hit index on a total is a hand of 3 or 4 cards half the time a basic strategy player could hold one against its upcard;
 * every other hand is two cards.
 */
class FlashcardDealer(preset: IndexPreset, private val options: FlashcardOptions = FlashcardOptions()) {
    val game: Game = preset.game

    /** The indices dealt, in the preset's order. */
    val indices: Set<PlayIndex> = preset.enabledIndices.toSet()

    /**
     * A card for an index other than [previous]'s while there is another to deal, and never the very card [previous] showed, so
     * a tap on the last card can't grade this one. Under [CardsShown.PRIORITIZE_WORSE], an index comes up in inverse proportion to
     * the accuracy [history] gives it, as the trainer's hands do. Null while no index is switched on.
     */
    fun deal(previous: Flashcard?, history: List<FlashcardAnswer> = emptyList(), random: Random = Random.Default): Flashcard? {
        val candidates = indices.filter { indices.size == 1 || it != previous?.index }.ifEmpty { return null }
        val index = when (options.cardsShown) {
            CardsShown.RANDOM -> candidates.random(random)
            CardsShown.PRIORITIZE_WORSE -> history.tallyByIndex(game).let { tallies -> Weighted(candidates) { weight(tallies[it]) } }.pick(random)
        }

        // Every hand comes in more than one suit, so a redraw soon finds another
        return generateSequence { cardsFor(index, random) }.first { it != previous }
    }

    /** Whether [card] is one this dealer could deal, so a card left from another preset or other options is redealt. */
    fun couldDeal(card: Flashcard): Boolean {
        val deviation = card.index.deviation
        return card.index in indices &&
            (card.player.size <= 2 || deviation is Deviation.OnHand && card.player.map { it.rank.value } in multiCardHands(deviation).flatten())
    }

    private fun multiCardHands(deviation: Deviation.OnHand): List<List<List<Int>>> {
        val hand = deviation.hand
        val drawsOn = options.multiCardHands && deviation.play in ANY_SIZE_PLAYS && hand.kind != HandKind.PAIR
        return if (drawsOn) MULTI_CARD_HANDS.getValue(deviation.upcard)[hand.total].orEmpty() else emptyList()
    }

    private fun cardsFor(index: PlayIndex, random: Random): Flashcard = when (val deviation = index.deviation) {
        Deviation.Insurance -> Flashcard(index, player = emptyList(), upcard = draw(Upcard.ACE, random))
        is Deviation.OnHand -> Flashcard(index, playerCards(deviation, random), draw(deviation.upcard, random))
    }

    private fun playerCards(deviation: Deviation.OnHand, random: Random): List<Card> {
        val bySize = multiCardHands(deviation)
        val points = if (bySize.isNotEmpty() && random.nextBoolean()) bySize.random(random).random(random) else twoCards(deviation.hand, random)
        return points.map { draw(valueOf(it), random) }
    }

    private fun twoCards(hand: IndexHand, random: Random): List<Int> = when (hand.kind) {
        HandKind.PAIR -> listOf(hand.value, hand.value)
        HandKind.SOFT -> listOf(1, hand.value - 11)
        // Two different cards from 2 to 10, so never a pair and never an ace, which would make the total soft
        HandKind.HARD -> (2..10).filter { it < hand.value - it && hand.value - it <= 10 }.random(random).let { listOf(it, hand.value - it) }
    }.shuffled(random)

    private fun valueOf(points: Int): Upcard = when (points) {
        1 -> Upcard.ACE
        10 -> Upcard.TEN
        else -> Upcard.fromLabel("$points")
    }

    private fun draw(value: Upcard, random: Random): Card = DECK_BY_VALUE.getValue(value).random(random)
}
