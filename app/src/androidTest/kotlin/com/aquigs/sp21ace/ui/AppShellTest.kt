package com.aquigs.sp21ace.ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
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
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.Sp21AceApp
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppShellTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Hard 16 vs A is a hit when the dealer stands on soft 17, and a surrender when the dealer hits
    private val sixteenVsAce = TrainerHand(listOf(Card(Rank.NINE, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS)), Card(Rank.ACE, Suit.SPADES))
    private val eightsVsSix = TrainerHand(listOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.EIGHT, Suit.SPADES)), Card(Rank.SIX, Suit.DIAMONDS))

    // Its own file, so the tests never overwrite the rules the app itself saved
    private val store by lazy { TableRulesStore(compose.activity, "table_rules_app_shell_test") }

    private fun string(id: Int) = compose.activity.getString(id)

    // A closed drawer stays composed just off screen, so being displayed is what tells open from closed
    private fun drawerItem(title: Int) = compose.onNode(hasText(string(title)) and isSelectable())

    // The drawer carries the same names, so match only the app bar's heading
    private fun appBarTitle(title: Int) = compose.onNode(hasText(string(title)) and isHeading())

    @Before
    fun setUp() {
        // Edge to edge like MainActivity, or the status bar inset never reaches the composables
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
        store.save(TableRules())

        // The app's own wiring, dealing 16 vs A first and a pair of 8s after every answer
        var dealt = 0
        compose.setContent { Sp21AceTheme(darkTheme = false) { Sp21AceApp(store, deal = { if (dealt++ == 0) sixteenVsAce else eightsVsSix }) } }
    }

    @After
    fun tearDown() = store.save(TableRules())

    private fun openFromDrawer(title: Int) {
        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()
        drawerItem(title).performClick()
    }

    private fun chooseDealerHits() {
        compose.onNodeWithText(string(R.string.soft_17)).performClick()
        compose.onNodeWithText(string(R.string.dealer_hits)).performClick()
    }

    @Test
    fun menuButtonOpensTheDrawerOnTheStrategyTrainer() {
        drawerItem(R.string.strategy_trainer).assertIsNotDisplayed()

        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()

        compose.onNodeWithText(string(R.string.basic_strategy)).assertIsDisplayed()
        drawerItem(R.string.strategy_trainer).assertIsDisplayed().assertIsSelected()
        drawerItem(R.string.table_rules).assertIsDisplayed()
        drawerItem(R.string.strategy_chart).assertIsDisplayed()
        assertTrue(drawerItem(R.string.strategy_chart).getBoundsInRoot().top >= drawerItem(R.string.table_rules).getBoundsInRoot().bottom)
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
}
