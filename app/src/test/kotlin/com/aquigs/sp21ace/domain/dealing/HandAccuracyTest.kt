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
    fun aHandIsItsTwoCardValuesInEitherOrderAgainstTheUpcardsWhateverTheSuitsAndItsMove() {
        // Hard 16 vs A hits when the dealer stands on soft 17
        assertEquals(HandValues(Upcard.SEVEN, Upcard.NINE, Upcard.ACE, Move.HIT), nineSevenVsAce.key(s17))
        assertEquals(nineSevenVsAce.key(s17), TrainerHand(cards("7h 9s"), card("Ad")).key(s17))
        assertEquals(tenSixVsAce.key(s17), TrainerHand(cards("6h Jd"), card("Ac")).key(s17))
    }

    @Test
    fun aHandOf3OrMoreCardsIsItsTotalAgainstTheUpcardAndTheMoveTheChartCallsFor() {
        // Hard 14 vs 4 is S4* when the dealer stands on soft 17: 3 cards stand, and 4 or more hit
        val fiveFourFiveVsFour = TrainerHand(cards("5c 4d 5h"), card("4s"))

        assertEquals(MultiCardHand(ChartRow(ChartTable.HARD, "14"), Upcard.FOUR, Move.STAND), fiveFourFiveVsFour.key(s17))
        assertEquals(fiveFourFiveVsFour.key(s17), TrainerHand(cards("Kh 2d 2s"), card("4d")).key(s17))
        assertEquals(MultiCardHand(ChartRow(ChartTable.HARD, "14"), Upcard.FOUR, Move.HIT), TrainerHand(cards("2c 3d 4h 5s"), card("4s")).key(s17))
    }

    @Test
    fun aDoubledHandIsItsTotalAfterDoublingAgainstTheUpcardAndTheMoveTheChartCallsForHoweverManyCards() {
        // Hard 14 vs 9 once doubled is a rescue whatever the rules
        val rescue = DoubledHand(ChartRow(ChartTable.AFTER_DOUBLE_HARD, "14"), Upcard.NINE, Move.RESCUE)

        assertEquals(rescue, TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true).key(s17))
        assertEquals(rescue, TrainerHand(cards("2c 3d 4h 5s"), card("9d"), doubled = true).key(s17))
        // Soft 18 once doubled stands, and without redoubling Double Down Rescue prints no row for it
        assertEquals(DoubledHand(ChartRow(ChartTable.AFTER_DOUBLE_SOFT, "A-7"), Upcard.NINE, Move.STAND), TrainerHand(cards("As 5d 2c"), card("9s"), doubled = true).key(s17))
    }

    @Test
    fun twoCardsWhoseSuitsMakeABonusAreAHandApartFromTheSameCardsInOtherSuits() {
        // Hard 14 vs 6 is S6" when the dealer hits soft 17, so a 6-8 of spades hits for the bonus while any other 6-8 stands
        val h17 = StrategyCharts.forRules(RuleSet.H17)
        val spaded = TrainerHand(cards("6s 8s"), card("6h")).key(h17)

        assertEquals(HandValues(Upcard.SIX, Upcard.EIGHT, Upcard.SIX, Move.HIT), spaded)
        assertEquals(HandValues(Upcard.SIX, Upcard.EIGHT, Upcard.SIX, Move.STAND), TrainerHand(cards("6h 8h"), card("6s")).key(h17))
        // Hard 14 vs 4 is S4*, so every 6-8 hits for the bonus
        assertEquals(TrainerHand(cards("6s 8s"), card("4h")).key(h17), TrainerHand(cards("6h 8d"), card("4s")).key(h17))
    }

    @Test
    fun theBonusSwitchCountsAnswersToBonusHandsInEverySuitByTheRulesThatGradedThem() {
        // When the dealer hits soft 17, hard 13 vs 6 is S4*, 7-7 vs 7 P$ and hard 14 vs 6 S6", while hard 13 vs 6 is a plain hit
        // when the dealer stands
        val history = listOf(
            answer(TrainerHand(cards("6c 7d"), card("6s")), right = true, Move.HIT, rules = RuleSet.H17),
            answer(TrainerHand(cards("7h 7h"), card("7s")), right = false, Move.HIT, rules = RuleSet.H17),
            answer(TrainerHand(cards("7h 7s"), card("7d")), right = true, Move.SPLIT, rules = RuleSet.H17),
            answer(TrainerHand(cards("6h 8c"), card("6d")), right = false, Move.STAND, rules = RuleSet.H17),
            answer(TrainerHand(cards("6c 7d"), card("6s")), right = true, Move.HIT, rules = RuleSet.S17),
            // Hard 14 vs 7 carries no bonus mark, and 3 cards could only make a bonus at 21
            answer(TrainerHand(cards("6c 8d"), card("7s")), right = true, Move.HIT, rules = RuleSet.H17),
            answer(TrainerHand(cards("2c 6d 5h"), card("6s")), right = true, Move.STAND, rules = RuleSet.H17),
        )

        assertEquals(Tally(correct = 2, incorrect = 2), history.bonusTally())
    }

    @Test
    fun everyAnswerEverGivenCountsForItsHandWhateverItsAgeAndTheRulesThatGradedIt() {
        val history = listOf(
            answer(nineSevenVsAce, right = true, at = Instant.EPOCH),
            answer(TrainerHand(cards("7h 9s"), card("Ad")), right = false, rules = RuleSet.S17),
            answer(tenSixVsAce, right = false),
            // Hard 16 vs A hits when the dealer stands on soft 17, whatever the rules that graded it
            answer(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = true, Move.HIT),
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = true, Move.RESCUE),
        )

        val expected = mapOf(
            HandValues(Upcard.SEVEN, Upcard.NINE, Upcard.ACE, Move.HIT) to Tally(correct = 1, incorrect = 1),
            HandValues(Upcard.SIX, Upcard.TEN, Upcard.ACE, Move.HIT) to Tally(correct = 0, incorrect = 1),
            MultiCardHand(ChartRow(ChartTable.HARD, "16"), Upcard.ACE, Move.HIT) to Tally(correct = 1, incorrect = 0),
            DoubledHand(ChartRow(ChartTable.AFTER_DOUBLE_HARD, "14"), Upcard.NINE, Move.RESCUE) to Tally(correct = 1, incorrect = 0),
        )
        assertEquals(expected, history.tallyByHand(s17))
    }

    @Test
    fun theSwitchForHandsOf3OrMoreCardsCountsEveryAnswerToOneNotYetDoubled() {
        val history = listOf(
            answer(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = true, Move.HIT),
            answer(TrainerHand(cards("2c 3d 4h 5s"), card("4s")), right = false, Move.HIT),
            answer(nineSevenVsAce, right = true),
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = true, Move.RESCUE),
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
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = false, Move.RESCUE, rules = RuleSet.S17),
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
    fun aDoubledHandCountsForTheSwitchOfItsAfterDoublingTableRatherThanTheHardOrSoftOne() {
        // Hard 14 vs 9 once doubled is a rescue, and soft 18 vs 4 a redouble with redoubling
        val history = listOf(
            answer(TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true), right = true, Move.RESCUE),
            answer(TrainerHand(cards("As 5d 2c"), card("4h"), doubled = true), right = false, Move.REDOUBLE, rules = RuleSet.H17_REDOUBLE),
        )

        val counted = mapOf(
            HandType(ChartTable.AFTER_DOUBLE_HARD, Move.RESCUE) to Tally(correct = 1, incorrect = 0),
            HandType(ChartTable.AFTER_DOUBLE_SOFT, Move.REDOUBLE) to Tally(correct = 0, incorrect = 1),
        )
        assertEquals(HAND_TYPES.associateWith { counted[it] ?: Tally(correct = 0, incorrect = 0) }, history.tallyByHandType())
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
