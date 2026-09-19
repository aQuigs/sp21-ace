package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.BonusException
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyChart
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    // 8-8 vs 6 is P
    private val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))

    // The trainer after a graded answer, for tests about what it leaves rather than the grade
    private fun TrainerState.after(asked: TrainerHand, move: Move, deal: (Grade) -> TrainerHand) = requireNotNull(answer(asked, move, s17, deal)).state

    @Test
    fun gradesTheHandOnTheTableThenDealsTheNext() {
        val grade = Grade(sixteenVsAce, Play(Action.HIT), Move.HIT, Move.HIT)

        assertEquals(
            Answered(TrainerState(softSeventeenVsTen, grade, streak = 1), grade),
            TrainerState(sixteenVsAce).answer(sixteenVsAce, Move.HIT, s17) { softSeventeenVsTen },
        )
        assertTrue(grade.isCorrect)
    }

    @Test
    fun gradesAWrongAnswerWithTheMoveTheSquareCallsFor() {
        val grade = requireNotNull(TrainerState(sixEightVsFour).answer(sixEightVsFour, Move.STAND, s17) { sixteenVsAce }).grade

        val square = Play(Action.STAND, hitWithCards = 4, bonusException = BonusException.ANY_678)
        assertEquals(Grade(sixEightVsFour, square, Move.STAND, Move.HIT), grade)
        assertFalse(grade.isCorrect)
    }

    @Test
    fun gradesAHandOf3OrMoreCardsByItsTotalWithTheCardCountTurningTheStandIntoAHit() {
        // Hard 14 vs 4 is S4*, with no 6-7-8 left to make once there are 3 cards
        val threeCards = TrainerHand(cards("5c 4d 5h"), card("4s"))
        val fourCards = TrainerHand(cards("2c 3d 4h 5s"), card("4s"))
        val square = Play(Action.STAND, hitWithCards = 4, bonusException = BonusException.ANY_678)

        assertEquals(Grade(threeCards, square, Move.STAND, Move.STAND), TrainerState(threeCards).answer(threeCards, Move.STAND, s17) { sixteenVsAce }?.grade)
        assertEquals(Grade(fourCards, square, Move.STAND, Move.HIT), TrainerState(fourCards).answer(fourCards, Move.STAND, s17) { sixteenVsAce }?.grade)
    }

    @Test
    fun aMoveOnlyTheFirstTwoCardsAllowGradesNothingOnceThereAreMore() {
        // Hard 17 vs A is RH: surrender on the first two cards, and a hit after
        val threeCards = TrainerHand(cards("9c 4d 4h"), card("As"))
        val state = TrainerState(threeCards)

        for (move in Move.entries - threeCards.moves(redoubling = false)) {
            assertNull("$move", state.answer(threeCards, move, s17) { error("A move the hand doesn't allow must not deal") })
        }
        assertEquals(Move.HIT, state.answer(threeCards, Move.STAND, s17) { sixteenVsAce }?.grade?.correctMove)
    }

    @Test
    fun gradesADoubledHandFromTheTablesForDoubledHandsWhateverItsCardCount() {
        val h17 = StrategyCharts.forRules(RuleSet.H17)
        val redouble = StrategyCharts.forRules(RuleSet.H17_REDOUBLE)
        // Doubled hard 16 vs 10 is R, and doubled hard 10 vs 5 D with redoubling
        val sixteenVsTen = TrainerHand(cards("2c 3d 4h 2s 5d"), card("Ks"), doubled = true)
        val tenVsFive = TrainerHand(cards("3c 2d 5h"), card("5s"), doubled = true)

        fun grade(hand: TrainerHand, move: Move, chart: StrategyChart) = TrainerState(hand).answer(hand, move, chart) { sixteenVsAce }?.grade

        assertEquals(Grade(sixteenVsTen, Play(Action.SURRENDER), Move.STAND, Move.RESCUE), grade(sixteenVsTen, Move.STAND, h17))
        assertEquals(Grade(tenVsFive, Play(Action.DOUBLE), Move.REDOUBLE, Move.REDOUBLE), grade(tenVsFive, Move.REDOUBLE, redouble))
        // Without redoubling Double Down Rescue prints no row for hard 10, which stands, and no redouble is taken
        assertEquals(Grade(tenVsFive, Play(Action.STAND), Move.STAND, Move.STAND), grade(tenVsFive, Move.STAND, h17))
        assertNull(grade(tenVsFive, Move.REDOUBLE, h17))
        for (move in listOf(Move.HIT, Move.DOUBLE, Move.SPLIT, Move.SURRENDER)) assertNull("$move", grade(tenVsFive, move, redouble))
    }

    @Test
    fun anAnswerToAHandNoLongerOnTheTableGradesNothing() {
        val state = TrainerState(softSeventeenVsTen, Grade(sixteenVsAce, Play(Action.HIT), Move.HIT, Move.HIT))

        // Nothing to record, and nothing dealt
        assertNull(state.answer(sixteenVsAce, Move.HIT, s17) { error("A stale answer must not deal") })
    }

    @Test
    fun keepsOnlyTheLatestAnswerWithItsHandAndBothMoves() {
        val last = TrainerState(sixteenVsAce)
            .after(sixteenVsAce, Move.HIT) { eightsVsSix }
            .after(eightsVsSix, Move.STAND) { softSeventeenVsTen }
            .lastGrade

        assertEquals(Grade(eightsVsSix, Play(Action.SPLIT), Move.STAND, Move.SPLIT), last)
    }

    @Test
    fun eachRightAnswerAddsOneToTheStreakWithNoCapAtTheTopRung() {
        val once = TrainerState(sixteenVsAce).after(sixteenVsAce, Move.HIT) { eightsVsSix }
        val twice = once.after(eightsVsSix, Move.SPLIT) { softSeventeenVsTen }

        assertEquals(listOf(1, 2), listOf(once.streak, twice.streak))
        assertEquals(257, TrainerState(sixteenVsAce, streak = 256).after(sixteenVsAce, Move.HIT) { eightsVsSix }.streak)
    }

    @Test
    fun aWrongAnswerDropsTheStreakToZero() {
        val next = TrainerState(eightsVsSix, streak = 5).after(eightsVsSix, Move.STAND) { sixteenVsAce }

        assertEquals(0, next.streak)
    }

    @Test
    fun theMeterMarksTheHighestRungTheStreakHasReached() {
        val rungs = listOf(0, 1, 3, 4, 255, 256, 300).associateWith { STREAK_RUNGS[streakRung(it)] }

        assertEquals(mapOf(0 to 0, 1 to 1, 3 to 2, 4 to 4, 255 to 128, 256 to 256, 300 to 256), rungs)
    }

    @Test
    fun comesBackEqualFromSerialization() {
        // Android serializes the saved state once the app is in the background, which recreating the activity in a device test doesn't.
        // A right answer from a streak and a doubled hand dealt next, so neither field lost in transit can hide behind its default.
        val state = TrainerState(sixEightVsFour, streak = 2).after(sixEightVsFour, Move.HIT) { TrainerHand(cards("5c 6d 3h"), card("9s"), doubled = true) }

        val bytes = ByteArrayOutputStream().also { ObjectOutputStream(it).use { out -> out.writeObject(state) } }.toByteArray()

        assertEquals(state, ObjectInputStream(bytes.inputStream()).use { it.readObject() })
    }
}
