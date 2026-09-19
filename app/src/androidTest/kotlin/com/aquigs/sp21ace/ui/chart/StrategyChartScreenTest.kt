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
import androidx.compose.ui.test.hasContentDescription
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
import com.aquigs.sp21ace.ui.isPlaced
import com.aquigs.sp21ace.ui.onPlacedNodeWithText
import com.aquigs.sp21ace.ui.swipeToNextTab
import com.aquigs.sp21ace.ui.swipeToPreviousTab
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
        compose.onNodeWithText(string(title)).performClick()
    }

    private fun chooseDoubled() = openTab(R.string.already_doubled)

    private fun describes(hand: String, upcard: String) = SemanticsMatcher("describes $hand vs $upcard") { node ->
        node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.any { it.startsWith("$hand vs $upcard: ") }
    } and isPlaced

    // The open page's square that reads [description]
    private fun described(description: String) = compose.onNode(hasContentDescription(description) and isPlaced)

    // Many squares print the same code, so a square is found by the hand and upcard its description opens with
    private fun square(hand: String, upcard: String, code: String) = compose.onNode(describes(hand, upcard) and hasText(code))

    private fun SemanticsNodeInteraction.centreX(): Float = getBoundsInRoot().let { (it.left + it.right).value / 2 }

    @Test
    fun theHardTabPrintsSquaresInChartNotationUnderTheirUpcards() {
        showChart()

        square("14", "4", "S4*").performScrollTo().assertIsDisplayed()
        square("15", "6", "S6\"†").performScrollTo().assertIsDisplayed()
        assertEquals(compose.onPlacedNodeWithText("A").centreX(), square("17", "A", "RH").centreX(), 1f)
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

        described("14 vs 4: Stand, but hit with 4 or more cards or while any 6-7-8 is possible").assertExists()
    }

    @Test
    fun thePairsTabSplitsSevensAgainstASevenButHitsSuitedOnes() {
        showChart()

        openTab(R.string.table_pairs)

        square("7-7", "7", "P$").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun aSwipeMovesToTheNextTableAndBack() {
        showChart()

        compose.swipeToNextTab()

        compose.onNodeWithText(string(R.string.table_soft)).assertIsSelected()
        compose.onNode(describes("A-7", "2")).assertExists()
        compose.onNode(describes("14", "4")).assertDoesNotExist()

        compose.swipeToPreviousTab()

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        compose.onNode(describes("14", "4")).assertExists()
    }

    @Test
    fun aSwipeMovesOnlyAmongTheTablesOfTheChosenGroup() {
        rules = RuleSet.H17_REDOUBLE
        showChart()

        // Hard, Soft and Pairs are the hands not yet doubled; a swipe past Pairs would stretch rather than open a doubled table
        repeat(2) { compose.swipeToNextTab() }

        compose.onNodeWithText(string(R.string.table_pairs)).assertIsSelected()
        compose.onNodeWithText(string(R.string.not_doubled)).assertIsSelected()
        compose.onNode(describes("A-7", "4")).assertDoesNotExist()
    }

    @Test
    fun theLegendFollowsTheTab() {
        showChart()

        compose.onPlacedNodeWithText("Sources still debate this square").performScrollTo().assertIsDisplayed()
        compose.onPlacedNodeWithText("Split").assertDoesNotExist()

        openTab(R.string.table_pairs)

        compose.onPlacedNodeWithText("Split").performScrollTo().assertIsDisplayed()
        compose.onPlacedNodeWithText("Sources still debate this square").assertDoesNotExist()
    }

    @Test
    fun theChoiceSwitchesBetweenTheTablesForHandsNotYetDoubledAndThoseAlreadyDoubled() {
        showChart()

        compose.onNodeWithText(string(R.string.not_doubled)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_pairs)).assertExists()

        chooseDoubled()
        compose.onNodeWithText(string(R.string.already_doubled)).assertIsSelected()

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_pairs)).assertDoesNotExist()
        described("16 vs 10: Rescue").assertExists()

        compose.onNodeWithText(string(R.string.not_doubled)).performClick()

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        described("16 vs 10: Hit").assertExists()
    }

    @Test
    fun choosingTheGroupAlreadyChosenKeepsItsTab() {
        showChart()
        openTab(R.string.table_pairs)

        compose.onNodeWithText(string(R.string.not_doubled)).performClick()

        compose.onNodeWithText(string(R.string.table_pairs)).assertIsSelected()
    }

    @Test
    fun withoutRedoublingADoubledHandStandsOrRescuesOnAHardTableOnly() {
        showChart()

        chooseDoubled()

        compose.onPlacedNodeWithText(string(R.string.doubled_moves)).assertIsDisplayed()
        square("16", "10", "R").performScrollTo().assertIsDisplayed()
        square("12", "2", "S").performScrollTo().assertIsDisplayed()
        described("12 vs 2: Stand").assertExists()
        compose.onNodeWithText(string(R.string.table_soft)).assertDoesNotExist()
    }

    @Test
    fun withRedoublingADoubledHandCanAlsoRedoubleAndHasASoftTable() {
        rules = RuleSet.H17_REDOUBLE
        showChart()

        chooseDoubled()

        compose.onPlacedNodeWithText(string(R.string.doubled_moves_redoubling)).assertIsDisplayed()
        described("11 vs 2: Redouble").assertExists()
        openTab(R.string.table_soft)
        described("A-7 vs 4: Redouble").assertExists()
    }

    @Test
    fun theCaptionNamesTheRules() {
        showChart()

        compose.onPlacedNodeWithText("Dealer stands on soft 17 · 6 decks").assertIsDisplayed()

        rules = RuleSet.H17_REDOUBLE

        compose.onPlacedNodeWithText("Dealer hits soft 17 · Redoubling allowed · 6 decks").assertIsDisplayed()
    }

    @Test
    fun aTabTheNewRulesDontHaveFallsBackToTheFirstOfItsGroup() {
        rules = RuleSet.H17_REDOUBLE
        showChart()
        chooseDoubled()
        openTab(R.string.table_soft)

        rules = RuleSet.S17

        compose.onNodeWithText(string(R.string.already_doubled)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
    }

    @Test
    fun theDoubledHandsStayChosenOnceRedoublingIsAllowed() {
        rules = RuleSet.H17
        showChart()
        chooseDoubled()

        rules = RuleSet.H17_REDOUBLE

        compose.onNodeWithText(string(R.string.already_doubled)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_soft)).assertExists()
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
            chart.plays(ChartTable.HARD).map { it.code } +
            listOf(R.string.not_doubled, R.string.already_doubled, R.string.table_hard, R.string.table_soft, R.string.table_pairs).map(::string)

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
