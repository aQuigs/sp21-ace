package com.aquigs.sp21ace.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatmapColorsTest {
    @Test
    fun inBothThemesTheScaleRunsFromRedWithNoneRightThroughTheMiddleToGreenWithAllRight() {
        for ((theme, heatmap) in mapOf("light" to LightSp21AceColors.heatmap, "dark" to DarkSp21AceColors.heatmap)) {
            assertEquals(theme, heatmap.noneRight, heatmap.at(0f))
            assertEquals(theme, heatmap.halfRight, heatmap.at(0.5f))
            assertEquals(theme, heatmap.allRight, heatmap.at(1f))

            assertTrue("$theme: none right is red", heatmap.noneRight.red > heatmap.noneRight.green)
            assertTrue("$theme: all right is green", heatmap.allRight.green > heatmap.allRight.red)
        }
    }
}
