package com.aquigs.sp21ace.ui.truecount

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.truecount.DeckPrecision
import com.aquigs.sp21ace.domain.truecount.DecksShown
import com.aquigs.sp21ace.domain.truecount.TrueCountOptions
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrueCountOptionsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var options by mutableStateOf(TrueCountOptions())

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    // Named with its setting, as a screen reader hears it
    private fun chip(setting: Int, value: String) = compose.onNodeWithContentDescription("${string(setting)}, $value").performScrollTo()

    @Before
    fun setUp() = compose.setContent {
        Sp21AceTheme { TrueCountOptionsScreen(options, onChange = { options = it }, onBack = {}) }
    }

    @Test
    fun startsWithSixDecksInHalvesRoundedDownShowingTheDecksLeft() {
        chip(R.string.shoe_size, "6 decks").assertIsSelected()
        chip(R.string.deck_precision, string(R.string.half_decks)).assertIsSelected()
        chip(R.string.decks_shown, string(R.string.decks_left_caption)).assertIsSelected()
        chip(R.string.true_count_rounding, string(R.string.floor)).assertIsSelected()
        chip(R.string.cards_shown, string(R.string.random)).assertIsSelected()
    }

    @Test
    fun eachChipSetsItsOption() {
        chip(R.string.shoe_size, "1 deck").performClick()
        chip(R.string.deck_precision, string(R.string.quarter_decks)).performClick()
        chip(R.string.decks_shown, string(R.string.decks_discarded_caption)).performClick()
        chip(R.string.true_count_rounding, string(R.string.round)).performClick()
        chip(R.string.cards_shown, string(R.string.prioritize_worse_decks)).performClick()

        assertEquals(TrueCountOptions(1, DeckPrecision.QUARTER, CountRounding.ROUND, DecksShown.DISCARDED, CardsShown.PRIORITIZE_WORSE), options)
    }

    @Test
    fun theRoundingsExampleFollowsIt() {
        for ((rounding, words) in listOf(R.string.floor to "1.5 counts as +1, and −1.5 as −2.", R.string.truncate to "1.5 counts as +1, and −1.5 as −1.", R.string.round to "1.5 counts as +2, and −1.5 as −1.")) {
            chip(R.string.true_count_rounding, string(rounding)).performClick()
            compose.onNodeWithText(words).performScrollTo().assertIsDisplayed()
        }
    }
}
