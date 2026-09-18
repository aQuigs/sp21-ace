package com.aquigs.sp21ace.ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // A closed drawer stays composed just off screen, so being displayed is what tells open from closed
    private val trainerItem
        get() = compose.onNode(hasText(compose.activity.getString(R.string.strategy_trainer)) and isSelectable())

    @Before
    fun setUp() {
        // Edge to edge like MainActivity, or the status bar inset never reaches the composables
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        compose.setContent { Sp21AceTheme(darkTheme = false) { AppShell() } }
    }

    @Test
    fun menuButtonOpensTheDrawerOnTheStrategyTrainer() {
        trainerItem.assertIsNotDisplayed()

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_menu)).performClick()

        compose.onNodeWithText(compose.activity.getString(R.string.basic_strategy)).assertIsDisplayed()
        trainerItem.assertIsDisplayed().assertIsSelected()
    }

    @Test
    fun backOnTheRootOpensTheDrawerAndBackAgainClosesIt() {
        Espresso.pressBack()

        trainerItem.assertIsDisplayed()

        Espresso.pressBack()

        trainerItem.assertIsNotDisplayed()
    }

    @Test
    fun theOpenLightDrawerStopsBelowTheStatusBar() {
        Espresso.pressBack()

        val screen = compose.onRoot().captureToImage().toPixelMap()
        assertTrue(screen[screen.width / 10, 0].luminance() < 0.5f)
    }
}
