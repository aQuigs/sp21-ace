package com.aquigs.sp21ace.ui.trainer

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
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
        val chart = StrategyCharts.forRules(RuleSet.S17)
        // Only one hand left to deal, so grading a second hand would throw
        val deals = listOf(eightsVsSix).iterator()
        var trainer by mutableStateOf(TrainerState(sixteenVsAce))

        compose.setContent {
            Sp21AceTheme {
                StrategyTrainerScreen(
                    state = trainer,
                    onAnswer = { asked, move -> trainer = trainer.answer(asked, move, chart, deals::next) },
                    onOpenDrawer = {},
                )
            }
        }
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

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs A. Hit").assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()

        compose.onNodeWithContentDescription("6 of diamonds").assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertIsDisplayed()
        // Only the Previous Hand panel still shows the answered hand
        compose.onAllNodesWithContentDescription("9 of clubs").assertCountEquals(1)
    }

    @Test
    fun aWrongAnswerTurnsTheBarWrong() {
        compose.onNodeWithText(string(R.string.stand)).performClick()

        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. Hard 16 vs A. Hit").assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()
    }

    @Test
    fun aSecondTapBeforeTheNextHandIsDrawnIsIgnored() {
        compose.mainClock.autoAdvance = false
        val hit = compose.onNodeWithText(string(R.string.hit))

        hit.performSemanticsAction(SemanticsActions.OnClick)
        hit.performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.autoAdvance = true

        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertIsDisplayed()
    }

    @Test
    fun beforeAnyAnswerThePreviousHandPanelShowsOnlyItsTitle() {
        compose.onNodeWithText(string(R.string.previous_hand)).assertIsDisplayed()

        compose.onNodeWithText(string(R.string.action)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.strategy)).assertDoesNotExist()
        compose.onAllNodesWithText(string(R.string.you)).assertCountEquals(1)
        compose.onAllNodesWithContentDescription(string(R.string.face_down_card)).assertCountEquals(1)
    }

    @Test
    fun aWrongAnswerRecapsTheCardsTheChosenMoveAndTheStrategy() {
        compose.onNodeWithText(string(R.string.stand)).performClick()

        val you = hasText(string(R.string.you)) and hasContentDescription("9 of clubs") and hasContentDescription("7 of diamonds")
        val dealer = hasText(string(R.string.dealer)) and hasContentDescription(string(R.string.face_down_card)) and
            hasContentDescription("Ace of spades")
        compose.onNode(you).assertIsDisplayed()
        compose.onNode(dealer).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.action)) and hasText(string(R.string.move_stand))).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.strategy)) and hasText(string(R.string.move_hit))).assertIsDisplayed()
    }

    @Test
    fun aRightAnswerShowsTheSameMoveInBothTiles() {
        compose.onNodeWithText(string(R.string.hit)).performClick()

        compose.onNode(hasText(string(R.string.action)) and hasText(string(R.string.move_hit))).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.strategy)) and hasText(string(R.string.move_hit))).assertIsDisplayed()
    }
}
