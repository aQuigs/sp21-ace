package com.aquigs.sp21ace.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.ui.components.TAP_GUARD_MILLIS
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var settings by mutableStateOf(Settings())
    private var clears = 0
    private var playClears = 0

    private fun string(id: Int) = compose.activity.getString(id)

    private fun showSettings(onOpenColorTheme: () -> Unit = {}, onOpenButtonLocation: () -> Unit = {}) {
        compose.setContent {
            Sp21AceTheme {
                SettingsScreen(
                    settings = settings,
                    onChange = { settings = it },
                    onOpenColorTheme = onOpenColorTheme,
                    onOpenButtonLocation = onOpenButtonLocation,
                    onClearHistory = { clears++ },
                    onClearPlayHistory = { playClears++ },
                    onBack = {},
                )
            }
        }
    }

    private fun switch(title: Int) = compose.onNode(hasText(string(title)) and isToggleable())

    private fun summary(title: Int, value: Int) = compose.onNode(hasText(string(title)) and hasText(string(value)))

    private fun openClearDialog() {
        showSettings()
        compose.onNodeWithText(string(R.string.clear_practice_history)).performScrollTo().performClick()
    }

    @Test
    fun theSwitchesStartAsBlackjackAcesDoAndEachSaysWhetherItsPartShows() {
        showSettings()

        switch(R.string.hand_totals).assertIsOff().performClick()
        switch(R.string.strategy_chart_button).assertIsOn().performClick()
        switch(R.string.streak_meter).performScrollTo().assertIsOn().performClick()

        assertEquals(Settings(handTotals = true, chartButton = false, streakMeter = false), settings)
        summary(R.string.hand_totals, R.string.visible).assertIsDisplayed()
        summary(R.string.strategy_chart_button, R.string.hidden).assertIsDisplayed()
        summary(R.string.streak_meter, R.string.hidden).assertIsDisplayed()
    }

    @Test
    fun soundEffectsStartMutedAfterTheChartButtonAndSayWhenTheyAreOn() {
        showSettings()

        summary(R.string.sound_effects, R.string.muted).assertIsDisplayed()
        // Between the chart button and the Practice section, as in Blackjack Ace
        val row = switch(R.string.sound_effects).getBoundsInRoot()
        assertTrue(switch(R.string.strategy_chart_button).getBoundsInRoot().bottom <= row.top)
        assertTrue(row.bottom <= compose.onNodeWithText(string(R.string.practice)).getBoundsInRoot().top)

        switch(R.string.sound_effects).assertIsOff().performClick()

        assertEquals(Settings(soundEffects = true), settings)
        summary(R.string.sound_effects, R.string.on).assertIsDisplayed()
    }

    @Test
    fun thePlaySectionComesLastAndWarnsOnIncorrectMovesAndShowsTheHintButtonAsBlackjackAceDoes() {
        showSettings()

        switch(R.string.warn_on_incorrect_move).performScrollTo().assertIsOn()
        summary(R.string.warn_on_incorrect_move, R.string.yes).assertIsDisplayed()
        assertTrue(
            compose.onNodeWithText(string(R.string.clear_practice_history)).getBoundsInRoot().bottom <=
                compose.onNodeWithText(string(R.string.play)).getBoundsInRoot().top,
        )

        switch(R.string.warn_on_incorrect_move).performClick()
        switch(R.string.hint_button).performScrollTo().assertIsOn().performClick()

        assertEquals(Settings(warnOnIncorrectMove = false, hintButton = false), settings)
        summary(R.string.warn_on_incorrect_move, R.string.no).assertIsDisplayed()
        summary(R.string.hint_button, R.string.hidden).assertIsDisplayed()
    }

    @Test
    fun theChoiceRowsShowTheirValuesAndOpenTheirPages() {
        var opened = listOf<Int>()
        showSettings(onOpenColorTheme = { opened += R.string.color_theme }, onOpenButtonLocation = { opened += R.string.button_location })

        summary(R.string.color_theme, R.string.theme_system).performClick()
        summary(R.string.button_location, R.string.right).performClick()

        assertEquals(listOf(R.string.color_theme, R.string.button_location), opened)
    }

    @Test
    fun theColorThemePageChangesTheThemeAtOnce() {
        val systemDark = compose.activity.resources.configuration.isNightModeActive
        compose.setContent { Sp21AceTheme(settings.colorTheme) { ColorThemeScreen(settings, onChange = { settings = it }, onBack = {}) } }

        compose.onNodeWithText(string(R.string.theme_system)).assertIsSelected()
        assertEquals(systemDark, compose.pageIsDark())

        for ((choice, dark) in listOf(R.string.theme_dark to true, R.string.theme_light to false, R.string.theme_system to systemDark)) {
            compose.onNodeWithText(string(choice)).performClick().assertIsSelected()

            assertEquals(string(choice), dark, compose.pageIsDark())
        }
    }

    @Test
    fun theButtonLocationPageMarksRightAndChoosingLeftGoesBack() {
        var backs = 0
        compose.setContent { Sp21AceTheme { ButtonLocationScreen(settings, onChange = { settings = it }, onBack = { backs++ }) } }

        compose.onNodeWithText(string(R.string.right)).assertIsSelected()
        compose.onNodeWithText(string(R.string.left)).performClick().assertIsSelected()

        assertEquals(Settings(buttonLocation = ButtonLocation.LEFT), settings)
        assertEquals(1, backs)
    }

    @Test
    fun clearingAsksFirstAndCancellingKeepsTheHistory() {
        openClearDialog()

        compose.onNodeWithText(string(R.string.clear_history_message)).assertIsDisplayed()
        assertEquals(0, clears)

        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.cancel)).performClick()

        compose.onNodeWithText(string(R.string.clear_history_message)).assertDoesNotExist()
        assertEquals(0, clears)
    }

    @Test
    fun clearingThePlayHistoryAsksFirstAndClearsOnlyIt() {
        showSettings()
        compose.onNodeWithText(string(R.string.clear_play_history)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.clear_play_history_message)).assertIsDisplayed()

        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.clear)).performClick()

        compose.onNodeWithText(string(R.string.clear_play_history_message)).assertDoesNotExist()
        assertEquals(listOf(0, 1), listOf(clears, playClears))
    }

    @Test
    fun confirmingClearsTheHistoryAndClosesTheDialog() {
        openClearDialog()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.clear)).performClick()

        compose.onNodeWithText(string(R.string.clear_history_message)).assertDoesNotExist()
        assertEquals(listOf(1, 0), listOf(clears, playClears))
    }
}
