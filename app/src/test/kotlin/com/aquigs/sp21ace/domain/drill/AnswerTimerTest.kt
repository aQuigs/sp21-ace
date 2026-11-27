package com.aquigs.sp21ace.domain.drill

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.indices.FakeIndices
import com.aquigs.sp21ace.domain.indices.Flashcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class AnswerTimerTest {
    private var now = 1_000L
    private val timer = AnswerTimer<Flashcard> { now }

    private val first = Flashcard(FakeIndices.STAND, cards("9c 9d"), card("2h"))
    private val second = Flashcard(FakeIndices.INSURANCE, emptyList(), card("As"))

    @Test
    fun timesACardFromWhenItIsFirstShown() {
        timer.dealt(first)
        now += 5_000
        timer.shown(first)
        now += 1_234
        // Shown again, as when the screen redraws it, doesn't restart the time
        timer.shown(first)
        now += 100

        assertEquals(1_334.milliseconds, timer.elapsed(first))
    }

    @Test
    fun anInterruptionLeavesTheCardUntimedEvenOnceItShowsAgain() {
        timer.dealt(first)
        timer.shown(first)
        timer.interrupted()
        timer.shown(first)

        assertNull(timer.elapsed(first))

        // The next card dealt is timed again
        timer.dealt(second)
        timer.shown(second)
        now += 700
        assertEquals(700.milliseconds, timer.elapsed(second))
    }

    @Test
    fun anInterruptionBeforeTheCardFirstShowsCostsItNothing() {
        timer.dealt(first)
        timer.interrupted()
        timer.shown(first)
        now += 400

        assertEquals(400.milliseconds, timer.elapsed(first))
    }

    @Test
    fun displaysZeroUntilTheCardFirstShowsThenItsTimeAndNothingOnceItIsUntimed() {
        assertNull(timer.display(first))

        timer.dealt(first)
        timer.interrupted()
        now += 900
        assertEquals(0.milliseconds, timer.display(first))
        assertNull(timer.display(second))

        timer.shown(first)
        now += 400
        assertEquals(400.milliseconds, timer.display(first))

        timer.interrupted()
        assertNull(timer.display(first))
    }

    @Test
    fun aCardItDidntSeeDealtIsNeverTimed() {
        // As a card restored in a recreated activity, shown before the timer was made
        timer.shown(first)
        assertNull(timer.elapsed(first))

        timer.dealt(first)
        timer.shown(first)
        assertNull(timer.elapsed(second))
    }
}
