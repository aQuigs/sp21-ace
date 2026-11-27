package com.aquigs.sp21ace.domain.drill

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakTest {
    @Test
    fun aRightAnswerAddsOneAndAWrongOneDropsToZero() {
        assertEquals(Streak(257), Streak(256).after(isCorrect = true))
        assertEquals(Streak(), Streak(5).after(isCorrect = false))
    }

    @Test
    fun theMeterMarksTheHighestRungTheStreakHasReached() {
        val rungs = listOf(0, 1, 3, 4, 255, 256, 300).associateWith { Streak.RUNGS[Streak(it).rung] }

        assertEquals(mapOf(0 to 0, 1 to 1, 3 to 2, 4 to 4, 255 to 128, 256 to 256, 300 to 256), rungs)
    }
}
