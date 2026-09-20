package com.aquigs.sp21ace.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayingCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val courts = listOf(Rank.JACK, Rank.QUEEN, Rank.KING).flatMap { rank -> Suit.entries.map { Card(rank, it) } }

    private fun Color.isWhite() = red > 0.95f && green > 0.95f && blue > 0.95f

    // Every other pixel of the part of the image within these fractions of its width and height
    private fun PixelMap.region(across: ClosedFloatingPointRange<Float>, down: ClosedFloatingPointRange<Float>): List<Color> {
        fun ClosedFloatingPointRange<Float>.pixels(length: Int) = ((start * length).toInt() until (endInclusive * length).toInt() step 2)

        return across.pixels(width).flatMap { x -> down.pixels(height).map { y -> this[x, y] } }
    }

    // On Fomin's 360 by 540 card the frame's outer edge lies 29 in from every side, and it opens for his index left of 60 and
    // above 150, and the same turned about. His index sits above 135 and the figure below 144, so a gap is left between them.
    @Test
    fun everyCourtFigureLeavesItsMarginsAndIndexCornersBlankForTheCardsOwn() {
        var figure by mutableStateOf(courts.first().figure()!!)
        compose.setContent { Image(painterResource(figure), null, Modifier.size(240.dp, 360.dp).background(Color.White).testTag(TAG)) }

        for (court in courts) {
            figure = court.figure()!!
            val pixels = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()

            val middle = pixels.region(0.3f..0.7f, 0.3f..0.7f)
            assertTrue("$court has no figure", middle.count { !it.isWhite() } > middle.size / 3)
            val blank = mapOf(
                "left margin" to pixels.region(0f..0.07f, 0f..1f),
                "right margin" to pixels.region(0.93f..1f, 0f..1f),
                "top margin" to pixels.region(0f..1f, 0f..0.045f),
                "bottom margin" to pixels.region(0f..1f, 0.955f..1f),
                "top-left corner" to pixels.region(0f..0.155f, 0f..0.26f),
                "bottom-right corner" to pixels.region(0.845f..1f, 0.74f..1f),
            )
            blank.forEach { (part, colours) -> assertTrue("$court draws in its $part", colours.all { it.isWhite() }) }
        }
    }

    @Test
    fun everyCourtCardDrawsItsFigureInItsFrameAndNothingBesideIt() {
        var card by mutableStateOf(courts.first())
        compose.setContent { PlayingCard(card, Modifier.height(280.dp).testTag(TAG)) }

        for (court in courts) {
            card = court
            val pixels = compose.onNodeWithTag(TAG).captureToImage().toPixelMap()

            val middle = pixels.region(0.3f..0.7f, 0.3f..0.7f)
            assertTrue("$court has no figure", middle.count { !it.isWhite() } > middle.size / 3)
            // Between the card's edge and the frame, below the index, so a figure drawn too wide would show here
            assertTrue("$court draws beside its frame", pixels.region(0.02f..0.09f, 0.35f..0.95f).all { it.isWhite() })
        }
    }

    private companion object {
        const val TAG = "card"
    }
}
