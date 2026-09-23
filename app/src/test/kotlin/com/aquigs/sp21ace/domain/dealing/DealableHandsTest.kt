package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.doubledRow
import com.aquigs.sp21ace.domain.strategy.totalRow
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.correctMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DealableHandsTest {
    private val s17Hands = twoCardHands(RuleSet.S17)

    private fun twoCardHands(rules: RuleSet) = dealableHands(rules).hands.values.flatten().filterIsInstance<DealableHand>()

    private fun multiCardHands(rules: RuleSet) = dealableHands(rules).hands.filterKeys { it is MultiCardHand }

    private fun doubledHands(rules: RuleSet, multiCardHands: Boolean = true) =
        dealableHands(rules).let { if (multiCardHands) it.hands else it.withoutHits }.filterKeys { it is DoubledHand }

    private fun waysToDeal(player: String, upcard: Upcard) = s17Hands.single { it.player.toSet() == cards(player).toSet() && it.upcard == upcard }.ways

    // A way a round reaches a hand of 3 or more cards: the hand, its first two card values and how many cards it holds
    private data class Way(val hand: MultiCardHand, val start: List<Upcard>, val cards: Int)

    private val oneOfEachValue = Rank.entries.distinctBy { it.value }

    // A card of each value as a six-deck shoe of 288 draws it, the tens four ranks of 24
    private fun drawChance(rank: Rank) = (if (rank.value == 10) 72 else 24) / 288.0

    // Each two-card hand the chart plays with [moves], dealt against a club of its upcard's value, with how often a round deals it
    private fun starts(rules: RuleSet, vararg moves: Move): List<Pair<TrainerHand, Double>> {
        val upcards = oneOfEachValue.map { Card(it, Suit.CLUBS) }.associateBy { it.upcard }
        return twoCardHands(rules).filter { moves.isEmpty() || it.type.move in moves }.groupBy { handValues(it.player, it.upcard, it.type.move) }.values.map { hands ->
            TrainerHand(hands.first().player, upcards.getValue(hands.first().upcard)) to hands.sumOf { it.ways } / (288.0 * 287 * 286)
        }
    }

    // How often a round reaches each way, walking every card a player following the chart hits to, each drawn as from a full shoe
    private fun reached(rules: RuleSet): Map<Way, Double> {
        val chart = StrategyCharts.forRules(rules)
        val reached = HashMap<Way, Double>()

        fun hit(player: List<Card>, upcard: Card, chance: Double) {
            for (rank in oneOfEachValue) {
                val grown = player + Card(rank, Suit.CLUBS)
                if (grown.total().value >= 21) continue

                val grownChance = chance * drawChance(rank)
                val move = chart.correctMove(grown, upcard)
                val way = Way(MultiCardHand(totalRow(grown), upcard.upcard, move), player.take(2).map { it.upcard }.sorted(), grown.size)
                reached.merge(way, grownChance, Double::plus)
                if (move == Move.HIT) hit(grown, upcard, grownChance)
            }
        }

        for ((hand, chance) in starts(rules, Move.HIT)) hit(hand.player, hand.upcard, chance)
        return reached
    }

    // How often a round reaches each doubled hand, walking every card a player following the chart hits, while [hits], and
    // doubles to, each drawn as from a full shoe
    private fun reachedDoubled(rules: RuleSet, hits: Boolean): Map<HandKey, Double> {
        val chart = StrategyCharts.forRules(rules)
        val reached = HashMap<HandKey, Double>()

        fun play(hand: TrainerHand, chance: Double) {
            if (hand.doubled) {
                if (chart.doubledRow(hand.player.total()) != null) reached.merge(hand.key(chart), chance, Double::plus)
                return
            }

            val move = chart.correctMove(hand)
            if (move != Move.DOUBLE && !(hits && move == Move.HIT)) return
            for (rank in oneOfEachValue) {
                val grown = hand.copy(player = hand.player + Card(rank, Suit.CLUBS), doubled = move == Move.DOUBLE)
                if (grown.player.total().value < 21) play(grown, chance * drawChance(rank))
            }
        }

        for ((hand, chance) in starts(rules)) play(hand, chance)
        return reached
    }

    @Test
    fun doubledHandsComeUpAsOftenAsARoundReachesThemByDoublingWhereTheChartSaysDoubleAndWithoutHandsOf3OrMoreCardsOnlyFromTwo() {
        for (rules in RuleSet.entries) {
            for (hits in listOf(true, false)) {
                val expected = reachedDoubled(rules, hits)
                val dealt = doubledHands(rules, multiCardHands = hits).mapValues { (_, ways) -> ways.sumOf { it.chance } }

                assertEquals("$rules $hits", expected.keys, dealt.keys)
                expected.forEach { (hand, chance) -> assertEquals("$rules $hits $hand", chance, dealt.getValue(hand), chance * 1e-9) }
            }
        }
    }

    @Test
    fun everyWayToDealADoubledHandDoublesOnceWhereTheChartSaysFromWhatTheShoeHasLeft() {
        val random = Random(21)

        for (rules in RuleSet.entries) {
            val chart = StrategyCharts.forRules(rules)

            for ((hand, ways) in doubledHands(rules)) {
                for (way in ways) {
                    val dealt = way.deal(random)
                    val start = dealt.player.dropLast(1)
                    val hits = (2 until start.size).map { chart.correctMove(start.take(it), dealt.upcard) }

                    assertEquals("$rules $hand", hand, dealt.key(chart))
                    assertTrue("$rules $dealt", dealt.doubled)
                    assertEquals("$rules $dealt", List(start.size - 2) { Move.HIT }, hits)
                    assertEquals("$rules $dealt", Move.DOUBLE, chart.correctMove(start, dealt.upcard))
                    assertTrue("$rules $dealt", (dealt.player + dealt.upcard).groupingBy { it }.eachCount().values.all { it <= 6 })
                }
            }
        }
    }

    @Test
    fun theDealtRowsAreHard5To20SoftA2ToA9EveryPairAndTheDoubledRowsJustAsTheDealReachesThem() {
        val random = Random(21)
        val picker = HandPicker(RuleSet.S17, HandCustomization())
        val reached = List(5_000) { picker.pick(emptyList(), random).row }.toSet()
        val expected = (5..20).map { ChartRow(ChartTable.HARD, "$it") } +
            (2..9).map { ChartRow(ChartTable.SOFT, "A-$it") } +
            Upcard.entries.map { ChartRow(ChartTable.PAIRS, "${it.label}-${it.label}") }
        // Double Down Rescue's rows are hard 12 to 17. With redoubling a double of hard 5 draws to hard 7 at the least, and one of
        // soft 13 to soft 14.
        val rescueRows = (12..17).map { ChartRow(ChartTable.AFTER_DOUBLE_HARD, "$it") }
        val redoubleRows = (7..20).map { ChartRow(ChartTable.AFTER_DOUBLE_HARD, "$it") } + (3..9).map { ChartRow(ChartTable.AFTER_DOUBLE_SOFT, "A-$it") }

        assertEquals((expected + redoubleRows).toSet(), dealtRows(RuleSet.H17_REDOUBLE))
        assertEquals((expected + rescueRows).toSet(), dealtRows(RuleSet.H17))
        assertEquals((expected + rescueRows).toSet(), dealtRows(RuleSet.S17))
        assertEquals(dealtRows(RuleSet.S17), reached)
    }

    @Test
    fun everyTwoCardsButABlackjackAreListedAgainstEachUpcardValueAndGradedAsAgainstEveryCardOfIt() {
        val deck = spanishShoe(decks = 1)

        for (rules in RuleSet.entries) {
            val chart = StrategyCharts.forRules(rules)
            val listed = twoCardHands(rules)
            val misgraded = listed.flatMap { hand -> deck.filter { it.upcard == hand.upcard }.map { TrainerHand(hand.player, it) }.filter { it.type(chart) != hand.type } }

            // A deck's 48 × 49 / 2 two cards, the same card twice included, less 4 aces × 12 tens
            assertEquals("$rules", 1_128 * 10, listed.size)
            assertEquals("$rules", listed.size, listed.map { it.player.toSet() to it.upcard }.toSet().size)
            assertEquals("$rules", emptyList<TrainerHand>(), misgraded)
        }
    }

    @Test
    fun theWaysToDealCountEveryDealOfASixDeckShoeButABlackjack() {
        // The same card twice, against any 7 left: 6 × 5 × 22
        assertEquals(660, waysToDeal("7h 7h", Upcard.SEVEN))
        // Either card first: 6 × 6 × 70 × 2
        assertEquals(5_040, waysToDeal("Ks Qs", Upcard.TEN))
        assertEquals(1_728, waysToDeal("9c 7d", Upcard.ACE))

        // The first card, the upcard and the second card from 288, less 24 aces and 72 tens either way against any upcard left
        assertEquals(288L * 287 * 286 - 24L * 72 * 2 * 286, s17Hands.sumOf { it.ways.toLong() })
    }

    @Test
    fun handsOf3OrMoreCardsComeUpAsOftenAsARoundReachesThemByHittingWhereTheChartSaysHit() {
        for (rules in RuleSet.entries) {
            val expected = reached(rules).entries.groupingBy { it.key.hand }.fold(0.0) { chance, way -> chance + way.value }
            val dealt = multiCardHands(rules).mapValues { (_, ways) -> ways.sumOf { it.chance } }

            assertEquals("$rules", expected.keys, dealt.keys)
            expected.forEach { (hand, chance) -> assertEquals("$rules $hand", chance, dealt.getValue(hand), chance * 1e-9) }
        }
    }

    @Test
    fun aHandOf3OrMoreCardsComesUpEachWayThereAsOftenAsARoundDoes() {
        // Hard 16 vs 10 hits with 3 or more cards, which come from many first two cards
        val hand = MultiCardHand(ChartRow(ChartTable.HARD, "16"), Upcard.TEN, Move.HIT)
        val ways = reached(RuleSet.S17).filterKeys { it.hand == hand }
        val random = Random(21)
        val deal = Weighted(dealableHands(RuleSet.S17).hands.getValue(hand)) { it.chance }

        val dealt = List(20_000) { deal.pick(random).deal(random) }
        val counts = dealt.groupingBy { Way(hand, it.player.take(2).map { card -> card.upcard }.sorted(), it.player.size) }.eachCount()

        assertTrue(ways.keys.containsAll(counts.keys))
        ways.forEach { (way, chance) -> assertEquals("$way", chance / ways.values.sum(), (counts[way] ?: 0) / 20_000.0, 0.01) }
    }

    @Test
    fun everyWayToDealAHandOf3OrMoreCardsDealsCardsAPlayerHitsToByTheChartFromWhatTheShoeHasLeft() {
        val random = Random(21)

        for (rules in RuleSet.entries) {
            val chart = StrategyCharts.forRules(rules)

            for ((hand, ways) in multiCardHands(rules)) {
                for (way in ways) {
                    val dealt = way.deal(random)
                    val hits = (2 until dealt.player.size).map { chart.correctMove(dealt.player.take(it), dealt.upcard) }

                    assertEquals("$rules $hand", hand, dealt.key(chart))
                    assertEquals("$rules $dealt", List(dealt.player.size - 2) { Move.HIT }, hits)
                    assertTrue("$rules $dealt", (dealt.player + dealt.upcard).groupingBy { it }.eachCount().values.all { it <= 6 })
                }
            }
        }
    }
}
