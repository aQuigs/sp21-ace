package com.aquigs.sp21ace.ui.chart

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrategyChartScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int) = compose.activity.getString(id)

    private fun showChart(rules: RuleSet = RuleSet.S17, onBack: () -> Unit = {}) {
        compose.setContent { Sp21AceTheme { StrategyChartScreen(StrategyCharts.forRules(rules), rules, onBack) } }
    }

    private fun openTab(title: Int) {
        compose.onNodeWithText(string(title)).performScrollTo().performClick()
    }

    // Many squares print the same code, so a square is found by the hand and upcard its description names
    private fun square(hand: String, upcard: String, code: String) =
        compose.onNode(hasContentDescription(compose.activity.getString(R.string.square_description, hand, upcard, code)) and hasText(code))

    @Test
    fun theHardTabPrintsSquaresInChartNotationAndDaggersDebatedOnes() {
        showChart()

        square("14", "4", "S4*").performScrollTo().assertIsDisplayed()
        square("15", "6", "S6\"†").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun thePairsTabSplitsSevensAgainstASevenButHitsSuitedOnes() {
        showChart()

        openTab(R.string.table_pairs)

        square("7-7", "7", "P$").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theLegendListsOnlyWhatTheTabUses() {
        showChart()

        compose.onNodeWithText(string(R.string.legend_debated)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.move_split)).assertDoesNotExist()

        openTab(R.string.table_pairs)

        compose.onNodeWithText(string(R.string.move_split)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.legend_debated)).assertDoesNotExist()
    }

    @Test
    fun theRulesWithoutRedoublingHaveADoubleDownRescueTab() {
        showChart(RuleSet.S17)

        openTab(R.string.table_rescue)

        square("16", "10", "R").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.table_after_double_hard)).assertDoesNotExist()
    }

    @Test
    fun redoublingTradesTheRescueTabForTheAfterDoublingTables() {
        showChart(RuleSet.H17_REDOUBLE)

        compose.onNodeWithText(string(R.string.table_after_double_hard)).assertExists()
        compose.onNodeWithText(string(R.string.table_after_double_soft)).assertExists()
        compose.onNodeWithText(string(R.string.table_rescue)).assertDoesNotExist()
    }

    @Test
    fun theCaptionNamesTheRules() {
        showChart(RuleSet.S17)

        compose.onNodeWithText("Dealer stands on soft 17 · 6 decks").assertIsDisplayed()
    }

    @Test
    fun theBackArrowGoesBack() {
        var backs = 0
        showChart(onBack = { backs++ })

        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(1, backs)
    }
}
