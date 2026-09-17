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
                ChartTable.RESCUE to """
                    hand  2     A
                    16    .     R†
                """,
            ),
        )

        assertEquals(listOf("16"), chart.hands(ChartTable.RESCUE))
        assertNull(chart.play(ChartTable.RESCUE, "16", Upcard.TWO))
        assertEquals(Play(Action.SURRENDER, debated = true), chart.play(ChartTable.RESCUE, "16", Upcard.ACE))
    }

    @Test
    fun rejectsMalformedGrids() {
        for (grid in listOf("", "hand 2 3\n9 D", "hand 2\n16 S\n16 H", "hand 2 2\n16 S H")) {
            assertThrows(grid, IllegalArgumentException::class.java) { StrategyChart.parse(mapOf(ChartTable.HARD to grid)) }
        }
    }
}
