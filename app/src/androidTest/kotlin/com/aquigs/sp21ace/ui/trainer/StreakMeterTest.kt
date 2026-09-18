package com.aquigs.sp21ace.ui.trainer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.trainer.STREAK_RUNGS
import com.aquigs.sp21ace.ui.assertFitsOnOneLine
import com.aquigs.sp21ace.ui.textLayout
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreakMeterTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun showMeter(streak: Int, height: Dp = 480.dp, configuration: DeviceConfigurationOverride = DeviceConfigurationOverride { content -> content() }) {
        compose.setContent { DeviceConfigurationOverride(configuration) { Sp21AceTheme { StreakMeter(streak, Modifier.height(height)) } } }
    }

    // A screen reader hears the meter as one item, so its numbers are only in the unmerged tree
    private fun centreOf(text: String): Float =
        compose.onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot().let { (it.top + it.bottom).value / 2 }

    @Test
    fun aStreakBetweenRungsSitsOnTheHighestRungItHasReached() {
        showMeter(3)

        assertEquals(centreOf("2"), centreOf("3"), 1f)
    }

    @Test
    fun pastTheTopRungTheStreakStaysThereAndKeepsCounting() {
        showMeter(300)

        assertEquals(centreOf("256"), centreOf("300"), 1f)
    }

    @Test
    fun onAShortLadderNoTwoRungLabelsOverlap() {
        // Rungs closer together than a label is tall, as on a short phone at a large font size
        showMeter(300, height = 160.dp)

        val placed = STREAK_RUNGS.flatMap { compose.onAllNodesWithText("$it", useUnmergedTree = true).fetchSemanticsNodes() }
            .filter { it.layoutInfo.isPlaced }
            .map { it.boundsInRoot }

        placed.zipWithNext().forEach { (below, above) -> assertTrue("$above overlaps $below", above.bottom <= below.top) }
    }

    @Test
    fun atTheLargestFontSizeAFourDigitStreakFitsItsCircle() {
        showMeter(1024, configuration = DeviceConfigurationOverride.FontScale(2f))

        compose.onNodeWithText("1024", useUnmergedTree = true).fetchSemanticsNode().textLayout().assertFitsOnOneLine()
    }
}
