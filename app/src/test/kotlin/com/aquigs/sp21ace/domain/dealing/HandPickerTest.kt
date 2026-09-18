package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.isBlackjack
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.dealTrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class HandPickerTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")

    // Hard 16 and 17 against an ace are the only hard hands to surrender when the dealer hits soft 17
    private val tenSixVsAce = TrainerHand(cards("Kc 6d"), card("As"))
    private val nineSevenVsAce = TrainerHand(cards("9c 7d"), card("Ah"))
    private val tenSevenVsAce = TrainerHand(cards("Qh 7s"), card("Ad"))
    private val nineEightVsAce = TrainerHand(cards("9d 8c"), card("Ac"))

    private val deck = spanishShoe(decks = 1)

    // Every two-card hand from one deck, the same card twice included so suited pairs are there, against an upcard of each value
    private val everyHand = deck.flatMap { first -> deck.map { listOf(first, it) } }
        .filterNot { it.isBlackjack() }
        .flatMap { player -> deck.filter { it.suit == Suit.SPADES }.distinctBy { it.upcard }.map { TrainerHand(player, it) } }

    private fun onlyOn(type: HandType, handsDealt: HandsDealt = HandsDealt.RANDOM) = HandCustomization(handsDealt, switchedOff = HAND_TYPES.toSet() - type)

    private fun HandPicker.deal(times: Int, seed: Int = 21): List<TrainerHand> {
        val random = Random(seed)
        return List(times) { pick(random) }
    }

    private fun answers(hand: TrainerHand, right: Int = 0, wrong: Int = 0, at: Instant = now, rules: RuleSet = RuleSet.H17) =
        List(right) { PracticeAnswer(at, rules, hand, Move.SURRENDER, Move.SURRENDER) } + List(wrong) { PracticeAnswer(at, rules, hand, Move.HIT, Move.SURRENDER) }

    private fun List<TrainerHand>.shares(of: (TrainerHand) -> Any): Map<Any, Double> = groupingBy(of).eachCount().mapValues { it.value.toDouble() / size }

    @Test
    fun aHandIsItsTwoCardValuesInEitherOrderAgainstTheUpcardsWhateverTheSuits() {
        assertEquals(HandValues(Upcard.SEVEN, Upcard.NINE, Upcard.ACE), nineSevenVsAce.values)
        assertEquals(nineSevenVsAce.values, TrainerHand(cards("7h 9s"), card("Ad")).values)
        assertEquals(tenSixVsAce.values, TrainerHand(cards("6h Jd"), card("Ac")).values)
    }

    @Test
    fun everyAnswerEverGivenCountsForItsHandWhateverItsAgeAndTheRulesThatGradedIt() {
        val history = answers(nineSevenVsAce, right = 1, at = Instant.EPOCH) +
            answers(TrainerHand(cards("7h 9s"), card("Ad")), wrong = 1, rules = RuleSet.S17) +
            answers(tenSixVsAce, wrong = 1) +
            // Three cards make no two-card hand
            answers(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = 1)

        assertEquals(mapOf(nineSevenVsAce.values to Tally(correct = 1, incorrect = 1), tenSixVsAce.values to Tally(correct = 0, incorrect = 1)), history.tallyByHand())
    }

    @Test
    fun aHandWeighsTheInverseOfItsAccuracyWithNoAnswersAt50PercentAndAFloorAt5Percent() {
        assertEquals(ACCURACY_FLOOR, 0.05, 0.0)
        assertEquals(2.0, weight(null), 1e-9)
        assertEquals(1.0, weight(Tally(correct = 3, incorrect = 0)), 1e-9)
        assertEquals(2.0, weight(Tally(correct = 1, incorrect = 1)), 1e-9)
        assertEquals(4.0, weight(Tally(correct = 1, incorrect = 3)), 1e-9)
        assertEquals(10.0, weight(Tally(correct = 1, incorrect = 9)), 1e-9)
        assertEquals(20.0, weight(Tally(correct = 1, incorrect = 29)), 1e-9)
        assertEquals(20.0, weight(Tally(correct = 0, incorrect = 3)), 1e-9)
    }

    @Test
    fun prioritizingWorseHandsDealsEachHandInInverseProportionToItsAccuracy() {
        val history = answers(tenSixVsAce, right = 1, wrong = 3) + answers(nineSevenVsAce, right = 2) + answers(nineEightVsAce, wrong = 2)
        val picker = HandPicker(RuleSet.H17, onlyOn(HandType(ChartTable.HARD, Move.SURRENDER), HandsDealt.PRIORITIZE_WORSE), history)

        val counts = picker.deal(27_000).groupingBy { it.values }.eachCount()

        // Weights 4, 1, 2 with no answers, and 20 at the floor, out of 27
        val expected = mapOf(tenSixVsAce.values to 4_000, nineSevenVsAce.values to 1_000, tenSevenVsAce.values to 2_000, nineEightVsAce.values to 20_000)
        assertEquals(expected.keys, counts.keys)
        expected.forEach { (hand, count) -> assertEquals("$hand", count.toDouble(), counts.getValue(hand).toDouble(), count * 0.1) }
    }

    @Test
    fun prioritizingWorseHandsDealsEveryHandAlikeUntilThereAreAnswers() {
        val hands = HandPicker(RuleSet.S17, HandCustomization(HandsDealt.PRIORITIZE_WORSE), emptyList()).deal(20_000)

        // 54 two-card hands, 10 of them pairs and 8 soft, against 10 upcard values
        val kinds = hands.shares { chartRow(it.player).table }
        assertEquals(10.0 / 54, kinds.getValue(ChartTable.PAIRS), 0.015)
        assertEquals(8.0 / 54, kinds.getValue(ChartTable.SOFT), 0.015)
        assertEquals(0.1, hands.shares { it.upcard.upcard }.getValue(Upcard.TEN), 0.015)
    }

    @Test
    fun randomDealsHandsAsOftenAsAShuffledShoe() {
        val random = Random(7)
        val shoe = List(20_000) { dealTrainerHand(random) }
        val picked = HandPicker(RuleSet.S17, HandCustomization(), emptyList()).deal(20_000)

        for (share in listOf<(TrainerHand) -> Any>({ chartRow(it.player).table }, { it.upcard.upcard })) {
            val expected = shoe.shares(share)
            val actual = picked.shares(share)

            assertEquals(expected.keys, actual.keys)
            expected.forEach { (key, value) -> assertEquals("$key", value, actual.getValue(key), 0.015) }
        }
    }

    @Test
    fun eachSwitchOnItsOwnDealsOnlyAndEveryHandThatCallsForItUnderEveryRuleSet() {
        val impossible = mutableListOf<Pair<RuleSet, HandType>>()

        for (rules in RuleSet.entries) {
            val chart = StrategyCharts.forRules(rules)
            val calling = everyHand.groupBy { it.type(chart) }

            for (type in HAND_TYPES) {
                val hands = calling[type]
                if (hands == null) {
                    impossible += rules to type
                    continue
                }

                val picker = HandPicker(rules, onlyOn(type), emptyList())
                assertEquals("$rules $type", hands.map { it.values }.toSet(), picker.hands.toSet())
                assertTrue("$rules $type", picker.deal(200).all { it.type(chart) == type })
            }
        }

        // The only pair to surrender is 8-8 against an ace, which splits when the dealer stands on soft 17, so that's the one type a rule set rules out
        assertEquals(listOf(RuleSet.S17 to HandType(ChartTable.PAIRS, Move.SURRENDER)), impossible)
    }

    @Test
    fun switchingOffAMoveStillDealsTheHandsWhoseSuitsCallForAnother() {
        // Hard 14 vs 6 is S6" when the dealer hits soft 17, so a 6-8 of spades hits while any other 6-8 stands
        val picker = HandPicker(RuleSet.H17, HandCustomization(switchedOff = setOf(HandType(ChartTable.HARD, Move.STAND))), emptyList())
        val sixEightVsSix = HandValues(Upcard.SIX, Upcard.EIGHT, Upcard.SIX)

        assertTrue(sixEightVsSix in picker.hands)
        assertTrue(picker.deal(2_000).filter { it.values == sixEightVsSix }.all { hand -> hand.player.all { it.suit == Suit.SPADES } })
    }

    @Test
    fun withNoSwitchedOnTypePossibleUnderTheRulesItDealsEveryType() {
        val every = HandPicker(RuleSet.S17, HandCustomization(), emptyList()).deal(500)

        assertEquals(every, HandPicker(RuleSet.S17, onlyOn(HandType(ChartTable.PAIRS, Move.SURRENDER)), emptyList()).deal(500))
        assertEquals(every, HandPicker(RuleSet.S17, HandCustomization(switchedOff = HAND_TYPES.toSet()), emptyList()).deal(500))
        assertEquals(setOf(ChartTable.HARD, ChartTable.SOFT, ChartTable.PAIRS), every.map { chartRow(it.player).table }.toSet())
    }
}
