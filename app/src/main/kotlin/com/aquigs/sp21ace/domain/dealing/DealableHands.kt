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
import com.aquigs.sp21ace.domain.strategy.afterDoublingRow
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.correctMoveAfterDoubling
import com.aquigs.sp21ace.domain.strategy.doubledRow
import com.aquigs.sp21ace.domain.strategy.move
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.strategy.row
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.correctMove
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

/** A way the trainer deals a hand: the type it's graded as, its total, how often a round deals it this way, and a deal of it. */
internal interface Dealable {
    val type: HandType
    val total: HandTotal
    val chance: Double

    fun deal(random: Random): TrainerHand
}

/** Two cards, suits and all, against an upcard of one value, and the ways a six-deck shoe deals them. */
internal class DealableHand(val player: List<Card>, val upcard: Upcard, override val type: HandType, val ways: Int) : Dealable {
    override val total: HandTotal get() = player.total()
    override val chance: Double get() = ways / DEALS_OF_THREE

    /** The hand as a shoe deals it: the player's cards in either order, and the upcard from what the shoe has left. */
    override fun deal(random: Random): TrainerHand = TrainerHand(player.shuffled(random), drawCard(upcard, player, random))
}

/** Every hand the trainer can deal under a rule set, as Prioritize worse hands tells them apart, each with its ways to deal. */
internal class DealableHands(val hands: Map<HandKey, List<Dealable>>) {
    val rows: Set<ChartRow> = PLAYER_CARDS.mapTo(HashSet(), ::chartRow) + hands.keys.filterIsInstance<TotalHand>().map { it.row }
}

private val DEALABLE: Map<Pair<RuleSet, Boolean>, Lazy<DealableHands>> = RuleSet.entries.flatMap { rules ->
    listOf(true, false).map { multiCardHands -> (rules to multiCardHands) to lazy { StrategyCharts.forRules(rules).dealableHands(multiCardHands) } }
}.toMap()

/**
 * Every hand the trainer can deal under [rules], or without [multiCardHands] only the ones a player reaches without hitting, so
 * a doubled hand only from two cards. Built on first use, and only for the rules and switch in play.
 */
internal fun dealableHands(rules: RuleSet, multiCardHands: Boolean = true): DealableHands = DEALABLE.getValue(rules to multiCardHands).value

/** The chart rows of every hand the trainer deals under [rules], read from the hands it deals, so the heatmap and the deal can't disagree. */
fun dealtRows(rules: RuleSet): Set<ChartRow> = dealableHands(rules).rows

private fun StrategyChart.dealableHands(multiCardHands: Boolean): DealableHands {
    val twoCard = LinkedHashMap<HandValues, MutableList<DealableHand>>()

    // The table once per two cards, since the upcard doesn't change it
    for (player in PLAYER_CARDS) {
        val table = chartRow(player).table

        for (upcard in Upcard.entries) {
            // Grading reads an upcard by its value, so any card of it stands for the rest
            val type = HandType(table, correctMove(player, SHOE_BY_VALUE.getValue(upcard).first()))
            twoCard.getOrPut(handValues(player, upcard, type.move)) { mutableListOf() } += DealableHand(player, upcard, type, waysToDeal(player, upcard))
        }
    }

    return DealableHands(
        buildMap {
            putAll(twoCard)
            twoCard.values.flatten().groupBy { it.upcard }.forEach { (upcard, twoCards) ->
                val multiCard = if (multiCardHands) hitting(upcard, twoCards.filter { it.type.move == Move.HIT }) else emptyMap()
                putAll(multiCard)
                putAll(doubling(upcard, (twoCards + multiCard.values.flatten()).filter { it.type.move == Move.DOUBLE }))
            }
        },
    )
}

/** The hands of 3 or more cards a player reaches against [upcard] by hitting where the chart says hit, from the two-card hands in [starts] the chart hits. */
private fun StrategyChart.hitting(upcard: Upcard, starts: List<Dealable>): Map<HandKey, List<Dealable>> {
    fun move(end: Reached) = play(end.total.row, upcard).move(cards = end.draws + 2)

    return Drawing(starts, doubles = false) { move(it) == Move.HIT }.hands { MultiCardHand(it.total.row, upcard, move(it)) }
}

