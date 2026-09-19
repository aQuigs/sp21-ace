package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.HandTotal
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.plusCard
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.move
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.strategy.row
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import kotlin.random.Random

// Six decks, the shoe size every chart is published for
private const val DECKS = 6

private val SHOE: List<Card> = spanishShoe(DECKS)

private val SHOE_BY_VALUE: Map<Upcard, List<Card>> = SHOE.groupBy { it.upcard }

private val POINTS: Map<Upcard, Int> = SHOE_BY_VALUE.mapValues { it.value.first().rank.value }

// How often a card drawn from a full shoe is worth each value
private val DRAW_CHANCES: Map<Upcard, Double> = SHOE_BY_VALUE.mapValues { it.value.size.toDouble() / SHOE.size }

// Every order of three different cards from the shoe, so a two-card hand's ways to deal become how often a round deals it
private val DEALS_OF_THREE: Double = SHOE.size * (SHOE.size - 1.0) * (SHOE.size - 2.0)

// Every two cards the shoe deals, the same card twice included, but no blackjack, which leaves nothing to decide
private val PLAYER_CARDS: List<List<Card>> = spanishShoe(decks = 1).let { deck ->
    deck.indices.flatMap { i -> (i until deck.size).map { j -> listOf(deck[i], deck[j]) } }.filterNot { it.isBlackjack() }
}

/** A way the trainer deals a hand: the type it's graded as, how often a round deals it this way, and a deal of it. */
internal interface Dealable {
    val type: HandType
    val chance: Double

    fun deal(random: Random): TrainerHand
}

/** Two cards, suits and all, against an upcard of one value, and the ways a six-deck shoe deals them. */
internal class DealableHand(val player: List<Card>, val upcard: Upcard, override val type: HandType, val ways: Int) : Dealable {
    override val chance: Double get() = ways / DEALS_OF_THREE

    /** The hand as a shoe deals it: the player's cards in either order, and the upcard from what the shoe has left. */
    override fun deal(random: Random): TrainerHand = TrainerHand(player.shuffled(random), drawCard(upcard, player, random))
}

/** Every hand the trainer can deal under a rule set, as Prioritize worse hands tells them apart, each with its ways to deal. */
internal class DealableHands(val hands: Map<HandKey, List<Dealable>>) {
    val rows: Set<ChartRow> = PLAYER_CARDS.mapTo(HashSet(), ::chartRow) + hands.keys.filterIsInstance<MultiCardHand>().map { it.row }
}

private val DEALABLE: Map<RuleSet, Lazy<DealableHands>> = RuleSet.entries.associateWith { rules -> lazy { StrategyCharts.forRules(rules).dealableHands() } }

/** Every hand the trainer can deal under [rules]. Built on first use, and only for the rules in play. */
internal fun dealableHands(rules: RuleSet): DealableHands = DEALABLE.getValue(rules).value

/** The chart rows of every hand the trainer deals under [rules], read from the hands it deals, so the heatmap and the deal can't disagree. */
fun dealtRows(rules: RuleSet): Set<ChartRow> = dealableHands(rules).rows

private fun StrategyChart.dealableHands(): DealableHands {
    val twoCard = LinkedHashMap<HandValues, MutableList<DealableHand>>()

    // The row and values once per two cards, since the upcard changes neither
    for (player in PLAYER_CARDS) {
        val table = chartRow(player).table
        val (low, high) = player.map { it.upcard }.sorted()

        for (upcard in Upcard.entries) {
            // Grading reads an upcard by its value, so any card of it stands for the rest
            val type = HandType(table, correctMove(player, SHOE_BY_VALUE.getValue(upcard).first()))
            twoCard.getOrPut(HandValues(low, high, upcard)) { mutableListOf() } += DealableHand(player, upcard, type, waysToDeal(player, upcard))
        }
    }

    val hits = twoCard.values.flatten().filter { it.type.move == Move.HIT }.groupBy { it.upcard }
    return DealableHands(
        buildMap {
            putAll(twoCard)
            hits.forEach { (upcard, starts) -> putAll(Hitting(this@dealableHands, upcard, starts).hands()) }
        },
    )
}

