package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class StrategyChartTest {
    @Test
    fun readsSquaresAndLeavesEmptyOnesOut() {
        val chart = StrategyChart.parse(
            mapOf(
                ChartTable.AFTER_DOUBLE_HARD to """
                    hand  2     3     4     5     6     7     8     9     10    A
                    16    .     .     .     .     .     .     R     R     R     R†
                """,
            ),
            redoubling = false,
        )

        assertEquals(listOf("16"), chart.hands(ChartTable.AFTER_DOUBLE_HARD))
        assertNull(chart.play(ChartTable.AFTER_DOUBLE_HARD, "16", Upcard.TWO))
        assertEquals(Play(Action.SURRENDER, debated = true), chart.play(ChartTable.AFTER_DOUBLE_HARD, "16", Upcard.ACE))
    }

    @Test
    fun rejectsMalformedGrids() {
        val header = "hand 2 3 4 5 6 7 8 9 10 A"
        val row = "16" + " S".repeat(10)

        for (grid in listOf("", "hand 2 A\n16 S S", "$header\n9 D", "$header\n$row\n$row")) {
            assertThrows(grid, IllegalArgumentException::class.java) { StrategyChart.parse(mapOf(ChartTable.HARD to grid), redoubling = false) }
        }
    }
}
