package com.aquigs.sp21ace.ui

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
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
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
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

    // Hard 16 vs A is a hit when the dealer stands on soft 17
    private val sixteenVsAce = TrainerHand(listOf(Card(Rank.NINE, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS)), Card(Rank.ACE, Suit.SPADES))
    private val eightsVsSix = TrainerHand(listOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.EIGHT, Suit.SPADES)), Card(Rank.SIX, Suit.DIAMONDS))

    private fun string(id: Int) = compose.activity.getString(id)

    // A closed drawer stays composed just off screen, so being displayed is what tells open from closed
    private fun drawerItem(title: Int) = compose.onNode(hasText(string(title)) and isSelectable())

    // The drawer carries the same names, so match only the app bar's heading
    private fun appBarTitle(title: Int) = compose.onNode(hasText(string(title)) and isHeading())

    @Before
    fun setUp() {
        // Edge to edge like MainActivity, or the status bar inset never reaches the composables
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }

        // Held above AppShell as MainActivity holds it, so only AppShell's own navigation could lose the hand
        val chart = StrategyCharts.forRules(RuleSet.S17)
        var trainer by mutableStateOf(TrainerState(sixteenVsAce))
        compose.setContent {
            Sp21AceTheme(darkTheme = false) {
                AppShell(trainer, RuleSet.S17, chart, onAnswer = { asked, move -> trainer = trainer.answer(asked, move, chart) { eightsVsSix } })
            }
        }
    }

    @Test
    fun menuButtonOpensTheDrawerOnTheStrategyTrainer() {
        drawerItem(R.string.strategy_trainer).assertIsNotDisplayed()

        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()

        compose.onNodeWithText(string(R.string.basic_strategy)).assertIsDisplayed()
        drawerItem(R.string.strategy_trainer).assertIsDisplayed().assertIsSelected()
        drawerItem(R.string.strategy_chart).assertIsDisplayed()
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
        compose.onNodeWithContentDescription(string(R.string.open_menu)).performClick()
        drawerItem(R.string.strategy_chart).performClick()

        appBarTitle(R.string.strategy_chart).assertIsDisplayed()
        drawerItem(R.string.strategy_chart).assertIsNotDisplayed()

        compose.onNodeWithContentDescription(string(R.string.back)).performClick()

        appBarTitle(R.string.strategy_trainer).assertIsDisplayed()
        compose.onNodeWithContentDescription("9 of clubs").assertIsDisplayed()
    }
}
