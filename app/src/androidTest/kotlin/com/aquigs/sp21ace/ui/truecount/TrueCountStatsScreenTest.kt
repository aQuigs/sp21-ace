package com.aquigs.sp21ace.ui.truecount

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.truecount.Decks
import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import com.aquigs.sp21ace.domain.truecount.TrueCountCard
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class TrueCountStatsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.ofEpochMilli(1_789_000_000_000)

    private fun answer(shoe: Int, quarters: Int, runningCount: Int, answer: Int, at: Instant = now) =
        TrueCountAnswer(at, TrueCountCard(shoe, Decks(quarters), CountRounding.FLOOR, runningCount), answer, 2.seconds)

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun show(history: List<TrueCountAnswer>) = compose.setContent {
        Sp21AceTheme { TrueCountStatsScreen(history, onBack = {}, now = { now }) }
    }

    private fun shoe(label: String) = compose.onNode(hasText(label) and isHeading())

    @Test
    fun withNoAnswersItSaysSo() {
        show(emptyList())

        compose.onNodeWithText(string(R.string.no_true_count_answers)).assertIsDisplayed()
    }

    @Test
    fun eachShoeAnsweredHasItsSectionAndSquares() {
        show(listOf(answer(8, 16, 4, 1), answer(8, 16, 4, 2), answer(2, 4, 1, 1)))

        assertTrue(shoe("2 decks").getUnclippedBoundsInRoot().bottom <= shoe("8 decks").getUnclippedBoundsInRoot().top)
        compose.onNodeWithContentDescription("1 deck left, true count +1: 100% right").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("4 decks left, true count +1: 50% right").performScrollTo().assertIsDisplayed()
        // The pad's ends head their columns, and a square without answers says so
        compose.onNodeWithContentDescription("4 decks left, true count +6 or higher: no answers").assertExists()
        compose.onAllNodesWithContentDescription("1 deck left, true count +2: no answers").fetchSemanticsNodes().let { assertEquals(1, it.size) }
    }

    @Test
    fun aLongerPeriodBringsBackAnOlderShoe() {
        show(listOf(answer(6, 16, 4, 1), answer(4, 8, 2, 1, at = now - Duration.ofDays(3))))
        shoe("4 decks").assertDoesNotExist()

        compose.onNodeWithText(string(R.string.week)).performClick()

        shoe("4 decks").assertExists()
        shoe("6 decks").assertExists()
    }
}
