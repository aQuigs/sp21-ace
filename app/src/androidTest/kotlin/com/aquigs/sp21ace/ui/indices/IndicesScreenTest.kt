package com.aquigs.sp21ace.ui.indices

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CardsShown
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.FlashcardOptions
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.ui.texts
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndicesScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var presets by mutableStateOf(IndexPresets.seeded())
    private var options by mutableStateOf(FlashcardOptions())
    private val opened = mutableListOf<String>()
    private val edited = mutableListOf<Deviation>()

    private val h17 = BuiltInIndexSet.BJ_SHOE_H17.name
    private val sixteenVsTen = IndexPresets.seeded().active.entries.first { it.index.deviation.title == "Hard 16 vs 10" }.index

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun show() {
        compose.setContent {
            Sp21AceTheme {
                IndicesScreen(
                    presets = presets,
                    options = options,
                    onPresetsChange = { presets = it },
                    onOptionsChange = { options = it },
                    onOpenPresets = { opened += "presets" },
                    onOpenDeckEstimate = { opened += "deck estimate" },
                    onOpenRounding = { opened += "rounding" },
                    onOpenCardsShown = { opened += "cards shown" },
                    onAddIndex = { opened += "add index" },
                    onEditIndex = { edited += it },
                    onBack = {},
                )
            }
        }
    }

    private fun row(title: String, summary: String) = compose.onNode(hasText(title) and hasText(summary))

    private fun indexSwitch(title: String) = compose.onNode(hasContentDescription(title) and isToggleable())

    @Test
    fun theActivePresetAndItsConventionsShowAndOpenTheirPages() {
        presets = presets.update(h17) { it.copy(deckEstimate = DeckEstimate.PERCENTILE_95, rounding = CountRounding.TRUNCATE) }
        show()

        row(string(R.string.preset), "Shoe, H17").performClick()
        row(string(R.string.deck_estimate), string(R.string.percentile_95)).performClick()
        row(string(R.string.true_count_rounding), string(R.string.truncate)).performClick()
        row(string(R.string.cards_shown), string(R.string.random)).performClick()
        compose.onNodeWithText(string(R.string.add_index)).performScrollTo().performClick()

        assertEquals(listOf("presets", "deck estimate", "rounding", "cards shown", "add index"), opened)
    }

    @Test
    fun theIndicesComeGroupedByPlayInsuranceLast() {
        show()

        val headings = listOf("Stand", "Double", "Split", "Surrender", string(R.string.insurance)).map { title ->
            compose.onNode(hasText(title) and isHeading()).performScrollTo().getBoundsInRoot().top
        }
        assertEquals(headings.sorted(), headings)
        compose.onNode(hasText("Hit") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun aRowSaysItsNumberAndMarksADebatedOneWithADaggerAndAnEditedOneCustom() {
        presets = presets.update(h17) { it.edit(sixteenVsTen.copy(threshold = 1)) }
        show()

        row("Hard 16 vs 9", "Stand at +5 or higher\u00A0†").performScrollTo().assertIsDisplayed()
        row("Hard 15 vs 10", "Stand at +4 or higher").performScrollTo().assertIsDisplayed()
        row("Hard 12 vs 3", "Stand at +1 or higher\u00A0†").performScrollTo().assertIsDisplayed()
        row("Hard 16 vs 10", string(R.string.custom_index, "Stand at +1 or higher")).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theSwitchAloneTurnsAnIndexOffAndOnAgainAndTheRowOpensItsEditor() {
        show()

        indexSwitch("Hard 16 vs 10").performScrollTo().assertIsOn().performClick().assertIsOff()
        assertTrue(sixteenVsTen !in presets.active.enabledIndices)
        assertEquals(emptyList<Deviation>(), edited)

        indexSwitch("Hard 16 vs 10").performClick().assertIsOn()
        assertEquals(IndexPresets.seeded(), presets)

        row("Hard 16 vs 10", "Stand at 0 or higher").performClick()
        assertEquals(listOf(sixteenVsTen.deviation), edited)
    }

    @Test
    fun theFlashcardOptionsShowAndTheSwitchForHandsOf3OrMoreCardsTurnsThemOn() {
        options = FlashcardOptions(CardsShown.PRIORITIZE_WORSE)
        show()

        row(string(R.string.cards_shown), string(R.string.prioritize_worse_indices)).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.multi_card_hands)) and isToggleable()).assertIsOff()
        compose.onNodeWithText(string(R.string.multi_card_flashcards_off)).assertIsDisplayed().performClick()

        assertEquals(FlashcardOptions(CardsShown.PRIORITIZE_WORSE, multiCardHands = true), options)
        compose.onNodeWithText(string(R.string.multi_card_flashcards_on)).assertIsDisplayed()
    }

    @Test
    fun aPresetWithNoIndicesSaysSoUnderAddIndex() {
        presets = presets.add("Mine", from = null)
        show()

        row(string(R.string.preset), "Mine").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.no_indices_in_preset)).performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("Stand") and isHeading()).assertDoesNotExist()
    }

    @Test
    fun theChoicePagesChangeOnlyTheActivePresetOrTheOptionsAndGoBack() {
        var backs by mutableStateOf(0)
        compose.setContent {
            Sp21AceTheme {
                when (backs) {
                    0 -> DeckEstimateScreen(presets, onChange = { presets = it }, onBack = { backs++ })
                    1 -> CountRoundingScreen(presets, onChange = { presets = it }, onBack = { backs++ })
                    else -> CardsShownScreen(options, onChange = { options = it }, onBack = { backs++ })
                }
            }
        }

        compose.onNode(hasText(string(R.string.truncate)) and isSelectable()).assertIsSelected()
        compose.onNodeWithText(string(R.string.round)).performClick()
        compose.onNode(hasText(string(R.string.floor)) and isSelectable()).assertIsSelected()
        compose.onNodeWithText(string(R.string.round)).performClick()
        compose.onNode(hasText(string(R.string.random)) and isSelectable()).assertIsSelected()
        compose.onNodeWithText(string(R.string.prioritize_worse_indices)).performClick()

        assertEquals(3, backs)
        assertEquals(listOf(DeckEstimate.ROUND to CountRounding.ROUND, DeckEstimate.DEFAULT to CountRounding.FLOOR), presets.presets.map { it.deckEstimate to it.rounding })
        assertEquals(FlashcardOptions(CardsShown.PRIORITIZE_WORSE), options)
    }

    @Test
    fun theIntroSaysTheBuiltInNumbersAndThatTheConventionsChangeNone() {
        show()

        val intro = compose.onNode(hasText("Illustrious 18", substring = true)).texts().single()
        assertTrue(intro, "rounded down" in intro && "don't change" in intro && "edit any number" in intro)
    }
}
