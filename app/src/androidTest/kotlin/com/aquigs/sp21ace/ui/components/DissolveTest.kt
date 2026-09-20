package com.aquigs.sp21ace.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    private val widths = mapOf(Color.Red to 80.dp, Color.Blue to 40.dp, Color.Green to 40.dp)
    private var colour by mutableStateOf(Color.Red)

    // On black, so each colour's share of a pixel is how far it has faded in
    private fun showColour() = compose.setContent {
        Box(Modifier.background(Color.Black)) {
            Dissolve(colour, Modifier.testTag(TAG)) { Box(Modifier.size(widths.getValue(it), 40.dp).background(it)) }
        }
    }

    private fun pixel(across: Float): Color = compose.onNodeWithTag(TAG).captureToImage().toPixelMap().let { it[(it.width * across).toInt(), it.height / 2] }

    private fun deal(next: Color, afterMillis: Long = DISSOLVE_MILLIS / 2L) {
        compose.mainClock.autoAdvance = false
        colour = next
        compose.mainClock.advanceTimeBy(afterMillis)
    }

    @Test
    fun theFirstValueShowsWithoutFadingIn() {
        compose.mainClock.autoAdvance = false
        showColour()
        compose.mainClock.advanceTimeByFrame()

        assertEquals(1f, pixel(0.5f).red, 0.02f)
    }

    @Test
    fun halfwayTheOldValueShowsThroughTheNew() {
        showColour()

        deal(Color.Blue)

        // The fade eases, so halfway through its time the new value is some way past half in
        pixel(0.5f).let { assertTrue("$it", it.red > 0.05f && it.blue in 0.1f..0.9f) }
    }

    @Test
    fun theNewValueEndsUpAlone() {
        showColour()

        colour = Color.Blue

        pixel(0.5f).let {
            assertEquals(0f, it.red, 0.02f)
            assertEquals(1f, it.blue, 0.02f)
        }
    }

    @Test
    fun aValueThatChangesMidDissolveFadesOnFromWhereItWas() {
        showColour()
        deal(Color.Blue)

        deal(Color.Green, afterMillis = 16)

        // Blue fading out from partway in rather than back at full, and red still fading out beneath it
        pixel(0.5f).let { assertTrue("$it", it.blue in 0.3f..0.85f && it.red > 0.03f) }
    }

    @Test
    fun theLayoutTakesTheNewValuesSizeAtOnce() {
        showColour()

        deal(Color.Blue)

        compose.onNodeWithTag(TAG).assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun aValueOnItsWayOutKeepsTheSizeItWasLaidOutAt() {
        // Each value fills the width it is given, as a fan of cards does, and the new one is given less
        compose.setContent {
            Box(Modifier.size(100.dp, 40.dp).background(Color.Black).testTag(TAG)) {
                Dissolve(colour, Modifier.width(widths.getValue(colour))) { Box(Modifier.fillMaxWidth().height(40.dp).background(it)) }
            }
        }

        deal(Color.Blue)

        assertTrue("the old value shrank", pixel(0.6f).red > 0.1f)
    }

    @Test
    fun aValueOnItsWayOutStaysWhereItWasWhenTheNewOneMovesTheLayout() {
        // Centred, as the trainer's hands are, so the narrower new value moves the layout's left edge in
        compose.setContent {
            Box(Modifier.size(100.dp, 40.dp).background(Color.Black).testTag(TAG), contentAlignment = Alignment.Center) {
                Dissolve(colour) { Box(Modifier.size(widths.getValue(it), 40.dp).background(it)) }
            }
        }

        deal(Color.Blue)

        // Red spanned 10 to 90 dp and still does, rather than 30 to 110 from the new left edge
        assertTrue("left edge", pixel(0.15f).red > 0.1f)
        assertEquals("past its right edge", 0f, pixel(0.95f).red, 0.02f)
    }

    @Test
    fun aValueInTheShapeOfOneStillFadingOutNeverBringsItBack() {
        // Red and green share a shape, as two hands of 2 cards do, and each dissolves what it shows on its own, as a hand's cards do
        val shapes = mapOf(Color.Red to 2, Color.Blue to 3, Color.Green to 2)
        compose.setContent {
            Box(Modifier.background(Color.Black)) {
                Dissolve(colour, Modifier.testTag(TAG), contentKey = { shapes[it] }) { shown ->
                    Dissolve(shown) { Box(Modifier.size(40.dp).background(it)) }
                }
            }
        }
        deal(Color.Blue)

        deal(Color.Green, afterMillis = 16)
        val fading = pixel(0.5f).red
        compose.mainClock.advanceTimeBy(100)

        pixel(0.5f).red.let { assertTrue("red went from $fading to $it", it < fading) }
    }

    @Test
    fun aScreenReaderFindsOnlyTheNewValue() {
        var text by mutableStateOf("Hard 16 vs A")
        compose.setContent { Dissolve(text) { Text(it) } }

        compose.mainClock.autoAdvance = false
        text = "Pair of 8s vs 6"
        compose.mainClock.advanceTimeBy(DISSOLVE_MILLIS / 2L)

        compose.onNodeWithText("Hard 16 vs A").assertDoesNotExist()
        compose.onNodeWithText("Pair of 8s vs 6").assertExists()
    }

    private companion object {
        const val TAG = "dissolve"
    }
}
