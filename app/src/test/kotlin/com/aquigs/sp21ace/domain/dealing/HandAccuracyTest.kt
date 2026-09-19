package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HandAccuracyTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")
    private val s17 = StrategyCharts.forRules(RuleSet.S17)
    private val tenSixVsAce = TrainerHand(cards("Kc 6d"), card("As"))
    private val nineSevenVsAce = TrainerHand(cards("9c 7d"), card("Ah"))

    private fun answer(hand: TrainerHand, right: Boolean, correctMove: Move = Move.SURRENDER, at: Instant = now, rules: RuleSet = RuleSet.H17) =
        PracticeAnswer(at, rules, hand, if (right) correctMove else Move.entries.first { it != correctMove }, correctMove)

    @Test
    fun aHandIsItsTwoCardValuesInEitherOrderAgainstTheUpcardsWhateverTheSuits() {
        assertEquals(HandValues(Upcard.SEVEN, Upcard.NINE, Upcard.ACE), nineSevenVsAce.values)
        assertEquals(nineSevenVsAce.values, TrainerHand(cards("7h 9s"), card("Ad")).values)
        assertEquals(tenSixVsAce.values, TrainerHand(cards("6h Jd"), card("Ac")).values)
    }

    @Test
    fun aHandOf3OrMoreCardsIsItsTotalAgainstTheUpcardAndTheMoveTheChartCallsFor() {
        // Hard 14 vs 4 is S4* when the dealer stands on soft 17: 3 cards stand, and 4 or more hit
        val fiveFourFiveVsFour = TrainerHand(cards("5c 4d 5h"), card("4s"))

        assertEquals(MultiCardHand(ChartRow(ChartTable.HARD, "14"), Upcard.FOUR, Move.STAND), fiveFourFiveVsFour.key(s17))
        assertEquals(fiveFourFiveVsFour.key(s17), TrainerHand(cards("Kh 2d 2s"), card("4d")).key(s17))
        assertEquals(MultiCardHand(ChartRow(ChartTable.HARD, "14"), Upcard.FOUR, Move.HIT), TrainerHand(cards("2c 3d 4h 5s"), card("4s")).key(s17))
        assertEquals(nineSevenVsAce.values, nineSevenVsAce.key(s17))
        // Prioritize worse hands never deals a doubled hand
        assertNull(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true).key(s17))
    }

    @Test
    fun everyAnswerEverGivenCountsForItsHandWhateverItsAgeAndTheRulesThatGradedIt() {
        val history = listOf(
            answer(nineSevenVsAce, right = true, at = Instant.EPOCH),
            answer(TrainerHand(cards("7h 9s"), card("Ad")), right = false, rules = RuleSet.S17),
            answer(tenSixVsAce, right = false),
            // Hard 16 vs A hits when the dealer stands on soft 17, whatever the rules that graded it
            answer(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = true, Move.HIT),
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = true),
        )

        val expected = mapOf(
            nineSevenVsAce.values to Tally(correct = 1, incorrect = 1),
            tenSixVsAce.values to Tally(correct = 0, incorrect = 1),
            MultiCardHand(ChartRow(ChartTable.HARD, "16"), Upcard.ACE, Move.HIT) to Tally(correct = 1, incorrect = 0),
        )
        assertEquals(expected, history.tallyByHand(s17))
    }

    @Test
    fun theSwitchForHandsOf3OrMoreCardsCountsEveryAnswerToOneNotYetDoubled() {
        val history = listOf(
            answer(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = true, Move.HIT),
            answer(TrainerHand(cards("2c 3d 4h 5s"), card("4s")), right = false, Move.HIT),
            answer(nineSevenVsAce, right = true),
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = true),
        )

        assertEquals(Tally(correct = 1, incorrect = 1), history.multiCardTally())
    }

    @Test
    fun theCardCountSwitchCountsAnswersOf3OrMoreCardsOnSquaresWhoseMoveTheCardCountDecides() {
        // When the dealer stands on soft 17, hard 14 vs 4 is S4*, hard 10 vs 8 D3, hard 17 vs A RH and hard 16 vs A a plain hit,
        // but when the dealer hits soft 17, hard 16 vs A is RH too
        val history = listOf(
            answer(TrainerHand(cards("5c 4d 5h"), card("4s")), right = true, Move.STAND, rules = RuleSet.S17),
            answer(TrainerHand(cards("2c 3d 4h 5s"), card("4s")), right = false, Move.HIT, rules = RuleSet.S17),
            answer(TrainerHand(cards("2c 3d 5h"), card("8s")), right = true, Move.HIT, rules = RuleSet.S17),
            answer(TrainerHand(cards("9c 4d 4h"), card("As")), right = true, Move.HIT, rules = RuleSet.S17),
            answer(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = false, Move.HIT, rules = RuleSet.S17),
            answer(TrainerHand(cards("9h 4c 3d"), card("Ac")), right = false, Move.HIT, rules = RuleSet.H17),
            // Hard 11 vs 5 is D5, but two cards make no card-count hand, and a doubled hand is read from the after-doubling tables
            answer(TrainerHand(cards("6c 5d"), card("5s")), right = false, Move.DOUBLE, rules = RuleSet.S17),
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = false, rules = RuleSet.S17),
        )

        assertEquals(Tally(correct = 3, incorrect = 2), history.cardCountTally())
    }

    @Test
    fun everyAnswerEverGivenCountsForTheTypeOfHandItsGradeCalledFor() {
        // When the dealer stands on soft 17, hard 16 vs A is a hit, hard 18 vs 6 a stand and a pair of 8s vs 6 a split
        val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
        val eighteenVsSix = TrainerHand(cards("Kc 8d"), card("6h"))
        val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))
        val history = listOf(
            answer(sixteenVsAce, right = true, Move.HIT, rules = RuleSet.S17),
            answer(sixteenVsAce, right = true, Move.HIT, rules = RuleSet.S17),
            answer(sixteenVsAce, right = false, Move.HIT, rules = RuleSet.S17),
            answer(eighteenVsSix, right = false, Move.STAND, rules = RuleSet.S17),
            // A year ago, which still counts
            answer(eightsVsSix, right = true, Move.SPLIT, at = now.minus(Duration.ofDays(365)), rules = RuleSet.S17),
        )

        val counted = mapOf(
            HandType(ChartTable.HARD, Move.HIT) to Tally(correct = 2, incorrect = 1),
            HandType(ChartTable.HARD, Move.STAND) to Tally(correct = 0, incorrect = 1),
            HandType(ChartTable.PAIRS, Move.SPLIT) to Tally(correct = 1, incorrect = 0),
        )
        assertEquals(HAND_TYPES.associateWith { counted[it] ?: Tally(correct = 0, incorrect = 0) }, history.tallyByHandType())
    }

    @Test
    fun aDoubledHandCountsForNoSwitchSinceItsReadFromTheAfterDoublingTables() {
        // Hard 14 vs 9 once doubled is a rescue, which no switch deals
        val history = listOf(answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = true))

        assertEquals(HAND_TYPES.associateWith { Tally(correct = 0, incorrect = 0) }, history.tallyByHandType())
    }

    @Test
    fun aHandWeighsTheInverseOfItsAccuracyWithNoAnswersAt50PercentAndAFloorAt5Percent() {
        val weights = mapOf(
            null to 2.0,
            Tally(correct = 3, incorrect = 0) to 1.0,
            Tally(correct = 1, incorrect = 1) to 2.0,
            Tally(correct = 1, incorrect = 3) to 4.0,
            Tally(correct = 1, incorrect = 9) to 10.0,
            Tally(correct = 1, incorrect = 29) to 20.0,
            Tally(correct = 0, incorrect = 3) to 20.0,
        )

        assertEquals(weights, weights.mapValues { weight(it.key) })
    }
}
