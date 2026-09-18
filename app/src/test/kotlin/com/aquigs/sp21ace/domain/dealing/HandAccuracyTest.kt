package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HandAccuracyTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")
    private val tenSixVsAce = TrainerHand(cards("Kc 6d"), card("As"))
    private val nineSevenVsAce = TrainerHand(cards("9c 7d"), card("Ah"))

    private fun answer(hand: TrainerHand, right: Boolean, at: Instant = now, rules: RuleSet = RuleSet.H17) =
        PracticeAnswer(at, rules, hand, if (right) Move.SURRENDER else Move.HIT, Move.SURRENDER)

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
