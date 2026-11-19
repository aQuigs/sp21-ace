package com.aquigs.sp21ace.ui.indices

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.ui.components.TAP_GUARD_MILLIS
import com.aquigs.sp21ace.ui.texts
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndexPresetsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val seeded = IndexPresets.seeded()
    private val h17 = BuiltInIndexSet.BJ_SHOE_H17.name
    private val s17 = BuiltInIndexSet.BJ_SHOE_S17.name
    private var presets by mutableStateOf(seeded)
    private var backs = 0

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun show() {
        compose.setContent { Sp21AceTheme { IndexPresetsScreen(presets, onChange = { presets = it }, onBack = { backs++ }) } }
    }

    private fun preset(name: String) = compose.onNode(hasText(name) and isSelectable())

    private fun openMenu(name: String, action: Int) {
        compose.onNodeWithContentDescription(string(R.string.more_options_for, name)).performClick()
        compose.onNodeWithText(string(action)).performClick()
        // The dialog it opens waits out a double tap
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
    }

    // The dialog's choice, not the preset's row of the same name behind it
    private fun startFrom(label: String) = compose.onNode(hasText(label) and isSelectable() and hasAnyAncestor(isDialog()))

    private fun nameField() = compose.onNode(hasSetTextAction())

    private fun button(label: Int) = compose.onNode(hasText(string(label)) and hasClickAction())

    @Test
    fun eachPresetSaysItsGameConventionsAndIndicesAndTheActiveOneIsChosen() {
        presets = seeded.update(s17) { it.copy(deckEstimate = DeckEstimate.PERCENTILE_95, rounding = CountRounding.ROUND, entries = it.entries.take(1)) }
        show()

        assertEquals(listOf("Shoe, H17", "Blackjack, 22 indices\nDeck estimate: Truncate\nRounding: Floor"), preset("Shoe, H17").assertIsSelected().texts())
        assertEquals(listOf("Shoe, S17", "Blackjack, 1 index\nDeck estimate: 95th percentile\nRounding: Round"), preset("Shoe, S17").assertIsNotSelected().texts())
    }

    @Test
    fun choosingAPresetMakesItActiveAndGoesBack() {
        show()

        preset("Shoe, S17").performClick()

        assertEquals(s17, presets.activeId)
        assertEquals(1, backs)
    }

    @Test
    fun aNewPresetStartsFromTheSetChosenOrEmptyAndBecomesActive() {
        show()

        compose.onNodeWithText(string(R.string.new_preset)).performClick()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        button(R.string.create).assertIsNotEnabled()
        nameField().performTextReplacement("  Mine  ")
        startFrom(string(R.string.empty_preset)).performClick()
        button(R.string.create).assertIsEnabled().performClick()

        assertEquals(listOf(h17, s17), presets.presets.take(2).map { it.id })
        assertEquals(IndexPreset(presets.activeId, "Mine", seeded.active.game, basedOn = null, entries = emptyList()), presets.active)
        preset("Mine").assertIsSelected()
        assertEquals(0, backs)

        compose.onNodeWithText(string(R.string.new_preset)).performClick()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        nameField().performTextReplacement("Mine from S17")
        // The first built-in set unless another is chosen
        startFrom(BuiltInIndexSet.BJ_SHOE_H17.title).assertIsSelected()
        startFrom(BuiltInIndexSet.BJ_SHOE_S17.title).performClick()
        button(R.string.create).performClick()

        assertEquals(IndexPreset.from(BuiltInIndexSet.BJ_SHOE_S17, presets.activeId, "Mine from S17"), presets.active)
    }

    @Test
    fun cancellingANewPresetAddsNothing() {
        show()

        compose.onNodeWithText(string(R.string.new_preset)).performClick()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        nameField().performTextReplacement("Mine")
        button(R.string.cancel).performClick()

        assertEquals(seeded, presets)
        compose.onNodeWithText(string(R.string.create)).assertDoesNotExist()
    }

    @Test
    fun renamingChangesOnlyTheNameAndCancellingKeepsIt() {
        show()

        openMenu("Shoe, S17", R.string.rename)
        nameField().performTextReplacement("Renamed")
        button(R.string.cancel).performClick()
        assertEquals(seeded, presets)

        openMenu("Shoe, S17", R.string.rename)
        nameField().performTextReplacement("Renamed")
        button(R.string.save).performClick()

        assertEquals(seeded.rename(s17, "Renamed"), presets)
        preset("Renamed").assertIsDisplayed()
    }

    @Test
    fun duplicatingCopiesThePresetJustAfterItAsTheActiveOne() {
        show()

        openMenu("Shoe, S17", R.string.duplicate)

        assertEquals(listOf(h17, s17), presets.presets.take(2).map { it.id })
        assertEquals(seeded.presets[1].copy(id = presets.activeId, name = "Shoe, S17 copy"), presets.active)
        preset("Shoe, S17 copy").assertIsSelected()
    }

    @Test
    fun aNameAnotherPresetHasCannotBeGivenThoughAPresetKeepsItsOwn() {
        show()

        compose.onNodeWithText(string(R.string.new_preset)).performClick()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        nameField().performTextReplacement(" shoe, s17 ")
        compose.onNodeWithText(string(R.string.preset_name_taken)).assertIsDisplayed()
        button(R.string.create).assertIsNotEnabled()
        nameField().performTextReplacement("Shoe, S17 mine")
        compose.onNodeWithText(string(R.string.preset_name_taken)).assertDoesNotExist()
        button(R.string.cancel).performClick()

        openMenu("Shoe, S17", R.string.rename)
        nameField().performTextReplacement("Shoe, h17")
        button(R.string.save).assertIsNotEnabled()
        nameField().performTextReplacement("SHOE, S17")
        button(R.string.save).assertIsEnabled().performClick()

        assertEquals(seeded.rename(s17, "SHOE, S17"), presets)
    }

    @Test
    fun aSecondCopyIsNumberedAndALongNameIsCopiedWhole() {
        presets = seeded.rename(s17, "🂡".repeat(20))
        show()

        openMenu("Shoe, H17", R.string.duplicate)
        openMenu("Shoe, H17", R.string.duplicate)
        openMenu("🂡".repeat(20), R.string.duplicate)

        assertEquals(listOf("Shoe, H17", "Shoe, H17 copy 2", "Shoe, H17 copy", "🂡".repeat(20), "🂡".repeat(20) + " copy"), presets.presets.map { it.name })
    }

    @Test
    fun deletingAsksFirstAndTheLastPresetCannotBeDeleted() {
        show()
        val message = string(R.string.delete_preset_message, "Shoe, H17")

        openMenu("Shoe, H17", R.string.delete)
        compose.onNodeWithText(message).assertIsDisplayed()
        button(R.string.cancel).performClick()
        assertEquals(seeded, presets)

        openMenu("Shoe, H17", R.string.delete)
        button(R.string.delete).performClick()

        assertEquals(seeded.delete(h17), presets)
        preset("Shoe, H17").assertDoesNotExist()

        compose.onNodeWithContentDescription(string(R.string.more_options_for, "Shoe, S17")).performClick()
        compose.onNodeWithText(string(R.string.delete)).assertIsNotEnabled()
    }
}
