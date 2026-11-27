package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.FULL_DECK
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.totalOf
import com.aquigs.sp21ace.domain.drill.CardDealer
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.upcard
import java.time.Instant
import kotlin.random.Random
import kotlin.time.Duration

// Grouped as a dealer's upcard reads them, so a ten-value card is a 10, J, Q or K, each as often as a deck holds it
private val DECK_BY_VALUE: Map<Upcard, List<Card>> = FULL_DECK.groupBy { it.upcard }

// Occasional, so a drill stays mostly its indices
private const val CARDS_PER_NO_INDEX = 8

// A double, split or surrender is offered only on the first two cards, but a hand of any size stands or hits
private val ANY_SIZE_PLAYS = setOf(DeviationPlay.STAND, DeviationPlay.HIT)

/**
 * Every hand of 3 or 4 cards a basic strategy player could hold against [upcard], by its total, in the order its cards were dealt,
 * an ace as 1: every hand on the way [plainlyHits]. Each total's hands come grouped by their number of cards.
 */
internal fun multiCardHands(upcard: Upcard): Map<HandTotal, List<List<List<Int>>>> {
    fun drawOn(hands: List<List<Int>>) = hands.filter { plainlyHits(it, upcard) }.flatMap { hand -> (1..10).map { hand + it } }

    val threeCards = drawOn((1..10).flatMap { first -> (1..10).map { listOf(first, it) } })
    return (threeCards + drawOn(threeCards)).groupBy(::totalOf).mapValues { (_, hands) -> hands.groupBy { it.size }.values.toList() }
}

/**
 * The hands closest to [preset]'s switched-on indices that have none: on either side of each, the nearest upcard that no index the
 * preset lists or a built-in set defines has for its hand, whatever the play. Each asks for that index's play, so it passes for one.
 */
internal fun nearMisses(preset: IndexPreset): Set<NoIndex> {
    val builtIn = BuiltInIndexSet.entries.filter { it.game == preset.game }.flatMap { set -> set.indices.map { it.index.deviation } }
    fun hasIndex(deviation: Deviation.OnHand) = preset.listsHandOf(deviation) || builtIn.any { it.asksSameHandAs(deviation) }

    return preset.enabledIndices.flatMap { index ->
        val deviation = index.deviation as? Deviation.OnHand ?: return@flatMap emptyList()
        val undefined = Upcard.entries.map { deviation.copy(upcard = it) }.filterNot(::hasIndex)

        listOfNotNull(undefined.lastOrNull { it.upcard < deviation.upcard }, undefined.firstOrNull { it.upcard > deviation.upcard })
            .map { NoIndex(it, index.direction) }
    }.toSet()
}

/**
 * Deals the flashcards of [preset]'s switched-on indices as [options] says, built once per change of either, so a deal only
 * picks. Never the same index twice in a row while more than one is on. With [FlashcardOptions.noIndexCards] on, about 1 card in
 * 8 is one of its [nearMisses]. A two-card hard total is never a pair, a ten-value card
 * is a 10, J, Q or K at random, and insurance shows the dealer's ace alone. With [FlashcardOptions.multiCardHands] on, a stand
 * or hit index on a total is a hand of 3 or 4 cards half the time a basic strategy player could hold one against its upcard;
 * every other hand is two cards.
 */
