package com.aquigs.sp21ace.domain.truecount

import com.aquigs.sp21ace.domain.drill.DrillGrade
import com.aquigs.sp21ace.domain.drill.DrillRun
import com.aquigs.sp21ace.domain.drill.Streak
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.serializedAndBack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrueCountCardTest {
    private fun trueCount(runningCount: Int, quarters: Int, rounding: CountRounding) = TrueCountCard(6, Decks(quarters), rounding, runningCount).trueCount

    @Test
    fun floorAlwaysRoundsDown() {
        // 7 / 3.5, 8 / 3.5, 6 / 4, −6 / 4, −1 / 5.75, 1 / 5.75
        assertEquals(listOf(2, 2, 1, -2, -1, 0), listOf(7 to 14, 8 to 14, 6 to 16, -6 to 16, -1 to 23, 1 to 23).map { (rc, q) -> trueCount(rc, q, CountRounding.FLOOR) })
    }

    @Test
    fun truncateRoundsTowardZero() {
        assertEquals(listOf(2, 1, -1, 0, 0), listOf(7 to 14, 6 to 16, -6 to 16, -1 to 23, 1 to 23).map { (rc, q) -> trueCount(rc, q, CountRounding.TRUNCATE) })
    }

    @Test
    fun roundGoesToTheNearestWithAHalfUp() {
        // 1.5 is 2, −1.5 is −1, 2.4 is 2, −2.4 is −2, 2.6 is 3, −2.6 is −3
        val halves = listOf(6 to 16, -6 to 16).map { (rc, q) -> trueCount(rc, q, CountRounding.ROUND) }
        val others = listOf(6 to 10, -6 to 10, 13 to 20, -13 to 20).map { (rc, q) -> trueCount(rc, q, CountRounding.ROUND) }

        assertEquals(listOf(2, -1), halves)
        assertEquals(listOf(2, -2, 3, -3), others)
    }

    @Test
    fun theUnroundedTrueCountReadsToTwoDecimals() {
        fun read(runningCount: Int, quarters: Int) = signedCount(TrueCountCard(8, Decks(quarters), CountRounding.FLOOR, runningCount).unroundedTrueCount)

        // 6 / 3.5, −6 / 4, 1 / 5.75, −8 / 1, and 1 / 8 = 0.125 and its negative, a half going away from zero
        assertEquals(
            listOf("+1.71", "−1.50", "+0.17", "−8.00", "+0.13", "−0.13"),
            listOf(read(6, 14), read(-6, 16), read(1, 23), read(-8, 4), read(1, 32), read(-1, 32)),
        )
        assertEquals("0.00", signedCount(0.0))
    }

    @Test
    fun anAnswerPastThePadIsItsEnd() {
        val plusSeven = TrueCountCard(6, Decks(4), CountRounding.FLOOR, 7)
        val minusEight = plusSeven.copy(runningCount = -8)

        assertEquals(7 to 6, plusSeven.trueCount to plusSeven.answer)
        assertEquals(-8 to -6, minusEight.trueCount to minusEight.answer)
        assertTrue(DrillGrade(plusSeven, 6, tally = null, timeMillis = null).isCorrect)
        assertFalse(DrillGrade(plusSeven, 5, tally = null, timeMillis = null).isCorrect)
    }

    @Test
    fun theDecksDiscardedAreTheRestOfTheShoe() {
        val card = TrueCountCard(8, Decks(9), CountRounding.FLOOR, 4)

        assertEquals("2.25" to "5.75", card.decksLeft.label to card.decksDiscarded.label)
        assertEquals(listOf("6", "0.5", "0"), listOf(Decks.whole(6), Decks(2), Decks(0)).map { it.label })
    }

    @Test
    fun aRunSurvivesTheActivitysSavedState() {
        val card = TrueCountCard(6, Decks(14), CountRounding.ROUND, -9)
        val run = DrillRun(card, DrillGrade(card.copy(runningCount = 3), 1, Tally(2, 1), timeMillis = 2350), streak = Streak(5))

        assertEquals(run, run.serializedAndBack())
    }
}
