package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class AnsweringTest {
    private val now = Instant.parse("2026-09-17T12:00:00Z")

    // Hard 16 and 17 against an ace are the only hard hands to surrender when the dealer hits soft 17
    private val nineEightVsAce = TrainerHand(cards("9d 8c"), card("Ac"))
    private val nineSevenVsAce = TrainerHand(cards("9c 7d"), card("Ah"))

    @Test
    fun anAnswerIsGradedUnderTheRulesAndRecordedWithTheNextHandDealt() {
        val recorded = TrainerState(nineEightVsAce).record(nineEightVsAce, Move.HIT, RuleSet.H17, emptyList(), now) { nineSevenVsAce }

        assertEquals(PracticeAnswer(now, RuleSet.H17, nineEightVsAce, Move.HIT, Move.SURRENDER), recorded?.answer)
        assertEquals(nineSevenVsAce, recorded?.state?.hand)
    }

    @Test
    fun anAnswerToAHandNoLongerOnTheTableRecordsNothing() {
        val state = TrainerState(nineSevenVsAce)

        assertNull(state.record(nineEightVsAce, Move.HIT, RuleSet.H17, emptyList(), now) { error("A stale answer must not deal") })
    }

    @Test
    fun aHandAnsweredWrongWeighsAsMissedOnTheVeryNextDeal() {
        val picker = HandPicker(RuleSet.H17, HandCustomization(HandsDealt.PRIORITIZE_WORSE, switchedOff = HAND_TYPES.toSet() - HandType(ChartTable.HARD, Move.SURRENDER)))
        val random = Random(21)

        val next = List(2_000) {
            requireNotNull(TrainerState(nineEightVsAce).record(nineEightVsAce, Move.HIT, RuleSet.H17, emptyList(), now) { picker.pick(it, random) }).state.hand
        }

        // Missed once, it weighs 20 against the other three unanswered hands' 2 apiece
        assertEquals(20.0 / 26, next.count { it.values == nineEightVsAce.values }.toDouble() / next.size, 0.03)
    }
}
