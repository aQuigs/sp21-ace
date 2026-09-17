package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test(expected = IllegalArgumentException::class)
    fun rejectsRowsWithMissingSquares() {
        StrategyChart.parse(mapOf(ChartTable.HARD to "hand 2 3\n9 D"))
    }
}
