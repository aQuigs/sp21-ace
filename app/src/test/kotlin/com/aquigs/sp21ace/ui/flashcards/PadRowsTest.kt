package com.aquigs.sp21ace.ui.flashcards

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PadRowsTest {
    @Test
    fun negativesComeFirstThenZeroAndThePositivesEachInOneRowWhileItFits() {
        assertEquals(listOf((-5..-1).toList(), (0..5).toList()), padRows(-5..5, perRow = 7))
        assertEquals(listOf((-6..-1).toList(), (0..6).toList()), padRows(-6..6, perRow = 7))
    }

    @Test
    fun aGroupTooLongForARowSplitsIntoRowsThatDifferByAtMostOne() {
        assertEquals(listOf((-10..-6).toList(), (-5..-1).toList(), (0..5).toList(), (6..10).toList()), padRows(-10..10, perRow = 7))
        assertEquals(listOf((-7..-5).toList(), (-4..-3).toList(), (-2..-1).toList(), (0..2).toList(), (3..5).toList(), (6..7).toList()), padRows(-7..7, perRow = 3))

        for (reach in 5..10) {
            for (perRow in 1..12) {
                val rows = padRows(-reach..reach, perRow)
                assertEquals((-reach..reach).toList(), rows.flatten())
                rows.forEach { assertTrue("$reach $perRow $rows", it.size in 1..perRow) }
            }
        }
    }
}