class FlashcardDealer(private val preset: IndexPreset, private val options: FlashcardOptions = FlashcardOptions()) :
    CardDealer<Flashcard, Int?, FlashcardAnswer> {
    val game: Game = preset.game

    /** The indices dealt, in the preset's order. */
    val indices: Set<PlayIndex> = preset.enabledIndices.toSet()

    private val nearMisses: Set<NoIndex> = if (options.noIndexCards) nearMisses(preset) else emptySet()

    // Only for the facts dealt, since every upcard's hands would hold thousands of lists
    private val multiCardHands: Map<Deviation.OnHand, List<List<List<Int>>>> = if (!options.multiCardHands) emptyMap() else {
        val drawsOn = (indices + nearMisses).map { it.deviation }.filterIsInstance<Deviation.OnHand>().filter { it.play in ANY_SIZE_PLAYS && it.hand.kind != HandKind.PAIR }
        val byUpcard = drawsOn.map { it.upcard }.distinct().associateWith(::multiCardHands)
        drawsOn.associateWith { byUpcard.getValue(it.upcard)[it.hand.total].orEmpty() }.filterValues { it.isNotEmpty() }
    }
    private val multiCardSets: Map<Deviation.OnHand, Set<List<Int>>> = multiCardHands.mapValues { (_, bySize) -> bySize.flatten().toSet() }

    /**
     * A card for an index other than [previous]'s while there is another to deal, and never the very card [previous] showed, so
     * a tap on the last card can't grade this one. Under [CardsShown.PRIORITIZE_WORSE], an index comes up in inverse proportion to
     * the accuracy [history] gives it, as the trainer's hands do. Null while no index is switched on.
     */
    override fun deal(previous: Flashcard?, history: List<FlashcardAnswer>, random: Random): Flashcard? {
        val candidates = indices.filter { indices.size == 1 || it != previous?.fact }.ifEmpty { return null }
        val fact = dealNearMiss(previous, random) ?: options.cardsShown.pick(candidates, random) { history.tallyByFact(game) }

        // Every hand comes in more than one suit, so a redraw soon finds another
        return generateSequence { cardsFor(fact, random) }.first { it != previous }
    }

    private fun dealNearMiss(previous: Flashcard?, random: Random): NoIndex? {
        val candidates = nearMisses.filter { it != previous?.fact }
        return if (candidates.isEmpty() || random.nextInt(CARDS_PER_NO_INDEX) > 0) null else candidates.random(random)
    }

    /** Whether [card] is one this dealer could deal, so a card left from another preset or other options is redealt. */
    override fun couldDeal(card: Flashcard): Boolean {
        val deviation = card.fact.deviation
        return (card.fact in indices || card.fact in nearMisses) && (card.player.size <= 2 || card.player.map { it.rank.value } in multiCardSets[deviation].orEmpty())
    }

    override fun record(card: Flashcard, answer: Int?, time: Duration?, answeredAt: Instant): FlashcardAnswer =
        FlashcardAnswer(answeredAt, game, card.fact, answer, time, card.player.size)

    /** Hands with no index share one tally, as they share a row in the stats. */
    override fun tally(card: Flashcard, history: List<FlashcardAnswer>): Tally = history
        .filter { if (card.fact is NoIndex) preset.hasNone(it.fact) else it.fact == card.fact }
        .tallyByFact(game).values
        .fold(Tally(correct = 0, incorrect = 0), Tally::plus)

    private fun cardsFor(fact: IndexFact, random: Random): Flashcard = when (val deviation = fact.deviation) {
        Deviation.Insurance -> Flashcard(fact, player = emptyList(), upcard = draw(Upcard.ACE, random))
        is Deviation.OnHand -> Flashcard(fact, playerCards(deviation, random), draw(deviation.upcard, random))
    }

    private fun playerCards(deviation: Deviation.OnHand, random: Random): List<Card> {
        val bySize = multiCardHands[deviation]
        val points = if (bySize != null && random.nextBoolean()) bySize.random(random).random(random) else twoCards(deviation.hand, random)
        return points.map { draw(Upcard.ofPoints(it), random) }
    }

    private fun twoCards(hand: IndexHand, random: Random): List<Int> = when (hand.kind) {
        HandKind.PAIR -> listOf(hand.value, hand.value)
        HandKind.SOFT -> listOf(1, hand.value - 11)
        // Two different cards from 2 to 10, so never a pair and never an ace, which would make the total soft
        HandKind.HARD -> (2..10).filter { it < hand.value - it && hand.value - it <= 10 }.random(random).let { listOf(it, hand.value - it) }
    }.shuffled(random)

    private fun draw(value: Upcard, random: Random): Card = DECK_BY_VALUE.getValue(value).random(random)
}
