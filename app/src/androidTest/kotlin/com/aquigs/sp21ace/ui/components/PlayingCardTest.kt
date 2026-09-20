package com.aquigs.sp21ace.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
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

    // Every other pixel of the part of the card within these fractions of its width and height
    private fun PixelMap.region(across: ClosedFloatingPointRange<Float>, down: ClosedFloatingPointRange<Float>): List<Color> {
        fun ClosedFloatingPointRange<Float>.pixels(length: Int) = ((start * length).toInt() until (endInclusive * length).toInt() step 2)

        return across.pixels(width).flatMap { x -> down.pixels(height).map { y -> this[x, y] } }
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
            // Between the card's edge and the frame, below the index, where the outline of the card the figure was drawn on would run
            assertTrue("$court draws beside its frame", pixels.region(0.02f..0.09f, 0.35f..0.95f).all { it.isWhite() })
        }
    }

    private companion object {
        const val TAG = "card"
    }
}
