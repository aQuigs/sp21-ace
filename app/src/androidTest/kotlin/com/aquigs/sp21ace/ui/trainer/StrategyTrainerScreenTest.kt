package com.aquigs.sp21ace.ui.trainer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.Trainer
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrategyTrainerScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Hard 16 vs A is a hit when the dealer stands on soft 17
    private val sixteenVsAce = TrainerHand(listOf(Card(Rank.NINE, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS)), Card(Rank.ACE, Suit.SPADES))
    private val eightsVsSix = TrainerHand(listOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.EIGHT, Suit.SPADES)), Card(Rank.SIX, Suit.DIAMONDS))

    private fun string(id: Int) = compose.activity.getString(id)

    @Before
    fun setUp() {
        val trainer = Trainer(StrategyCharts.forRules(RuleSet.S17), listOf(sixteenVsAce, eightsVsSix).iterator()::next)

        compose.setContent { Sp21AceTheme { StrategyTrainerScreen(trainer = trainer, onOpenDrawer = {}) } }
    }

    @Test
    fun showsTheDealerUpcardOverTheHoleCardAndThePlayersTwoCards() {
        compose.onNodeWithText(string(R.string.dealer)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.you)).assertIsDisplayed()

        for (card in listOf(string(R.string.face_down_card), "Ace of spades", "9 of clubs", "7 of diamonds")) {
            compose.onNodeWithContentDescription(card).assertIsDisplayed()
        }
    }

    @Test
    fun aRightAnswerTurnsTheBarRightAndDealsTheNextHand() {
        compose.onNodeWithText(string(R.string.hit)).performClick()

        compose.onNodeWithContentDescription(string(R.string.right_answer)).assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()

        compose.onNodeWithContentDescription("6 of diamonds").assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertIsDisplayed()
        compose.onNodeWithContentDescription("9 of clubs").assertDoesNotExist()
    }

    @Test
    fun aWrongAnswerTurnsTheBarWrong() {
        compose.onNodeWithText(string(R.string.stand)).performClick()

        compose.onNodeWithContentDescription(string(R.string.wrong_answer)).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.right_answer)).assertDoesNotExist()
        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()
    }
}