/**
 * The doubled hands a player reaches against [upcard] by doubling where the chart says double, from the hands in [starts] it
 * doubles. Only a doubled hand the chart prints a square for is dealt, so without redoubling one on a row of Double Down Rescue.
 * None is redoubled: a rescue gives up the original bet however many doubles are on the hand, so the after-doubling tables,
 * worked out for one double, can't answer a hand with more at stake.
 */
private fun StrategyChart.doubling(upcard: Upcard, starts: List<Dealable>): Map<HandKey, List<Dealable>> =
    Drawing(starts, doubles = true) { false }.hands { end ->
        doubledRow(end.total)?.let { DoubledHand(end.total.afterDoublingRow, upcard, correctMoveAfterDoubling(end.total, upcard)) }
    }

/** A total a hand reaches, and how many cards were drawn to it after the hand it started from. */
private data class Reached(val total: HandTotal, val draws: Int)

/** A card drawn to reach a total: the total before it, and the card's value. */
private data class Draw(val from: HandTotal, val value: Upcard)

/**
 * The totals a player reaches drawing a card at a time from the hands in [starts], drawing again wherever [drawsAgain], each
 * card a hit or, with [doubles], a double. Only the starting cards and the upcard come out of the shoe. Each card drawn after
 * them is drawn as from a full shoe: the few cards gone by then shift its chances by a shade, and counting them out would take a
 * total for every set of cards drawn rather than one for every total.
 */
private class Drawing(starts: List<Dealable>, private val doubles: Boolean, drawsAgain: (Reached) -> Boolean) {
    private val startsByTotal: Map<HandTotal, Weighted<Dealable>> = starts.groupBy { it.total }.mapValues { (_, hands) -> Weighted(hands) { it.chance } }

    // Every total reached, and the draws reaching it, each weighed by how often a round comes that way
    private val drawsInto = HashMap<Reached, Weighted<Pair<Draw, Double>>>()

    init {
        var drawing: Map<HandTotal, Double> = startsByTotal.mapValues { it.value.total }
        var draws = 0

        while (drawing.isNotEmpty()) {
            draws++
            val reached = HashMap<HandTotal, MutableList<Pair<Draw, Double>>>()
            for ((total, chance) in drawing) {
                for ((value, drawChance) in DRAW_CHANCES) {
                    val drawn = total.plusCard(POINTS.getValue(value))
                    // A 21 or a bust leaves nothing to decide
                    if (drawn.value < 21) reached.getOrPut(drawn) { mutableListOf() } += Draw(total, value) to chance * drawChance
                }
            }

            for ((total, ways) in reached) drawsInto[Reached(total, draws)] = Weighted(ways) { it.second }
            drawing = reached.filterKeys { drawsAgain(Reached(it, draws)) }.mapValues { (_, ways) -> ways.sumOf { it.second } }
        }
    }

    /** Every total reached that [key] tells apart as a hand the trainer deals, with each way to reach it. */
    fun hands(key: (Reached) -> TotalHand?): Map<HandKey, List<Dealable>> {
        // In the order reached rather than by hash, which an enum's changes from run to run, so a seeded Random deals the same hands every run
        val hands = LinkedHashMap<HandKey, MutableList<Dealable>>()
        for ((end, ways) in drawsInto) {
            val hand = key(end) ?: continue
            hands.getOrPut(hand) { mutableListOf() } += End(end, hand.type, ways.total)
        }
        return hands
    }

    // A total reached with a count of cards drawn, dealt by tracing back from it a card at a time, as often as a round comes each way
    private inner class End(private val end: Reached, override val type: HandType, override val chance: Double) : Dealable {
        override val total: HandTotal get() = end.total

        override fun deal(random: Random): TrainerHand {
            var at = end
            val drawn = ArrayDeque<Upcard>()
            while (at.draws > 0) {
                val (draw, _) = drawsInto.getValue(at).pick(random)
                drawn.addFirst(draw.value)
                at = Reached(draw.from, at.draws - 1)
            }

            val start = startsByTotal.getValue(at.total).pick(random).deal(random)
            val hand = drawn.fold(start) { hand, value -> hand.copy(player = hand.player + drawCard(value, hand.player + hand.upcard, random)) }
            return if (doubles) hand.copy(doubled = true) else hand
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

/** The type [chart] grades the hand as: its kind of hand, a doubled hand's the table for doubled hands, and its correct move. */
internal fun TrainerHand.type(chart: StrategyChart) = HandType(row.table, chart.correctMove(this))
