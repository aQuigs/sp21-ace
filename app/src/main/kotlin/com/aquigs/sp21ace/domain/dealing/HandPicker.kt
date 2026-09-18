package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

/**
 * The lowest accuracy a hand is weighed at, so a hand never answered right comes up 20 times as often as one always answered
 * right, rather than every time until it is.
 */
internal const val ACCURACY_FLOOR = 0.05

private const val NO_ANSWERS_ACCURACY = 0.5

/** A hand as Prioritize worse hands tells hands apart: the player's two card values, lower first, against the upcard's. Suits, and J, Q and K, read alike. */
internal data class HandValues(val low: Upcard, val high: Upcard, val upcard: Upcard)

internal val TrainerHand.values: HandValues
    get() {
        require(player.size == 2) { "A dealt hand has two cards, not ${player.size}" }
        val (low, high) = player.map { it.upcard }.sorted()
        return HandValues(low, high, upcard.upcard)
    }

/** The type [chart] reads the hand as: its kind of hand and its correct move. */
internal fun TrainerHand.type(chart: StrategyChart) = HandType(chartRow(player).table, chart.firstMove(player, upcard))

/** Every answer ever given to each two-card hand, however old and whatever rules graded it, as Blackjack Ace counts them. */
internal fun List<PracticeAnswer>.tallyByHand(): Map<HandValues, Tally> = filter { it.hand.player.size == 2 }
    .groupingBy { it.hand.values }
    .fold(Tally(correct = 0, incorrect = 0)) { tally, answer ->
        if (answer.isCorrect) tally.copy(correct = tally.correct + 1) else tally.copy(incorrect = tally.incorrect + 1)
    }

/** How heavily a hand weighs under Prioritize worse hands: the inverse of its accuracy, with no answers counting as 50%. */
internal fun weight(tally: Tally?): Double {
    val accuracy = tally?.takeIf { it.total > 0 }?.let { it.correct.toDouble() / it.total } ?: NO_ANSWERS_ACCURACY
    return 1 / maxOf(accuracy, ACCURACY_FLOOR)
}

private val SHOE_BY_VALUE: Map<Upcard, List<Card>> = spanishShoe(decks = 6).groupBy { it.upcard }

// A bonus exception tells two cards apart only by whether they share a suit and whether that suit is spades
private val SUIT_PATTERNS = listOf(Suit.HEARTS to Suit.CLUBS, Suit.HEARTS to Suit.HEARTS, Suit.SPADES to Suit.SPADES)

private fun Upcard.card(suit: Suit) = Card(Rank.entries.first { Card(it, suit).upcard == this }, suit)

// Every hand but a blackjack, which the trainer never deals
private val HAND_VALUES: List<HandValues> = Upcard.entries.flatMap { low ->
    Upcard.entries.filter { it >= low }.flatMap { high -> Upcard.entries.map { HandValues(low, high, it) } }
}.filterNot { listOf(it.low.card(Suit.HEARTS), it.high.card(Suit.CLUBS)).isBlackjack() }

private fun StrategyChart.types(hand: HandValues): Set<HandType> = SUIT_PATTERNS.mapTo(HashSet()) { (first, second) ->
    TrainerHand(listOf(hand.low.card(first), hand.high.card(second)), hand.upcard.card(Suit.DIAMONDS)).type(this)
}

private val TYPES_BY_HAND: Map<RuleSet, Map<HandValues, Set<HandType>>> = RuleSet.entries.associateWith { rules ->
    StrategyCharts.forRules(rules).let { chart -> HAND_VALUES.associateWith { chart.types(it) } }
}

/** The ways a six-deck shoe deals these values as the first card, the upcard and the second card, either card first. */
private val HandValues.waysToDeal: Long
    get() {
        val ways = listOf(low, high, upcard).groupingBy { it }.eachCount().entries.fold(1L) { ways, (value, times) ->
            (0 until times).fold(ways) { product, drawn -> product * (SHOE_BY_VALUE.getValue(value).size - drawn) }
        }
        return if (low == high) ways else ways * 2
    }

// Each card is drawn from what the shoe holds of its value, so suits, and J, Q and K, come up as often as a shuffled shoe deals them
private fun HandValues.deal(random: Random): TrainerHand {
    val shuffled = HashMap<Upcard, Iterator<Card>>()
    fun draw(value: Upcard) = shuffled.getOrPut(value) { SHOE_BY_VALUE.getValue(value).shuffled(random).iterator() }.next()

    val (first, second) = listOf(low, high).shuffled(random)
    return TrainerHand(listOf(draw(first), draw(second)), draw(upcard))
}

/**
 * Deals the trainer's hands under [rules]: only the types still switched on, or every type when none of those can come up,
 * and each hand as often as a shuffled shoe deals it or in inverse proportion to the accuracy [history] gives it.
 */
class HandPicker(rules: RuleSet, customization: HandCustomization, history: List<PracticeAnswer>) {
    private val chart = StrategyCharts.forRules(rules)
    private val typesByHand = TYPES_BY_HAND.getValue(rules)
    private val dealt = typesByHand.values.flatten().toSet().let { possible -> (possible - customization.switchedOff).ifEmpty { possible } }

    /** The hands whose suits can make them a type still dealt. */
    internal val hands: List<HandValues> = HAND_VALUES.filter { hand -> typesByHand.getValue(hand).any { it in dealt } }

    private val runningWeights: List<Double> = when (customization.handsDealt) {
        HandsDealt.RANDOM -> hands.map { it.waysToDeal.toDouble() }
        HandsDealt.PRIORITIZE_WORSE -> history.tallyByHand().let { tallies -> hands.map { weight(tallies[it]) } }
    }.runningReduce { total, weight -> total + weight }

    // A hand whose suits made it a type switched off is dealt again from the start, so the patterns still on keep a shoe's odds
    fun pick(random: Random = Random.Default): TrainerHand = generateSequence {
        val target = random.nextDouble(runningWeights.last())
        hands[runningWeights.indexOfFirst { it > target }].deal(random)
    }.first { it.type(chart) in dealt }
}
