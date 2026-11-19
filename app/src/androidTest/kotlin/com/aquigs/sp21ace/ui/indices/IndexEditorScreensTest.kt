package com.aquigs.sp21ace.ui.indices

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.HandKind
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.ui.components.TAP_GUARD_MILLIS
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndexEditorScreensTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val seeded = IndexPresets.seeded()
    private var presets by mutableStateOf(seeded)
    private var changes = 0
    private var backs = 0

    // Shoe H17 stands on hard 16 vs 10 at 0 or higher
    private val sixteenVsTen = seeded.active.entries.first { it.index.deviation.title == "Hard 16 vs 10" }.index

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun change(next: IndexPresets) {
        presets = next
        changes++
    }

    private fun showAdd(start: IndexPresets = seeded) {
        presets = start
        compose.setContent { Sp21AceTheme { AddIndexScreen(presets, ::change, onBack = { backs++ }) } }
    }

    private fun showEdit(deviation: Deviation = sixteenVsTen.deviation) {
        compose.setContent { Sp21AceTheme { EditIndexScreen(presets, deviation, ::change, onBack = { backs++ }) } }
    }

    private fun chip(field: Int, label: String) = compose.onNode(hasContentDescription("${string(field)}, $label") and isSelectable())

    private fun kind(label: String) = chip(R.string.index_hand, label)

    private fun total(label: String) = chip(R.string.total, label)

    private fun upcard(label: String) = chip(R.string.dealers_upcard, label)

    private fun play(label: String) = chip(R.string.index_play, label)

    private fun direction(label: String) = chip(R.string.direction, label)

    private fun choose(vararg chips: SemanticsNodeInteraction) = chips.forEach { it.performScrollTo().performClick() }

    private fun slideTo(number: Int) =
        compose.onNodeWithContentDescription(string(R.string.true_count)).performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(number.toFloat()) }

    private fun addButton(): SemanticsNodeInteraction = compose.onNode(hasText(string(R.string.add_index)) and hasClickAction()).performScrollTo()

    @Test
    fun anIndexWithEveryFieldChosenIsAddedSwitchedOnAtTheEndAndThePageGoesBack() {
        showAdd()

        choose(kind("Hard total"), total("13"), upcard("2"), play("Hit"), direction("or lower"))
        slideTo(-1)
        compose.onNodeWithText("Hit at −1 or lower").assertIsDisplayed()
        addButton().performClick()

        val hit = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 13), Upcard.TWO, DeviationPlay.HIT), -1, IndexDirection.AT_OR_BELOW)
        assertEquals(seeded.active.entries + IndexEntry(hit), presets.active.entries)
        assertEquals(listOf(1, 1), listOf(changes, backs))
    }

    @Test
    fun nothingCanBeAddedUntilTheHandUpcardAndPlayAreChosen() {
        showAdd()

        addButton().assertIsNotEnabled()
        choose(total("13"), upcard("2"))
        addButton().assertIsNotEnabled()
        // Shoe H17 stands on 13 vs 2 already, but doesn't double it
        choose(play("Double"))
        addButton().assertIsEnabled()
    }

    @Test
    fun aHandUpcardAndPlayThePresetAlreadyHasCannotBeAddedAgain() {
        showAdd()

        choose(total("16"), upcard("10"), play("Stand"))

        compose.onNodeWithText(string(R.string.index_already_listed, "Hard 16 vs 10, Stand")).performScrollTo().assertIsDisplayed()
        addButton().assertIsNotEnabled()

        // Another play on the same hand is another index
        choose(play("Surrender"))

        compose.onNodeWithText(string(R.string.index_already_listed, "Hard 16 vs 10, Stand")).assertDoesNotExist()
        addButton().assertIsEnabled()
    }

    @Test
    fun eachKindOfHandOffersItsValuesAndOnlyThePlaysItCanMake() {
        showAdd()

        for (label in listOf("Hit", "Stand", "Double", "Surrender")) play(label).assertExists()
        play("Split").assertDoesNotExist()
        total("5").assertExists()
        total("19").assertExists()
        total("4").assertDoesNotExist()
        total("20").assertDoesNotExist()

        choose(kind("Soft total"))
        total("13").assertExists()
        total("20").assertExists()
        total("12").assertDoesNotExist()
        play("Surrender").assertDoesNotExist()
        play("Split").assertDoesNotExist()

        choose(kind("Pair"))
        chip(R.string.pair_of, "A").assertExists()
        chip(R.string.pair_of, "10").assertExists()
        play("Split").assertExists()
        play("Surrender").assertExists()
    }

    @Test
    fun anotherKindClearsTheValueChosen() {
        showAdd()

        choose(total("16"))
        total("16").assertIsSelected()
        choose(kind("Soft total"))

        total("16").assertIsNotSelected()
    }

    @Test
    fun insuranceAsksOnlyForItsNumber() {
        showAdd(seeded.add("Mine", from = null))

        choose(kind(string(R.string.insurance)))
        compose.onNodeWithText(string(R.string.dealers_upcard)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.index_play)).assertDoesNotExist()
        slideTo(3)
        compose.onNodeWithText("Insurance at +3 or higher").assertIsDisplayed()
        addButton().performClick()

        assertEquals(listOf(IndexEntry(PlayIndex(Deviation.Insurance, 3))), presets.active.entries)
    }

    @Test
    fun backLeavesAnIndexHalfChosenUnadded() {
        showAdd()

        choose(total("13"), upcard("2"), play("Hit"))
        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(listOf(0, 1), listOf(changes, backs))
        assertEquals(seeded, presets)
    }

    @Test
    fun anEditSavesTheNumberAndDirectionKeepingTheSwitchAndGoesBack() {
        presets = seeded.update(seeded.activeId) { it.switch(sixteenVsTen.deviation, enabled = false) }
        showEdit()

        compose.onNode(hasText("Hard 16 vs 10") and hasText("Stand")).assertIsDisplayed()
        compose.onNodeWithText("Stand at 0 or higher").assertIsDisplayed()
        direction("or higher").assertIsSelected()
        choose(direction("or lower"))
        slideTo(-2)
        compose.onNodeWithText("Stand at −2 or lower").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.save)).performScrollTo().performClick()

        val entry = presets.active.entries.first { it.index.deviation == sixteenVsTen.deviation }
        assertEquals(IndexEntry(sixteenVsTen.copy(threshold = -2, direction = IndexDirection.AT_OR_BELOW), enabled = false), entry)
        assertEquals(listOf(1, 1), listOf(changes, backs))
    }

    @Test
    fun backLeavesAnEditUnsaved() {
        showEdit()

        choose(direction("or lower"))
        slideTo(4)
        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(listOf(0, 1), listOf(changes, backs))
        assertEquals(seeded, presets)
    }

    @Test
    fun deletingAsksFirstAndCancellingKeepsTheIndex() {
        showEdit()
        val message = string(R.string.delete_index_message, "Hard 16 vs 10", "Stand at 0 or higher")

        compose.onNodeWithText(string(R.string.delete_index)).performScrollTo().performClick()
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.cancel)).performClick()

        compose.onNodeWithText(message).assertDoesNotExist()
        assertEquals(listOf(0, 0), listOf(changes, backs))

        compose.onNodeWithText(string(R.string.delete_index)).performClick()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.delete)).performClick()

        assertEquals(seeded.active.entries.filter { it.index != sixteenVsTen }, presets.active.entries)
        assertEquals(listOf(1, 1), listOf(changes, backs))
    }
}
