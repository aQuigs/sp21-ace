package com.aquigs.sp21ace.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var settings by mutableStateOf(Settings())

    private fun string(id: Int) = compose.activity.getString(id)

    private fun showSettings(onOpenColorTheme: () -> Unit = {}, onOpenButtonLocation: () -> Unit = {}, onClearHistory: () -> Unit = {}) {
        compose.setContent {
            Sp21AceTheme {
                SettingsScreen(
                    settings = settings,
                    onChange = { settings = it },
                    onOpenColorTheme = onOpenColorTheme,
                    onOpenButtonLocation = onOpenButtonLocation,
                    onClearHistory = onClearHistory,
                    onBack = {},
                )
            }
        }
    }

    private fun switch(title: Int) = compose.onNode(hasText(string(title)) and isToggleable())

    private fun openClearDialog() = compose.onNodeWithText(string(R.string.clear_practice_history)).performScrollTo().performClick()

    // The page's margin, clear of any row's text
    private fun pageIsDark(): Boolean {
        val page = compose.onRoot().captureToImage().toPixelMap()
        return page[4, page.height * 3 / 4].luminance() < 0.5f
    }

    @Test
    fun theSwitchesStartAsBlackjackAcesDoAndEachShowsOrHidesItsPart() {
        showSettings()

        switch(R.string.hand_totals).assertIsOff().performClick().assertIsOn()
        switch(R.string.strategy_chart_button).assertIsOn().performClick().assertIsOff()
        switch(R.string.streak_meter).performScrollTo().assertIsOn().performClick().assertIsOff()

        assertEquals(Settings(handTotals = true, chartButton = false, streakMeter = false), settings)
        compose.onNode(hasText(string(R.string.hand_totals)) and hasText(string(R.string.visible))).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.streak_meter)) and hasText(string(R.string.hidden))).assertIsDisplayed()
    }

    @Test
    fun theChoiceRowsShowTheirValuesAndOpenTheirPages() {
        var opened = listOf<Int>()
        showSettings(onOpenColorTheme = { opened += R.string.color_theme }, onOpenButtonLocation = { opened += R.string.button_location })

        compose.onNode(hasText(string(R.string.color_theme)) and hasText(string(R.string.theme_system))).performClick()
        compose.onNode(hasText(string(R.string.button_location)) and hasText(string(R.string.right))).performClick()

        assertEquals(listOf(R.string.color_theme, R.string.button_location), opened)
    }

    @Test
    fun theColorThemePageChangesTheThemeAtOnce() {
        val systemDark = compose.activity.resources.configuration.isNightModeActive
        var backs = 0
        compose.setContent { Sp21AceTheme(settings.colorTheme) { ColorThemeScreen(settings, onChange = { settings = it }, onBack = { backs++ }) } }

        compose.onNodeWithText(string(R.string.theme_system)).assertIsSelected()
        assertEquals(systemDark, pageIsDark())

        for ((choice, dark) in listOf(R.string.theme_dark to true, R.string.theme_light to false, R.string.theme_system to systemDark)) {
            compose.onNodeWithText(string(choice)).performClick().assertIsSelected()

            assertEquals(string(choice), dark, pageIsDark())
        }
        assertEquals(3, backs)
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
        var clears = 0
        showSettings(onClearHistory = { clears++ })

        openClearDialog()

        compose.onNodeWithText(string(R.string.clear_history_message)).assertIsDisplayed()
        assertEquals(0, clears)

        compose.onNodeWithText(string(R.string.cancel)).performClick()

        compose.onNodeWithText(string(R.string.clear_history_message)).assertDoesNotExist()
        assertEquals(0, clears)
    }

    @Test
    fun confirmingClearsTheHistoryAndClosesTheDialog() {
        var clears = 0
        showSettings(onClearHistory = { clears++ })

        openClearDialog()
        compose.onNodeWithText(string(R.string.clear)).performClick()

        compose.onNodeWithText(string(R.string.clear_history_message)).assertDoesNotExist()
        assertEquals(1, clears)
    }
}