/** A total a hand reaches, and how many cards make it. */
private data class Reached(val total: HandTotal, val cards: Int)

/** A card hit to reach a total: the total before it, and the card's value. */
private data class Hit(val from: HandTotal, val value: Upcard)

/**
 * The hands of 3 or more cards a player reaches against [upcard] by hitting where the chart says hit, from the two-card hands in
 * [starts] the chart hits. Only those two cards and the upcard come out of the shoe. Each card hit after them is drawn as from a
 * full shoe: the few cards gone by then shift its chances by a shade, and counting them out would take a total for every set of
 * cards drawn rather than one for every total.
 */
private class Hitting(private val chart: StrategyChart, private val upcard: Upcard, starts: List<DealableHand>) {
    private val startsByTotal: Map<HandTotal, Weighted<DealableHand>> = starts.groupBy { it.player.total() }.mapValues { (_, hands) -> Weighted(hands) { it.chance } }

    // Every total reached with 3 or more cards, and the hits reaching it, each weighed by how often a round comes that way
    private val hitsInto = HashMap<Reached, Weighted<Pair<Hit, Double>>>()

    init {
        var hitting: Map<HandTotal, Double> = startsByTotal.mapValues { it.value.total }
        var cards = 2

        while (hitting.isNotEmpty()) {
            cards++
            val reached = HashMap<HandTotal, MutableList<Pair<Hit, Double>>>()
            for ((total, chance) in hitting) {
                for ((value, drawChance) in DRAW_CHANCES) {
                    val drawn = total.plusCard(POINTS.getValue(value))
                    // A 21 or a bust leaves nothing to decide
                    if (drawn.value < 21) reached.getOrPut(drawn) { mutableListOf() } += Hit(total, value) to chance * drawChance
                }
            }

            for ((total, hits) in reached) hitsInto[Reached(total, cards)] = Weighted(hits) { it.second }
            hitting = reached.filterKeys { move(it, cards) == Move.HIT }.mapValues { (_, hits) -> hits.sumOf { it.second } }
        }
    }

    fun hands(): Map<MultiCardHand, List<Dealable>> {
        // In the order reached rather than by hash, which an enum's changes from run to run, so a seeded Random deals the same hands every run
        val hands = LinkedHashMap<MultiCardHand, MutableList<Dealable>>()
        for ((end, hits) in hitsInto) {
            val hand = MultiCardHand(end.total.row, upcard, move(end.total, end.cards))
            hands.getOrPut(hand) { mutableListOf() } += End(end, hand.type, hits.total)
        }
        return hands
    }

    private fun move(total: HandTotal, cards: Int): Move = chart.play(total.row, upcard).move(cards)

    // A total reached with a count of cards, dealt by tracing back from it a hit at a time, as often as a round comes each way
    private inner class End(private val end: Reached, override val type: HandType, override val chance: Double) : Dealable {
        override fun deal(random: Random): TrainerHand {
            var at = end
            val drawn = ArrayDeque<Upcard>()
            while (at.cards > 2) {
                val (hit, _) = hitsInto.getValue(at).pick(random)
                drawn.addFirst(hit.value)
                at = Reached(hit.from, at.cards - 1)
            }

            val start = startsByTotal.getValue(at.total).pick(random).deal(random)
            return drawn.fold(start) { hand, value -> hand.copy(player = hand.player + drawCard(value, hand.player + hand.upcard, random)) }
        }
    }
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

// A card of [value] from what the shoe has left once [dealt] are out of it, so its suit, or J, Q or K, comes up as often
private fun drawCard(value: Upcard, dealt: List<Card>, random: Random): Card =
    SHOE_BY_VALUE.getValue(value).toMutableList().apply { dealt.forEach { remove(it) } }.random(random)

/** The type [chart] grades the hand as: its kind of hand and its correct move. */
internal fun TrainerHand.type(chart: StrategyChart) = HandType(chartRow(player).table, chart.correctMove(player, upcard))
