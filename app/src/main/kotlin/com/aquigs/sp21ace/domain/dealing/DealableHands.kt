package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.firstMove
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

// Six decks, the shoe size every chart is published for
private const val DECKS = 6

private val SHOE_BY_VALUE: Map<Upcard, List<Card>> = spanishShoe(DECKS).groupBy { it.upcard }

// Every two cards the shoe deals, the same card twice included, but no blackjack, which leaves nothing to decide
private val PLAYER_CARDS: List<List<Card>> = spanishShoe(decks = 1).let { deck ->
    deck.indices.flatMap { i -> (i until deck.size).map { j -> listOf(deck[i], deck[j]) } }.filterNot { it.isBlackjack() }
}

/** The chart rows of every hand the trainer deals, read from the cards it deals from, so the heatmap and the deal can't disagree. */
val DEALT_ROWS: Set<ChartRow> = PLAYER_CARDS.mapTo(HashSet(), ::chartRow)

/** Two cards, suits and all, against an upcard of one value: the type they're graded under a rule set, and the ways a six-deck shoe deals them. */
internal class DealableHand(val player: List<Card>, val upcard: Upcard, val type: HandType, val ways: Int)

private val DEALABLE: Map<RuleSet, Lazy<Map<HandValues, List<DealableHand>>>> =
    RuleSet.entries.associateWith { rules -> lazy { StrategyCharts.forRules(rules).dealableHands() } }

/** Every hand the trainer can deal under [rules], by card values. Built on first use, and only for the rules in play. */
internal fun dealableHands(rules: RuleSet): Map<HandValues, List<DealableHand>> = DEALABLE.getValue(rules).value

private fun StrategyChart.dealableHands(): Map<HandValues, List<DealableHand>> {
    val byValues = LinkedHashMap<HandValues, MutableList<DealableHand>>()

    // The row and values once per two cards, since the upcard changes neither
    for (player in PLAYER_CARDS) {
        val table = chartRow(player).table
        val (low, high) = player.map { it.upcard }.sorted()

        for (upcard in Upcard.entries) {
            // Grading reads an upcard by its value, so any card of it stands for the rest
            val type = HandType(table, firstMove(player, SHOE_BY_VALUE.getValue(upcard).first()))
            byValues.getOrPut(HandValues(low, high, upcard)) { mutableListOf() } += DealableHand(player, upcard, type, waysToDeal(player, upcard))
        }
    }
    return byValues
}

// Three different cards from the shoe: a copy of one player card, a copy of the other left after it, and a card of the
// upcard's value left after both, with either player card dealt first
private fun waysToDeal(player: List<Card>, upcard: Upcard): Int {
    val (first, second) = player
    val firstDraws = DECKS
    val secondDraws = if (second == first) DECKS - 1 else DECKS
    val upcardDraws = SHOE_BY_VALUE.getValue(upcard).size - player.count { it.upcard == upcard }
    val orders = if (second == first) 1 else 2
    return firstDraws * secondDraws * upcardDraws * orders
}

/** The hand as a shoe deals it: the player's cards in either order, and the upcard from what the shoe has left of its value, so its suit, or J, Q or K, comes up as often. */
internal fun DealableHand.deal(random: Random): TrainerHand {
    val left = SHOE_BY_VALUE.getValue(upcard).toMutableList().apply { player.forEach { remove(it) } }
    return TrainerHand(player.shuffled(random), left.random(random))
}

/** The type [chart] grades the hand as: its kind of hand and its correct move. */
internal fun TrainerHand.type(chart: StrategyChart) = HandType(chartRow(player).table, chart.firstMove(player, upcard))
