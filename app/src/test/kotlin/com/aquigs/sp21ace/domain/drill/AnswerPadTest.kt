package com.aquigs.sp21ace.domain.drill

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerPadTest {
    private val open = AnswerPad(-6..6, openEnded = true)
    private val closed = AnswerPad(-6..6)

    @Test
    fun anOpenPadsEndsStandForEveryNumberPastThem() {
        assertEquals(listOf(-6, -6, -5, 0, 6, 6), listOf(-9, -6, -5, 0, 6, 12).map(open::answer))
        assertEquals(listOf("≤−6", "−5", "0", "+5", "≥+6"), listOf(-6, -5, 0, 5, 6).map(open::label))
        assertEquals(listOf("−6 or lower", "0", "+6 or higher"), listOf(-6, 0, 6).map(open::inPlainWords))
    }

    @Test
    fun aClosedPadsEndsStandOnlyForThemselves() {
        assertEquals(listOf("−6", "0", "+6"), listOf(-6, 0, 6).map(closed::label))
        assertEquals(listOf("−6", "0", "+6"), listOf(-6, 0, 6).map(closed::inPlainWords))
    }
}
