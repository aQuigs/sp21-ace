package com.aquigs.sp21ace.ui.trainer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
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

    private fun button(move: Move) = compose.onNodeWithContentDescription(string(move.displayName))

    private fun showTrainer(modifier: Modifier = Modifier, deals: List<TrainerHand> = listOf(eightsVsSix)) {
        val chart = StrategyCharts.forRules(RuleSet.S17)
        // Only these hands are left to deal, so grading one answer more would throw
        val next = deals.iterator()
        var trainer by mutableStateOf(TrainerState(sixteenVsAce))

        compose.setContent {
            Sp21AceTheme {
                StrategyTrainerScreen(
                    state = trainer,
                    onAnswer = { asked, move -> trainer = trainer.answer(asked, move, chart, next::next) },
                    onOpenDrawer = {},
                    modifier = modifier,
                )
            }
        }
    }

    private fun assertStreakOnItsRung(streak: Int) {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.streak_count, streak)).assertIsDisplayed()

        // The circle repeats the label of the rung it sits on, so exactly that number shows twice, level. Nothing else on the
        // trainer is a bare number.
        val levels = compose.onAllNodesWithText("$streak", useUnmergedTree = true).fetchSemanticsNodes().map { it.boundsInRoot.center.y }
        assertEquals(2, levels.size)
        assertEquals(levels[0], levels[1], 2f)
    }

    @Test
    fun showsTheDealerUpcardOverTheHoleCardAndThePlayersTwoCards() {
        showTrainer()

        compose.onNodeWithText(string(R.string.dealer)).assertIsDisplayed()
        compose.onNodeWithText(string(R.string.you)).assertIsDisplayed()

        for (card in listOf(string(R.string.face_down_card), "Ace of spades", "9 of clubs", "7 of diamonds")) {
            compose.onNodeWithContentDescription(card).assertIsDisplayed()
        }
    }

    @Test
    fun aRightAnswerTurnsTheBarRightAndDealsTheNextHand() {
        showTrainer()

        button(Move.HIT).performClick()

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs A. Hit").assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()

        compose.onNodeWithContentDescription("6 of diamonds").assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertIsDisplayed()
        // Only the Previous Hand panel still shows the answered hand
        compose.onAllNodesWithContentDescription("9 of clubs").assertCountEquals(1)
    }

    @Test
    fun aWrongAnswerTurnsTheBarWrong() {
        showTrainer()

        button(Move.STAND).performClick()

        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. Hard 16 vs A. Hit").assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()
    }

    @Test
    fun aSecondTapBeforeTheNextHandIsDrawnIsIgnored() {
        showTrainer()
        compose.mainClock.autoAdvance = false
        val hit = button(Move.HIT)

        hit.performSemanticsAction(SemanticsActions.OnClick)
        hit.performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.autoAdvance = true

        compose.onNodeWithText("Hard 16 vs A | Hit").assertIsDisplayed()
        compose.onNodeWithContentDescription("8 of hearts").assertIsDisplayed()
    }

    @Test
    fun beforeAnyAnswerThePreviousHandPanelIsBlank() {
        showTrainer()

        compose.onNodeWithText(string(R.string.previous_hand)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.action)).assertDoesNotExist()
        compose.onNodeWithText(string(R.string.strategy)).assertDoesNotExist()
        compose.onAllNodesWithText(string(R.string.you)).assertCountEquals(1)
        compose.onAllNodesWithContentDescription(string(R.string.face_down_card)).assertCountEquals(1)
    }

    @Test
    fun aWrongAnswerRecapsTheCardsTheChosenMoveAndTheStrategy() {
        showTrainer()

        button(Move.STAND).performClick()

        val you = hasText(string(R.string.you)) and hasContentDescription("9 of clubs") and hasContentDescription("7 of diamonds")
        val dealer = hasText(string(R.string.dealer)) and hasContentDescription(string(R.string.face_down_card)) and
            hasContentDescription("Ace of spades")
        compose.onNode(you).assertIsDisplayed()
        compose.onNode(dealer).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.action)) and hasText(string(R.string.move_stand))).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.strategy)) and hasText(string(R.string.move_hit))).assertIsDisplayed()
    }

    @Test
    fun onAShortScreenTheAnswerButtonsShrinkEvenly() {
        // Too short for five full-size buttons above the recap, as on a small phone at a large font size
        showTrainer(Modifier.height(480.dp))

        val heights = Move.entries.map { button(it).getBoundsInRoot().height.value }

        heights.forEach { assertEquals(heights.first(), it, 1f) }
    }

    @Test
    fun rightAnswersClimbTheStreakMeterAndAWrongOneDropsItToZero() {
        showTrainer(deals = listOf(eightsVsSix, sixteenVsAce, eightsVsSix))

        button(Move.HIT).performClick()
        button(Move.SPLIT).performClick()

        assertStreakOnItsRung(2)

        button(Move.STAND).performClick()

        assertStreakOnItsRung(0)
    }
}
