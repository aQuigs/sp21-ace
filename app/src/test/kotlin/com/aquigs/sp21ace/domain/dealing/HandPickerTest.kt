package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Suit
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
import com.aquigs.sp21ace.domain.strategy.correctMove
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class HandPickerTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")
    private val s17 = StrategyCharts.forRules(RuleSet.S17)

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

    // Every hand a round asks about, from the first two cards on for as long as the chart says hit
    private fun decisions(shuffled: List<Card>, chart: StrategyChart): List<TrainerHand> {
        val (first, upcard, second) = shuffled
        val asked = mutableListOf<TrainerHand>()
        var player = listOf(first, second)

        while (player.total().value < 21) {
            asked += TrainerHand(player, upcard)
            if (chart.correctMove(player, upcard) != Move.HIT) break
            player = player + shuffled[player.size + 1]
        }

        return asked
    }

    @Test
    fun randomDealsHandsAsOftenAsAShuffledShoeDealsThemToAPlayerFollowingTheChart() {
        val random = Random(7)
        val shoe = spanishShoe(decks = 6)
        val shuffled = generateSequence { shoe.shuffled(random) }.flatMap { decisions(it, s17) }.take(20_000).toList()
        val picked = HandPicker(RuleSet.S17, HandCustomization()).deal(20_000)

        val measures = listOf<(TrainerHand) -> Any>(
            { chartRow(it.player).table },
            { it.upcard.upcard },
            { it.upcard.rank },
            { hand -> hand.player.map { it.suit }.toSet().size },
            { minOf(it.player.size, 5) },
            { s17.correctMove(it.player, it.upcard) },
        )
        for (measure in measures) {
            val expected = shuffled.shares(measure)
            val actual = picked.shares(measure)

            assertEquals(expected.keys, actual.keys)
            expected.forEach { (key, share) -> assertEquals("$key", share, actual.getValue(key), 0.015) }
        }
    }

    @Test
    fun handsOf3OrMoreCardsNeverComeUpWithTheirSwitchOff() {
        val picker = HandPicker(RuleSet.S17, HandCustomization(multiCardHands = false))

        assertTrue(picker.deal(5_000).all { it.player.size == 2 })
        assertTrue(picker.hands.all { it is HandValues })
    }

    @Test
    fun prioritizingWorseHandsDealsEachHandInInverseProportionToItsAccuracy() {
        val history = answers(tenSixVsAce, right = 1, wrong = 3) + answers(nineSevenVsAce, right = 2) + answers(nineEightVsAce, wrong = 2)
        val picker = HandPicker(RuleSet.H17, onlyOn(HandType(ChartTable.HARD, Move.SURRENDER), HandsDealt.PRIORITIZE_WORSE))

        val counts = picker.deal(27_000, history).groupingBy { it.values }.eachCount()

        // Weights 4, 1, 2 with no answers, and 20 at the floor, out of 27
        val expected = mapOf(tenSixVsAce.values to 4_000, nineSevenVsAce.values to 1_000, tenSevenVsAce.values to 2_000, nineEightVsAce.values to 20_000)
        assertEquals(expected.keys, counts.keys)
        expected.forEach { (hand, count) -> assertEquals("$hand", count.toDouble(), counts.getValue(hand).toDouble(), count * 0.1) }
    }

    @Test
    fun prioritizingWorseHandsDealsEveryHandAlikeUntilThereAreAnswers() {
        val picker = HandPicker(RuleSet.S17, HandCustomization(HandsDealt.PRIORITIZE_WORSE))
        val dealt = picker.deal(20_000)
        val all = picker.hands.size.toDouble()
        val multiCard = picker.hands.count { it is MultiCardHand }

        // 54 two-card hands, 10 of them pairs and 8 soft, against 10 upcard values, then every hand of 3 or more cards
        assertEquals(540, picker.hands.size - multiCard)
        val kinds = dealt.shares { if (it.player.size > 2) "3 or more cards" else chartRow(it.player).table }
        assertEquals(100 / all, kinds.getValue(ChartTable.PAIRS), 0.015)
        assertEquals(80 / all, kinds.getValue(ChartTable.SOFT), 0.015)
        assertEquals(multiCard / all, kinds.getValue("3 or more cards"), 0.015)
        assertEquals(picker.hands.count { it.upcard == Upcard.TEN } / all, dealt.shares { it.upcard.upcard }.getValue(Upcard.TEN), 0.015)
    }

    @Test
    fun aHandOf3OrMoreCardsWeighsTheAnswersToItsTotalAndMoveWhateverTheCards() {
        // Hard 14 vs 4 is S4* when the dealer stands on soft 17, so with 3 cards it stands
        val fiveFourFiveVsFour = TrainerHand(cards("5c 4d 5h"), card("4s"))
        val fourteenVsFour = MultiCardHand(ChartRow(ChartTable.HARD, "14"), Upcard.FOUR, Move.STAND)
        val history = List(3) { PracticeAnswer(now, RuleSet.S17, fiveFourFiveVsFour, Move.HIT, Move.STAND) }
        val picker = HandPicker(RuleSet.S17, onlyOn(HandType(ChartTable.HARD, Move.STAND), HandsDealt.PRIORITIZE_WORSE))

        val dealt = picker.deal(20_000, history)

        // Missed every time, it weighs 20 against 2 for each hand not yet answered, whichever cards make it
        val share = 20 / (20 + 2.0 * (picker.hands.size - 1))
        assertTrue(fourteenVsFour in picker.hands)
        assertEquals(share, dealt.count { it.key(s17) == fourteenVsFour } / 20_000.0, share * 0.15)
        assertTrue(dealt.filter { it.key(s17) == fourteenVsFour }.map { it.player }.toSet().size > 1)
    }

    @Test
    fun aHandWhoseSuitsDecideItsMoveKeepsItsWholeWeightOnTheSuitsStillDealt() {
        // Hard 14 vs 6 is S6" when the dealer hits soft 17, so a 6-8 of spades hits while any other 6-8 stands
        val sixEightVsSix = TrainerHand(cards("6s 8s"), card("6h"))
        val history = answers(sixEightVsSix, wrong = 1) + answers(nineEightVsAce, wrong = 1)
        val picker = HandPicker(RuleSet.H17, HandCustomization(HandsDealt.PRIORITIZE_WORSE, switchedOff = setOf(HandType(ChartTable.HARD, Move.STAND))))

        val dealt = picker.deal(20_000, history)
        val sixEights = dealt.filter { it.player.size == 2 && it.values == sixEightVsSix.values }
        val nineEights = dealt.count { it.player.size == 2 && it.values == nineEightVsAce.values }

        assertTrue(sixEights.isNotEmpty())
        assertTrue(sixEights.all { hand -> hand.player.all { it.suit == Suit.SPADES } })
        // Both missed, so both weigh 20, however few of a hand's suits the switches leave
        assertEquals(nineEights.toDouble(), sixEights.size.toDouble(), nineEights * 0.2)
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

            // The only pair to surrender is 8-8 against an ace, which splits when the dealer stands on soft 17
            val impossible = if (rules == RuleSet.S17) setOf(pairsSurrender) else emptySet()
            assertEquals("$rules", HAND_TYPES.toSet() - impossible, calling.keys)

            for ((type, hands) in calling) {
                val picker = HandPicker(rules, onlyOn(type))

                assertEquals("$rules $type", hands.mapTo(HashSet()) { it.values }, picker.hands.filterIsInstance<HandValues>().toSet())
                assertTrue("$rules $type", picker.hands.filterIsInstance<MultiCardHand>().all { it.type == type })
                assertEquals("$rules $type", List(200) { type }, picker.deal(200).map { it.type(chart) })
            }
        }
    }

    @Test
    fun aSwitchReadsTheMoveEachHandIsGradedWithBonusExceptionsIncluded() {
        // Under the default rules hard 14 vs 4 is S4*, so any 6-8 hits while a 5-9 stands
        val picker = HandPicker(TableRules().ruleSet, onlyOn(HandType(ChartTable.HARD, Move.STAND)))
        val sixEightVsFour = HandValues(Upcard.SIX, Upcard.EIGHT, Upcard.FOUR)

        assertTrue(HandValues(Upcard.FIVE, Upcard.NINE, Upcard.FOUR) in picker.hands)
        assertTrue(sixEightVsFour !in picker.hands)
        assertTrue(picker.deal(2_000).none { it.player.size == 2 && it.values == sixEightVsFour })
    }

    @Test
    fun itDealsEveryTypeExactlyWhenNoTypeSwitchedOnCanComeUpUnderTheRules() {
        val every = HandPicker(RuleSet.S17, HandCustomization()).deal(500)
        val eightsVsAce = HandValues(Upcard.EIGHT, Upcard.EIGHT, Upcard.ACE)

        // No pair surrenders when the dealer stands on soft 17, while 8-8 against an ace does when the dealer hits
        assertEquals(every, HandPicker(RuleSet.S17, onlyOn(pairsSurrender)).deal(500))
        assertEquals(every, HandPicker(RuleSet.S17, HandCustomization(switchedOff = HAND_TYPES.toSet())).deal(500))
        assertEquals(List(500) { eightsVsAce }, HandPicker(RuleSet.H17, onlyOn(pairsSurrender)).deal(500).map { it.values })
        assertEquals(setOf(ChartTable.HARD, ChartTable.SOFT, ChartTable.PAIRS), every.map { chartRow(it.player).table }.toSet())
    }
}
