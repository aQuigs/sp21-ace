package com.aquigs.sp21ace.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DissolveTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // The old value is wider than the new, as a hand of 3 cards is than the hand of 2 dealt after it
    private val widths = mapOf(Color.Red to 80.dp, Color.Blue to 40.dp)
    private var colour by mutableStateOf(Color.Red)

    private fun showColour() = compose.setContent {
        Dissolve(colour, Modifier.testTag(TAG)) { Box(Modifier.size(widths.getValue(it), 40.dp).background(it)) }
    }

    private fun centre(): Color = compose.onNodeWithTag(TAG).captureToImage().toPixelMap().let { it[it.width / 2, it.height / 2] }

    private fun dealBlue() {
        compose.mainClock.autoAdvance = false
        colour = Color.Blue
        compose.mainClock.advanceTimeBy(DISSOLVE_MILLIS / 2L)
    }

    @Test
    fun theFirstValueShowsWithoutFadingIn() {
        compose.mainClock.autoAdvance = false
        showColour()
        compose.mainClock.advanceTimeByFrame()

        assertEquals(1f, centre().red, 0.02f)
    }

    @Test
    fun halfwayTheOldValueShowsThroughTheNew() {
        showColour()

        dealBlue()

        centre().let { assertTrue("$it", it.red > 0.1f && it.blue > 0.1f) }
    }

    @Test
    fun theNewValueEndsUpAlone() {
        showColour()

        colour = Color.Blue

        centre().let {
            assertEquals(0f, it.red, 0.02f)
            assertEquals(1f, it.blue, 0.02f)
        }
    }

    @Test
    fun theLayoutTakesTheNewValuesSizeAtOnce() {
        showColour()

        dealBlue()

        compose.onNodeWithTag(TAG).assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun aScreenReaderFollowsOneNodeThatHoldsOnlyTheNewValue() {
        var text by mutableStateOf("Hard 16 vs A")
        compose.setContent { Dissolve(text) { Text(it) } }
        val node = compose.onNodeWithText("Hard 16 vs A").fetchSemanticsNode().id

        compose.mainClock.autoAdvance = false
        text = "Pair of 8s vs 6"
        compose.mainClock.advanceTimeBy(DISSOLVE_MILLIS / 2L)

        compose.onNodeWithText("Hard 16 vs A").assertDoesNotExist()
        assertEquals(node, compose.onNodeWithText("Pair of 8s vs 6").fetchSemanticsNode().id)
    }

    private companion object {
        const val TAG = "dissolve"
    }
}
