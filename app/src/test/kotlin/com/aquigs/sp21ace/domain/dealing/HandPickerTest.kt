package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.cards.total
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.countsCards
import com.aquigs.sp21ace.domain.strategy.doubledRow
import com.aquigs.sp21ace.domain.strategy.play
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.correctMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class HandPickerTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")
    private val s17 = StrategyCharts.forRules(RuleSet.S17)
    private val h17 = StrategyCharts.forRules(RuleSet.H17)

    // Hard 16 and 17 against an ace are the only hard hands to surrender when the dealer hits soft 17
    private val tenSixVsAce = TrainerHand(cards("Kc 6d"), card("As"))
    private val nineSevenVsAce = TrainerHand(cards("9c 7d"), card("Ah"))
    private val tenSevenVsAce = TrainerHand(cards("Qh 7s"), card("Ad"))
    private val nineEightVsAce = TrainerHand(cards("9d 8c"), card("Ac"))

    private val pairsSurrender = HandType(ChartTable.PAIRS, Move.SURRENDER)

    private fun onlyOn(type: HandType, handsDealt: HandsDealt = HandsDealt.RANDOM) = HandCustomization(handsDealt, switchedOff = HAND_TYPES.toSet() - type)

    private fun HandPicker.deal(times: Int, history: List<PracticeAnswer> = emptyList()): List<TrainerHand> {
        val random = Random(21)
        return List(times) { pick(history, random) }
    }

    private fun answers(hand: TrainerHand, right: Int = 0, wrong: Int = 0) =
        List(right) { PracticeAnswer(now, RuleSet.H17, hand, Move.SURRENDER, Move.SURRENDER) } + List(wrong) { PracticeAnswer(now, RuleSet.H17, hand, Move.HIT, Move.SURRENDER) }

    private fun List<TrainerHand>.shares(of: (TrainerHand) -> Any): Map<Any, Double> = groupingBy(of).eachCount().mapValues { it.value.toDouble() / size }

    // Every hand a round asks about, from the first two cards on for as long as the chart says hit, and once it says double the
    // doubled hand, wherever the chart prints a square for it
    private fun decisions(shuffled: List<Card>, chart: StrategyChart): List<TrainerHand> {
        val (first, upcard, second) = shuffled
        val asked = mutableListOf<TrainerHand>()
        var hand = TrainerHand(listOf(first, second), upcard)

        while (hand.player.total().value < 21) {
            if (!hand.doubled || chart.doubledRow(hand.player.total()) != null) asked += hand
            val drawn = hand.player + shuffled[hand.player.size + 1]
            hand = when (chart.correctMove(hand)) {
                Move.HIT -> hand.copy(player = drawn)
                Move.DOUBLE -> hand.copy(player = drawn, doubled = true)
                else -> break
            }
        }

        return asked
    }

    // Hands dealt when the dealer stands on soft 17, with how often a table deals each
    private fun tableShares(of: (HandKey) -> Boolean): Map<HandKey, Double> {
        val hands = dealableHands(RuleSet.S17).hands
        val all = hands.values.sumOf { ways -> ways.sumOf { it.chance } }

        return hands.filterKeys(of).mapValues { (_, ways) -> ways.sumOf { it.chance } / all }
    }

    private val cardCountHands: Map<HandKey, Double> by lazy { tableShares { it is MultiCardHand && s17.play(it.row, it.upcard).countsCards } }

    // Hard 14 vs 4 to 6 and 15 vs 2 and 3 carry *, so any 6-8 or 7-8 hits, 15 vs 4 and 6 carry ", so only a spaded 7-8 hits and
    // the rest stand, and 7-7 vs 7 carries $, so suited 7s hit and the rest split
    private val bonusHands: Map<HandKey, Double> by lazy {
        val sixEights = listOf(Upcard.FOUR, Upcard.FIVE, Upcard.SIX).map { HandValues(Upcard.SIX, Upcard.EIGHT, it, Move.HIT) }
        val sevenEights = listOf(Upcard.TWO, Upcard.THREE, Upcard.FOUR, Upcard.SIX).map { HandValues(Upcard.SEVEN, Upcard.EIGHT, it, Move.HIT) } +
            listOf(Upcard.FOUR, Upcard.SIX).map { HandValues(Upcard.SEVEN, Upcard.EIGHT, it, Move.STAND) }
        val sevens = listOf(Move.HIT, Move.SPLIT).map { HandValues(Upcard.SEVEN, Upcard.SEVEN, Upcard.SEVEN, it) }
        val expected = (sixEights + sevenEights + sevens).toSet()

        tableShares { it in expected }.also { assertEquals(expected, it.keys) }
    }

    private fun dealt(customization: HandCustomization): Map<HandKey?, Int> = HandPicker(RuleSet.S17, customization).deal(80_000).groupingBy { it.key(s17) }.eachCount()

    // Each of [hands] comes up [share] of the deals shared evenly, on top of its share at a table of the [rest]
    private fun assertDealtMoreOften(dealt: Map<HandKey?, Int>, hands: Map<HandKey, Double>, share: Double, rest: Double) {
        val deals = dealt.values.sum()
        for ((hand, tableShare) in hands) {
            val expected = deals * (share / hands.size + rest * tableShare)
            assertEquals("$hand", expected, (dealt[hand] ?: 0).toDouble(), expected * 0.25)
        }

        // Each hand's count is too noisy to pin the share, so their total pins it
        val expectedTotal = deals * (share + rest * hands.values.sum())
        assertEquals("$share", expectedTotal, hands.keys.sumOf { dealt[it] ?: 0 }.toDouble(), expectedTotal * 0.03)
    }

    @Test
    fun randomDealsHandsAsOftenAsAShuffledShoeDealsThemToAPlayerFollowingTheChart() {
        val random = Random(7)
        val shoe = spanishShoe(decks = 6)
        val shuffled = generateSequence { shoe.shuffled(random) }.flatMap { decisions(it, s17) }.take(20_000).toList()
        // Card-count and bonus hands on their own switches come up more often than a shoe deals them
        val picked = HandPicker(RuleSet.S17, HandCustomization(cardCountHands = false, bonusHands = false)).deal(20_000)

        val measures = listOf<(TrainerHand) -> Any>(
            { it.row.table },
            { it.upcard.upcard },
            { it.upcard.rank },
            { hand -> hand.player.map { it.suit }.toSet().size },
            { minOf(it.player.size, 5) },
            { it.doubled },
            { s17.correctMove(it) },
        )
        for (measure in measures) {
            val expected = shuffled.shares(measure)
            val actual = picked.shares(measure)

            assertEquals(expected.keys, actual.keys)
            expected.forEach { (key, share) -> assertEquals("$key", share, actual.getValue(key), 0.015) }
        }
    }

    @Test
    fun handsOf3OrMoreCardsNeverComeUpWithTheirSwitchOffButForDoublesFromTwoCards() {
        // The card-count switch, on by default, has none left to deal either
        val picker = HandPicker(RuleSet.H17_REDOUBLE, HandCustomization(multiCardHands = false))
        val dealt = picker.deal(5_000)

        assertTrue(dealt.all { it.player.size == 2 || it.doubled && it.player.size == 3 })
        assertTrue(dealt.any { it.doubled })
        assertTrue(picker.hands.all { it is HandValues || it is DoubledHand })
    }

    @Test
    fun withTheirSwitchOnCardCountHandsTakeOneDealInFourEachAsOftenAsTheNextOnTopOfTheirShareAtATable() {
        // D5 makes a 5-card hard 11 vs 5 a hit, which a shoe deals far too seldom to learn
        assertTrue(MultiCardHand(ChartRow(ChartTable.HARD, "11"), Upcard.FIVE, Move.HIT) in cardCountHands)
        assertDealtMoreOften(dealt(HandCustomization(bonusHands = false)), cardCountHands, share = 0.25, rest = 0.75)
    }

    @Test
    fun withTheirSwitchOnBonusHandsTakeOneDealInTenEachAsOftenAsTheNextOnTopOfTheirShareAtATable() {
        // Among them a spaded 7-8 vs 4, which a shoe deals about 1 hand in 20,000
        assertDealtMoreOften(dealt(HandCustomization(cardCountHands = false)), bonusHands, share = 0.1, rest = 0.9)
    }

    @Test
    fun withBothSwitchesOnEachTakesItsOwnShareOfTheDeals() {
        val dealt = dealt(HandCustomization())

        assertDealtMoreOften(dealt, cardCountHands, share = 0.25, rest = 1 - 0.25 - 0.1)
        assertDealtMoreOften(dealt, bonusHands, share = 0.1, rest = 1 - 0.25 - 0.1)
    }

    @Test
    fun theOtherSuitsOfABonusHandComeOnlyBesideTheSuitsThatHitForTheBonus() {
        // With only splits dealt, 7-7 vs 7 in other suits is just another pair to split, so the bonus switch has nothing to deal
        val splits = onlyOn(HandType(ChartTable.PAIRS, Move.SPLIT))

        assertEquals(HandPicker(RuleSet.S17, splits.copy(bonusHands = false)).deal(2_000), HandPicker(RuleSet.S17, splits).deal(2_000))
    }

    @Test
    fun underPrioritizeWorseHandsTheCardCountDealsFavorTheCardCountHandsMissedMost() {
        val fiveCardElevenVsFive = TrainerHand(cards("2c 3d 2h 2s 2d"), card("5s"))
        val history = List(3) { PracticeAnswer(now, RuleSet.S17, fiveCardElevenVsFive, Move.DOUBLE, Move.HIT) }
        val picker = HandPicker(RuleSet.S17, HandCustomization(HandsDealt.PRIORITIZE_WORSE, bonusHands = false))

        val dealt = picker.deal(20_000, history).count { it.key(s17) == fiveCardElevenVsFive.key(s17) }

        // Missed every time, it weighs 20 against 2 for each hand not yet answered, among the card-count hands and among them all
        val share = 0.25 * 20 / (20 + 2.0 * (cardCountHands.size - 1)) + 0.75 * 20 / (20 + 2.0 * (picker.hands.size - 1))
        assertEquals(share, dealt / 20_000.0, share * 0.15)
    }

    @Test
    fun prioritizingWorseHandsDealsEachHandInInverseProportionToItsAccuracy() {
        val history = answers(tenSixVsAce, right = 1, wrong = 3) + answers(nineSevenVsAce, right = 2) + answers(nineEightVsAce, wrong = 2)
        val picker = HandPicker(RuleSet.H17, onlyOn(HandType(ChartTable.HARD, Move.SURRENDER), HandsDealt.PRIORITIZE_WORSE))

        val counts = picker.deal(27_000, history).groupingBy { it.key(h17) }.eachCount()

        // Weights 4, 1, 2 with no answers, and 20 at the floor, out of 27
        val expected = mapOf(tenSixVsAce.key(h17) to 4_000, nineSevenVsAce.key(h17) to 1_000, tenSevenVsAce.key(h17) to 2_000, nineEightVsAce.key(h17) to 20_000)
        assertEquals(expected.keys, counts.keys)
        expected.forEach { (hand, count) -> assertEquals("$hand", count.toDouble(), counts.getValue(hand).toDouble(), count * 0.1) }
    }

    @Test
    fun prioritizingWorseHandsDealsEveryHandAlikeUntilThereAreAnswers() {
        val picker = HandPicker(RuleSet.S17, HandCustomization(HandsDealt.PRIORITIZE_WORSE, cardCountHands = false, bonusHands = false))
        val dealt = picker.deal(20_000)
        val all = picker.hands.size.toDouble()
        val multiCard = picker.hands.count { it is MultiCardHand }
        val doubled = picker.hands.count { it is DoubledHand }

        // 54 two-card hands, 10 of them pairs and 8 soft, against 10 upcard values, then the suited 7-7 vs 7 and spaded 7-8 vs 4 and
        // vs 6, whose bonuses make them hands of their own, and every hand of 3 or more cards and every doubled hand
        assertEquals(543, picker.hands.count { it is HandValues })
        // Doubled hard 12 to 17, the rows of Double Down Rescue, against 10 upcard values
        assertEquals(60, doubled)
        val kinds = dealt.shares { if (it.doubled) "doubled" else if (it.player.size > 2) "3 or more cards" else chartRow(it.player).table }
        assertEquals(101 / all, kinds.getValue(ChartTable.PAIRS), 0.015)
        assertEquals(80 / all, kinds.getValue(ChartTable.SOFT), 0.015)
        assertEquals(multiCard / all, kinds.getValue("3 or more cards"), 0.015)
        assertEquals(doubled / all, kinds.getValue("doubled"), 0.015)
        assertEquals(picker.hands.count { it.upcard == Upcard.TEN } / all, dealt.shares { it.upcard.upcard }.getValue(Upcard.TEN), 0.015)
    }

    @Test
    fun aHandOf3OrMoreCardsWeighsTheAnswersToItsTotalAndMoveWhateverTheCards() {
        // Hard 14 vs 4 is S4* when the dealer stands on soft 17, so with 3 cards it stands
        val fiveFourFiveVsFour = TrainerHand(cards("5c 4d 5h"), card("4s"))
        val fourteenVsFour = MultiCardHand(ChartRow(ChartTable.HARD, "14"), Upcard.FOUR, Move.STAND)
        val history = List(3) { PracticeAnswer(now, RuleSet.S17, fiveFourFiveVsFour, Move.HIT, Move.STAND) }
        // A card count decides hard 14 vs 4, so with the card-count and bonus switches off it comes up by its weight alone
        val picker = HandPicker(RuleSet.S17, onlyOn(HandType(ChartTable.HARD, Move.STAND), HandsDealt.PRIORITIZE_WORSE).copy(cardCountHands = false, bonusHands = false))

        val dealt = picker.deal(20_000, history)

        // Missed every time, it weighs 20 against 2 for each hand not yet answered, whichever cards make it
        val share = 20 / (20 + 2.0 * (picker.hands.size - 1))
        assertTrue(fourteenVsFour in picker.hands)
        assertEquals(share, dealt.count { it.key(s17) == fourteenVsFour } / 20_000.0, share * 0.15)
        assertTrue(dealt.filter { it.key(s17) == fourteenVsFour }.map { it.player }.toSet().size > 1)
    }

    @Test
    fun theSuitsThatHitForABonusAreAHandOfTheirOwnApartFromTheSameCardsInOtherSuits() {
        // Hard 14 vs 6 is S6" when the dealer hits soft 17, so a 6-8 of spades hits while any other 6-8 stands
        val spaded = HandValues(Upcard.SIX, Upcard.EIGHT, Upcard.SIX, Move.HIT)
        val picker = HandPicker(RuleSet.H17, HandCustomization(HandsDealt.PRIORITIZE_WORSE))

        assertTrue(spaded in picker.hands && spaded.copy(move = Move.STAND) in picker.hands)
        // So missing a spaded one weighs against the spaded ones alone
        assertEquals(setOf(spaded), answers(TrainerHand(cards("6s 8s"), card("6h")), wrong = 1).tallyByHand(h17).keys)
    }

    @Test
    fun eachSwitchOnItsOwnDealsOnlyAndEveryHandThatCallsForItUnderEveryRuleSet() {
        val deck = spanishShoe(decks = 1)
        // Every two cards of a deck but a blackjack, the same card twice included so suited pairs are there, against a card of each upcard value
        val everyHand = deck.flatMap { first -> deck.map { listOf(first, it) } }
            .filterNot { it.isBlackjack() }
            .flatMap { player -> deck.distinctBy { it.upcard }.map { TrainerHand(player, it) } }

        for (rules in RuleSet.entries) {
            val chart = StrategyCharts.forRules(rules)
            val calling = everyHand.groupBy { it.type(chart) }
            val doubledTypes = dealableHands(rules).hands.keys.filterIsInstance<DoubledHand>().mapTo(HashSet()) { it.type }

            // The only pair to surrender is 8-8 against an ace, which splits when the dealer stands on soft 17, and without
            // redoubling no doubled hand redoubles and none is soft, since Double Down Rescue prints only hard rows
            val withoutRedoubling = setOf(
                HandType(ChartTable.AFTER_DOUBLE_HARD, Move.REDOUBLE),
                HandType(ChartTable.AFTER_DOUBLE_SOFT, Move.REDOUBLE),
                HandType(ChartTable.AFTER_DOUBLE_SOFT, Move.STAND),
            )
            val impossible = when (rules) {
                RuleSet.H17_REDOUBLE -> emptySet()
                RuleSet.H17 -> withoutRedoubling
                RuleSet.S17 -> withoutRedoubling + pairsSurrender
            }
            assertEquals("$rules", HAND_TYPES.toSet() - impossible, calling.keys + doubledTypes)

            for (type in calling.keys + doubledTypes) {
                val picker = HandPicker(rules, onlyOn(type))

                assertEquals("$rules $type", calling[type].orEmpty().mapTo(HashSet()) { it.key(chart) }, picker.hands.filterIsInstance<HandValues>().toSet())
                assertTrue("$rules $type", picker.hands.filterIsInstance<TotalHand>().all { it.type == type })
                assertEquals("$rules $type", List(200) { type }, picker.deal(200).map { it.type(chart) })
            }
        }
    }

    @Test
    fun aSwitchReadsTheMoveEachHandIsGradedWithBonusExceptionsIncluded() {
        // Under the default rules hard 14 vs 4 is S4*, so any 6-8 hits while a 5-9 stands
        val picker = HandPicker(TableRules().ruleSet, onlyOn(HandType(ChartTable.HARD, Move.STAND)))
        val sixEightVsFour = HandValues(Upcard.SIX, Upcard.EIGHT, Upcard.FOUR, Move.HIT)

        assertTrue(HandValues(Upcard.FIVE, Upcard.NINE, Upcard.FOUR, Move.STAND) in picker.hands)
        assertTrue(sixEightVsFour !in picker.hands)
        assertTrue(picker.deal(2_000).none { it.player.size == 2 && it.key(s17) == sixEightVsFour })
    }

    @Test
    fun itDealsEveryTypeExactlyWhenNoTypeSwitchedOnCanComeUpUnderTheRules() {
        val every = HandPicker(RuleSet.S17, HandCustomization()).deal(500)
        val eightsVsAce = HandValues(Upcard.EIGHT, Upcard.EIGHT, Upcard.ACE, Move.SURRENDER)

        // No pair surrenders when the dealer stands on soft 17, while 8-8 against an ace does when the dealer hits
        assertEquals(every, HandPicker(RuleSet.S17, onlyOn(pairsSurrender)).deal(500))
        assertEquals(every, HandPicker(RuleSet.S17, HandCustomization(switchedOff = HAND_TYPES.toSet())).deal(500))
        assertEquals(List(500) { eightsVsAce }, HandPicker(RuleSet.H17, onlyOn(pairsSurrender)).deal(500).map { it.key(h17) })
        assertEquals(setOf(ChartTable.HARD, ChartTable.SOFT, ChartTable.PAIRS), every.map { chartRow(it.player).table }.toSet())
    }
}
