package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.BonusException
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class TrainerTest {
    private val s17 = StrategyCharts.forRules(RuleSet.S17)
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val softSeventeenVsTen = TrainerHand(cards("As 6d"), card("Kh"))

    // Hard 14 vs 4 is S4*, so a 6-8 hits while the 6-7-8 bonus is possible
    private val sixEightVsFour = TrainerHand(cards("6c 8d"), card("4s"))

    @Test
    fun gradesTheHandOnTheTableThenDealsTheNext() {
        val next = TrainerState(sixteenVsAce).answer(sixteenVsAce, Move.HIT, s17) { softSeventeenVsTen }

        assertEquals(TrainerState(softSeventeenVsTen, Grade(sixteenVsAce, Play(Action.HIT), Move.HIT, Move.HIT)), next)
        assertTrue(next.lastGrade!!.isCorrect)
    }

    @Test
    fun gradesAWrongAnswerWithTheMoveTheSquareCallsFor() {
        val grade = TrainerState(sixEightVsFour).answer(sixEightVsFour, Move.STAND, s17) { sixteenVsAce }.lastGrade!!

        val square = Play(Action.STAND, hitWithCards = 4, bonusException = BonusException.ANY_678)
        assertEquals(Grade(sixEightVsFour, square, Move.STAND, Move.HIT), grade)
        assertFalse(grade.isCorrect)
    }

    @Test
    fun ignoresAnAnswerToAHandNoLongerOnTheTable() {
        val state = TrainerState(softSeventeenVsTen, Grade(sixteenVsAce, Play(Action.HIT), Move.HIT, Move.HIT))

        assertSame(state, state.answer(sixteenVsAce, Move.HIT, s17) { error("A stale answer must not deal") })
    }

    @Test
    fun keepsOnlyTheLatestAnswerWithItsHandAndBothMoves() {
        // 8-8 vs 6 is P
        val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))

        val last = TrainerState(sixteenVsAce)
            .answer(sixteenVsAce, Move.HIT, s17) { eightsVsSix }
            .answer(eightsVsSix, Move.STAND, s17) { softSeventeenVsTen }
            .lastGrade

        assertEquals(Grade(eightsVsSix, Play(Action.SPLIT), Move.STAND, Move.SPLIT), last)
    }

    @Test
    fun comesBackEqualFromSerialization() {
        // Android serializes the saved state once the app is in the background, which recreating the activity in a device test doesn't
        val state = TrainerState(sixEightVsFour).answer(sixEightVsFour, Move.STAND, s17) { dealTrainerHand() }

        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(state) } }.toByteArray()

        assertEquals(state, ObjectInputStream(bytes.inputStream()).use { it.readObject() })
    }
}
