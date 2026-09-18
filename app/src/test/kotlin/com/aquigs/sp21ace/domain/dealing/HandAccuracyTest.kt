package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class HandAccuracyTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")
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
    fun everyAnswerEverGivenCountsForItsHandWhateverItsAgeAndTheRulesThatGradedIt() {
        val history = listOf(
            answer(nineSevenVsAce, right = true, at = Instant.EPOCH),
            answer(TrainerHand(cards("7h 9s"), card("Ad")), right = false, rules = RuleSet.S17),
            answer(tenSixVsAce, right = false),
            // Three cards make no two-card hand
            answer(TrainerHand(cards("9c 4d 3s"), card("Ad")), right = true),
        )

        assertEquals(mapOf(nineSevenVsAce.values to Tally(correct = 1, incorrect = 1), tenSixVsAce.values to Tally(correct = 0, incorrect = 1)), history.tallyByHand())
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
