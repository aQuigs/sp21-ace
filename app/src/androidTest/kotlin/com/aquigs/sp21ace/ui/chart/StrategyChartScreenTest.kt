package com.aquigs.sp21ace.ui.chart

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.code
import com.aquigs.sp21ace.domain.strategy.legend
import com.aquigs.sp21ace.ui.assertFitsOnOneLine
import com.aquigs.sp21ace.ui.textLayout
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrategyChartScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var rules by mutableStateOf(RuleSet.S17)

    private fun string(id: Int) = compose.activity.getString(id)

    private fun showChart(onBack: () -> Unit = {}) {
        compose.setContent { Sp21AceTheme { StrategyChartScreen(rules, onBack) } }
    }

    private fun openTab(title: Int) {
        compose.onNodeWithText(string(title)).performScrollTo().performClick()
    }

    // Many squares print the same code, so a square is found by the hand and upcard its description opens with
    private fun square(hand: String, upcard: String, code: String) = compose.onNode(
        SemanticsMatcher("describes $hand vs $upcard") { node ->
            node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.any { it.startsWith("$hand vs $upcard: ") }
        } and hasText(code),
    )

    private fun SemanticsNodeInteraction.centreX(): Float = getBoundsInRoot().let { (it.left + it.right).value / 2 }

    @Test
    fun theHardTabPrintsSquaresInChartNotationUnderTheirUpcards() {
        showChart()

        square("14", "4", "S4*").performScrollTo().assertIsDisplayed()
        square("15", "6", "S6\"†").performScrollTo().assertIsDisplayed()
        assertEquals(compose.onNodeWithText("A").centreX(), square("17", "A", "RH").centreX(), 1f)
    }

    @Test
    fun theChartKeepsTheRowsTheTrainerNeverDeals() {
        showChart()

        // Two ten-value cards are a pair, so the accuracy heatmap has no hard 20 row, but the chart still prints it
        square("20", "2", "S").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aSquareReadsOutAsWords() {
        showChart()

        compose.onNodeWithContentDescription("14 vs 4: Stand, but hit with 4 or more cards or while any 6-7-8 is possible").assertExists()
    }

    @Test
    fun thePairsTabSplitsSevensAgainstASevenButHitsSuitedOnes() {
        showChart()

        openTab(R.string.table_pairs)

        square("7-7", "7", "P$").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theLegendFollowsTheTab() {
        showChart()

        compose.onNodeWithText("Sources still debate this square").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Split").assertDoesNotExist()

        openTab(R.string.table_pairs)

        compose.onNodeWithText("Split").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Sources still debate this square").assertDoesNotExist()
    }

    @Test
    fun theRulesWithoutRedoublingHaveADoubleDownRescueTabThatExplainsItsBlankSquares() {
        showChart()

        openTab(R.string.table_rescue)

        square("16", "10", "R").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("16 vs 10: Rescue").assertExists()
        compose.onNodeWithContentDescription("12 vs 2: Stand, no rescue").assertExists()
        compose.onNodeWithText("Stand, no rescue").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(string(R.string.table_after_double_hard)).assertDoesNotExist()
    }

    @Test
    fun withRedoublingTheRescueTabIsAfterDoublingHardBesideAfterDoublingSoft() {
        rules = RuleSet.H17_REDOUBLE
        showChart()

        compose.onNodeWithText(string(R.string.table_after_double_hard)).assertExists()
        compose.onNodeWithText(string(R.string.table_after_double_soft)).assertExists()
        compose.onNodeWithText(string(R.string.table_rescue)).assertDoesNotExist()
    }

    @Test
    fun theCaptionNamesTheRules() {
        showChart()

        compose.onNodeWithText("Dealer stands on soft 17 · 6 decks").assertIsDisplayed()

        rules = RuleSet.H17_REDOUBLE

        compose.onNodeWithText("Dealer hits soft 17 · Redoubling allowed · 6 decks").assertIsDisplayed()
    }

    @Test
    fun aTabTheNewRulesDontHaveFallsBackToHard() {
        rules = RuleSet.H17_REDOUBLE
        showChart()
        openTab(R.string.table_after_double_soft)

        rules = RuleSet.S17

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
    }

    @Test
    fun doubleDownRescueStaysOpenOnceRedoublingIsAllowedAsTheSameHandsAfterDoublingHard() {
        rules = RuleSet.H17
        showChart()
        openTab(R.string.table_rescue)

        rules = RuleSet.H17_REDOUBLE

        compose.onNodeWithText(string(R.string.table_after_double_hard)).assertIsSelected()
    }

    @Test
    fun atTwiceTheFontSizeOnANarrowPhoneNoCodeOrLabelIsCutShort() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                Sp21AceTheme { Box(Modifier.width(360.dp)) { StrategyChartScreen(RuleSet.S17, onBack = {}) } }
            }
        }
        val chart = StrategyCharts.forRules(RuleSet.S17)
        val hands = chart.hands(ChartTable.HARD)
        val gridText = Upcard.entries.map { it.label } + hands + chart.legend(ChartTable.HARD).map { it.symbol } +
            hands.flatMap { hand -> Upcard.entries.mapNotNull { chart.play(ChartTable.HARD, hand, it)?.code } }

        compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.textLayout() }
            .filter { it.layoutInput.text.text in gridText }
            .forEach { it.assertFitsOnOneLine() }
    }

    @Test
    fun theBackArrowGoesBack() {
        var backs = 0
        showChart(onBack = { backs++ })

        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        assertEquals(1, backs)
    }
}
