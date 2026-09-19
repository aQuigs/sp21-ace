package com.aquigs.sp21ace.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.Sp21AceApp
import com.aquigs.sp21ace.data.HandCustomizationStore
import com.aquigs.sp21ace.data.PracticeHistoryStore
import com.aquigs.sp21ace.data.SettingsStore
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.dealing.HAND_TYPES
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.dealing.HandPicker
import com.aquigs.sp21ace.domain.dealing.HandType
import com.aquigs.sp21ace.domain.dealing.type
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.accuracy.accuracyCardTexts
import com.aquigs.sp21ace.ui.accuracy.cardTexts
import com.aquigs.sp21ace.ui.accuracy.square
import com.aquigs.sp21ace.ui.hands.handTypeSwitch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Hard 16 vs A is a hit when the dealer stands on soft 17, and a surrender when the dealer hits
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))

    // Their own files, so the tests never overwrite the rules, the customization, the settings or the history the app itself saved
    private val store by lazy { TableRulesStore(compose.activity, "table_rules_app_shell_test") }
    private val handsStore by lazy { HandCustomizationStore(compose.activity, "customize_hands_app_shell_test") }
    private val settingsStore by lazy { SettingsStore(compose.activity, "settings_app_shell_test") }
    private val historyStore by lazy { PracticeHistoryStore(File(compose.activity.filesDir, "practice_history_app_shell_test.txt")) }

    private var handsDealt = 0

    // Deals 16 vs A first and a pair of 8s after every answer, unless a test deals through the picker instead
    private var dealHand: (HandPicker, List<PracticeAnswer>) -> TrainerHand = { _, _ -> if (handsDealt++ == 0) sixteenVsAce else eightsVsSix }

    private fun string(id: Int) = compose.activity.getString(id)

    // A closed drawer stays composed just off screen, so being displayed is what tells open from closed
    private fun drawerItem(title: Int) = compose.onNode(hasText(string(title)) and isSelectable())

    // The drawer carries the same names, so match only the app bar's heading
    private fun appBarTitle(title: Int) = compose.onNode(hasText(string(title)) and isHeading())

    @Before
    fun setUp() {
        store.save(TableRules())
        handsStore.save(HandCustomization())
        // Light whatever the emulator's own theme, as the drawer's status bar check expects
        settingsStore.save(Settings(colorTheme = ColorTheme.LIGHT))
        historyStore.clear()

        compose.setContent { Sp21AceApp(store, historyStore, handsStore, settingsStore, deal = { picker, history -> dealHand(picker, history) }) }
    }

    @After
    fun tearDown() {
        store.save(TableRules())
        handsStore.save(HandCustomization())
        settingsStore.save(Settings())
        historyStore.clear()
    }

    private fun openFromDrawer(title: Int) {
        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()
        drawerItem(title).performClick()
    }

    private fun chooseDealerHits() {
        compose.onNodeWithText(string(R.string.soft_17)).performClick()
        compose.onNodeWithText(string(R.string.dealer_hits)).performClick()
    }

    private fun overallCard() = compose.onScreen.cardTexts(string(R.string.overall), string(R.string.correct))

    private fun overallFigures(percentage: Double, correct: Int, incorrect: Int) =
        compose.activity.accuracyCardTexts(R.string.overall, compose.activity.getString(R.string.percentage, percentage), correct, incorrect)

    @Test
    fun menuButtonOpensTheDrawerOnTheStrategyTrainerWithItsItemsInBlackjackAcesOrder() {
        drawerItem(R.string.strategy_trainer).assertIsNotDisplayed()

        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()

        compose.onNodeWithText(string(R.string.basic_strategy)).assertIsDisplayed()
        drawerItem(R.string.strategy_trainer).assertIsSelected()
        val items = listOf(R.string.strategy_trainer, R.string.table_rules, R.string.strategy_chart, R.string.customize_hands, R.string.accuracy, R.string.settings)
            .map { drawerItem(it).assertIsDisplayed().getBoundsInRoot() }
        items.zipWithNext().forEach { (above, below) -> assertTrue(below.top >= above.bottom) }
    }

    @Test
    fun theTrainersAnswersCountOnAccuracyUnderTheirKindOfHand() {
        // Right on hard 16 vs A, then wrong on the pair of 8s
        compose.onNodeWithContentDescription(string(R.string.move_hit)).performClick()
        compose.onNodeWithContentDescription(string(R.string.move_stand)).performClick()

        openFromDrawer(R.string.accuracy)

        appBarTitle(R.string.accuracy).assertIsDisplayed()
        assertEquals(overallFigures(percentage = 100.0, correct = 1, incorrect = 0), overallCard())

        compose.onNodeWithText(string(R.string.table_pairs)).performClick()

        assertEquals(overallFigures(percentage = 0.0, correct = 0, incorrect = 1), overallCard())
    }

    @Test
    fun clearingThePracticeHistoryResetsTheStreakMeterAccuracyAndItsHeatmap() {
        // Right on hard 16 vs A
        compose.onNodeWithContentDescription(string(R.string.move_hit)).performClick()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.streak_count, 1)).assertIsDisplayed()

        openFromDrawer(R.string.settings)
        compose.onNodeWithText(string(R.string.clear_practice_history)).performClick()
        compose.onNodeWithText(string(R.string.clear)).performClick()
        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        compose.onNodeWithContentDescription(compose.activity.getString(R.string.streak_count, 0)).assertIsDisplayed()

        openFromDrawer(R.string.accuracy)

        assertEquals(compose.activity.accuracyCardTexts(R.string.overall, string(R.string.no_data), correct = 0, incorrect = 0), overallCard())
        compose.onScreen.square("16 vs A: Hit, no answers", "H")
    }

    @Test
    fun backOnTheRootOpensTheDrawerAndBackAgainClosesIt() {
        Espresso.pressBack()

        drawerItem(R.string.strategy_trainer).assertIsDisplayed()

        Espresso.pressBack()

        drawerItem(R.string.strategy_trainer).assertIsNotDisplayed()
    }

    @Test
    fun theOpenLightDrawerStopsBelowTheStatusBar() {
        Espresso.pressBack()

        val screen = compose.onRoot().captureToImage().toPixelMap()
        assertTrue(screen[screen.width / 10, 0].luminance() < 0.5f)
    }

    @Test
    fun theChartTileOpensTheChartAndBackReturnsToTheSameHand() {
        compose.onNodeWithContentDescription(string(R.string.move_hit)).performClick()
        compose.onNodeWithContentDescription(string(R.string.open_strategy_chart)).performClick()

        appBarTitle(R.string.strategy_chart).assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertDoesNotExist()

        Espresso.pressBack()

        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertIsDisplayed()
        drawerItem(R.string.strategy_trainer).assertIsNotDisplayed()
    }

    @Test
    fun theDrawerOpensTheChartAndItsBackArrowReturnsToTheTrainer() {
        openFromDrawer(R.string.strategy_chart)

        appBarTitle(R.string.strategy_chart).assertIsDisplayed()
        drawerItem(R.string.strategy_chart).assertIsNotDisplayed()

        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        appBarTitle(R.string.strategy_trainer).assertIsDisplayed()
        compose.onNodeWithContentDescription("9 of clubs").assertIsDisplayed()
    }

    @Test
    fun twoBacksBeforeARedrawStopAtTheRootScreen() {
        compose.onNodeWithContentDescription(string(R.string.open_strategy_chart)).performClick()
        val tapBackArrow = compose.onNodeWithContentDescription(string(R.string.back)).fetchSemanticsNode().config[SemanticsActions.OnClick].action

        // A double tap on the arrow while the app is too busy to redraw between the taps
        compose.runOnUiThread {
            tapBackArrow?.invoke()
            tapBackArrow?.invoke()
        }

        appBarTitle(R.string.strategy_trainer).assertIsDisplayed()
    }

    @Test
    fun twoDrawerPicksBeforeItClosesOpenOnlyTheSecondOverTheTrainer() {
        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()
        val pickChart = drawerItem(R.string.strategy_chart).fetchSemanticsNode().config[SemanticsActions.OnClick].action
        val pickRules = drawerItem(R.string.table_rules).fetchSemanticsNode().config[SemanticsActions.OnClick].action

        // A second tap landing while the drawer is still sliding shut
        compose.runOnUiThread {
            pickChart?.invoke()
            pickRules?.invoke()
        }

        appBarTitle(R.string.table_rules).assertIsDisplayed()

        Espresso.pressBack()

        appBarTitle(R.string.strategy_trainer).assertIsDisplayed()
    }

    @Test
    fun theDrawerOpensTableRulesAndBackFromTheSoft17PageReturnsToIt() {
        openFromDrawer(R.string.table_rules)

        appBarTitle(R.string.table_rules).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.table_rules_intro)).assertIsDisplayed()

        compose.onNodeWithText(string(R.string.soft_17)).performClick()

        appBarTitle(R.string.soft_17).assertIsDisplayed()

        Espresso.pressBack()

        appBarTitle(R.string.table_rules).assertIsDisplayed()
    }

    @Test
    fun withTheDealerHittingAndRedoublingTheChartHasItsAfterDoublingTabs() {
        openFromDrawer(R.string.table_rules)
        chooseDealerHits()

        compose.onNode(hasText(string(R.string.soft_17)) and hasText(string(R.string.dealer_hits))).assertIsDisplayed()

        compose.onNode(hasText(string(R.string.redoubling)) and isToggleable()).performClick()
        Espresso.pressBack()
        compose.onNodeWithContentDescription(string(R.string.open_strategy_chart)).performClick()

        compose.onNodeWithText("Dealer hits soft 17 · Redoubling allowed · 6 decks").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.table_after_double_hard)).assertExists()
    }

    @Test
    fun withTheDealerHittingTheSameHandGradesSurrenderOnSixteenVsAceAsRight() {
        openFromDrawer(R.string.table_rules)
        chooseDealerHits()
        Espresso.pressBack()

        compose.onNodeWithContentDescription("9 of clubs").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.move_surrender)).performClick()

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs A. Surrender, otherwise hit").assertIsDisplayed()
    }

    @Test
    fun theDrawerOpensCustomizeHandsAndBackFromTheHandsDealtPageReturnsToIt() {
        openFromDrawer(R.string.customize_hands)

        appBarTitle(R.string.customize_hands).assertIsDisplayed()

        compose.onNode(hasText(string(R.string.hands_dealt)) and hasText(string(R.string.random))).performClick()

        appBarTitle(R.string.hands_dealt).assertIsDisplayed()

        Espresso.pressBack()

        appBarTitle(R.string.customize_hands).assertIsDisplayed()
    }

    @Test
    fun withOnlyPairsToSplitSwitchedOnEveryHandDealtIsAPairToSplit() {
        val pairsSplit = HandType(ChartTable.PAIRS, Move.SPLIT)
        val random = Random(21)
        val dealt = mutableListOf<TrainerHand>()
        dealHand = { picker, history -> picker.pick(history, random).also { dealt += it } }

        openFromDrawer(R.string.customize_hands)
        HAND_TYPES.filter { it != pairsSplit }.forEach { compose.handTypeSwitch(compose.activity, it).performScrollTo().performClick() }
        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        // The hand on the table stays, and each answer deals the next through the switches
        compose.onNodeWithContentDescription("9 of clubs").assertIsDisplayed()
        repeat(20) { compose.onNodeWithContentDescription(string(R.string.move_split)).performClick() }

        assertEquals(List(20) { pairsSplit }, dealt.map { it.type(StrategyCharts.forRules(RuleSet.S17)) })
    }
}
