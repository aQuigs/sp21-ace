package com.aquigs.sp21ace

import android.app.UiModeManager
import android.os.Build
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.data.PracticeHistoryStore
import com.aquigs.sp21ace.data.SettingsStore
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.ui.accuracy.cardTexts
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    // These tests use the app's real rules, settings and history files, so put back what a fresh install opens with
    @After
    fun tearDown() {
        TableRulesStore(compose.activity).save(TableRules())
        SettingsStore(compose.activity).save(Settings())
        PracticeHistoryStore.forApp(compose.activity).clear()
        // The system keeps a chosen theme for the app, apart from the settings file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            compose.activity.getSystemService(UiModeManager::class.java).setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
        }
    }

    @Test
    fun opensOnTheStrategyTrainerWithAHandDealt() {
        // The closed drawer also carries the Strategy Trainer label, so match only the app bar's heading
        val title = hasText(compose.activity.getString(R.string.strategy_trainer)) and isHeading()

        compose.onNode(title).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.face_down_card)).assertIsDisplayed()
    }

    @Test
    fun keepsTheHandTheVerdictAndThePreviousHandWhenRecreated() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.move_hit)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.action)).assertIsDisplayed()
        val answered = everythingOnScreen()

        // What a theme or font size change does to the activity
        compose.activityRule.scenario.recreate()

        assertEquals(answered, everythingOnScreen())
    }

    @Test
    fun keepsTheOpenChartAndItsTabWhenRecreated() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_strategy_chart)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.table_pairs)).performClick()

        compose.activityRule.scenario.recreate()

        compose.onNode(hasText(compose.activity.getString(R.string.strategy_chart)) and isHeading()).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.table_pairs)).assertIsSelected()
    }

    @Test
    fun keepsTheChosenTableRulesWhenRecreated() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_menu)).performClick()
        compose.onNode(hasText(compose.activity.getString(R.string.table_rules)) and isSelectable()).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.soft_17)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.dealer_hits)).performClick()

        compose.activityRule.scenario.recreate()

        compose.onNode(hasText(compose.activity.getString(R.string.soft_17)) and hasText(compose.activity.getString(R.string.dealer_hits))).assertIsDisplayed()
    }

    @Test
    fun keepsTheChosenColorThemeWhenRecreated() {
        // The theme the system isn't using, so only a theme the app applies itself shows
        val systemDark = compose.activity.resources.configuration.isNightModeActive
        val chosen = compose.activity.getString(if (systemDark) R.string.theme_light else R.string.theme_dark)

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_menu)).performClick()
        compose.onNode(hasText(compose.activity.getString(R.string.settings)) and isSelectable()).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.color_theme)).performClick()
        compose.onNodeWithText(chosen).performClick()

        compose.activityRule.scenario.recreate()

        compose.onNode(hasText(compose.activity.getString(R.string.color_theme)) and hasText(chosen)).assertIsDisplayed()
        val screen = compose.onRoot().captureToImage().toPixelMap()
        // The page's margin, clear of any row's text
        assertEquals(!systemDark, screen[4, screen.height * 3 / 4].luminance() < 0.5f)
    }

    @Test
    fun anAnswerStillCountsOnAccuracyWhenRecreated() {
        val before = answersOnAccuracy()

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.move_hit)).performClick()
        compose.activityRule.scenario.recreate()

        assertEquals(before + 1, answersOnAccuracy())
    }

    // Counted on the All tab, since the hand dealt is random, and against a count taken first, since the app's own history may
    // already hold answers from today
    private fun answersOnAccuracy(): Int {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_menu)).performClick()
        compose.onNode(hasText(compose.activity.getString(R.string.accuracy)) and isSelectable()).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.all_hands)).performClick()

        val correct = compose.activity.getString(R.string.correct)
        val incorrect = compose.activity.getString(R.string.incorrect)
        val overall = compose.cardTexts(compose.activity.getString(R.string.overall), correct)
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()

        // Each count sits before its label
        return listOf(correct, incorrect).sumOf { overall[overall.indexOf(it) - 1].toInt() }
    }

    // Hands are dealt at random, so compare every card, bar and recap word rather than expected values
    private fun everythingOnScreen(): List<String> =
        compose.onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true).fetchSemanticsNodes().flatMap { node ->
            node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() } +
                node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
        }
}
