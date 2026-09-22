package com.aquigs.sp21ace.ui.play

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.game.STARTING_BANKROLL
import com.aquigs.sp21ace.domain.game.Shoe
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.ui.components.TAP_GUARD_MILLIS
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class PlayScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var table by mutableStateOf(Table(STARTING_BANKROLL, Shoe(emptyList())))

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    // Dealt in order: player, upcard, player, hole, then the draws
    private fun stacked(deal: String, ruleSet: RuleSet = RuleSet.S17): Table =
        requireNotNull(Table(STARTING_BANKROLL, Shoe(cards(deal))).addChip(2_500)?.deal(ruleSet, Random(1)))

    // 16 against a 6, which stands, and the next card is a Q, which busts whoever draws it
    private val sixteenVsSix get() = stacked("Kc 6s 6d Kh Qs")

    // Most tests play whatever move they need, so the warning is off unless a test is about it
    private fun show(start: Table, settings: Settings = Settings(warnOnIncorrectMove = false)) {
        table = start
        compose.setContent {
            Sp21AceTheme {
                PlayScreen(
                    table = table,
                    settings = settings,
                    onUpdate = { change -> change(table)?.let { table = it } },
                    onDeal = { table = requireNotNull(table.deal(RuleSet.S17, Random(1))) },
                    onOpenDrawer = {},
                    onOpenChart = {},
                )
            }
        }
    }

    private fun button(name: Int, vararg args: Any) = compose.onNodeWithContentDescription(string(name, *args))

    // The table's buttons take taps once they've been on screen a double tap's length
    private fun tap(name: Int) {
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        button(name).performClick()
    }

    private fun band(text: Int) = compose.onNodeWithText(string(text))

    @Test
    fun asksForABetAndOffersDealOnceOneIsDown() {
        show(Table(STARTING_BANKROLL, Shoe(cards("9c 6s 7d Kh"))))
        band(R.string.place_your_bet).assertIsDisplayed()
        button(R.string.deal).assertDoesNotExist()

        button(R.string.bet_chip, "25").performClick()
        button(R.string.bet_chip, "5").performClick()

        button(R.string.bankroll_description, "970").assertIsDisplayed()
        button(R.string.deal).assertIsDisplayed()

        button(R.string.take_back_bet, "30").performClick()
        button(R.string.bankroll_description, "1,000").assertIsDisplayed()
        button(R.string.deal).assertDoesNotExist()
    }

    @Test
    fun greysOutAChipTheBankrollCantCover() {
        show(Table(2_000, Shoe(emptyList())))

        button(R.string.bet_chip, "25").assertIsNotEnabled()
    }

    @Test
    fun dealsWithTheHoleCardFaceDownAndOnlyTheMovesTheHandCanMake() {
        show(requireNotNull(Table(STARTING_BANKROLL, Shoe(cards("9c 6s 7d Kh"))).addChip(2_500)))

        tap(R.string.deal)

        button(R.string.face_down_card).assertIsDisplayed()
        band(R.string.place_your_bet).assertDoesNotExist()
        listOf(R.string.move_hit, R.string.move_stand, R.string.move_double, R.string.move_surrender).forEach { button(it).assertIsDisplayed() }
        button(R.string.move_split).assertDoesNotExist()
    }

    @Test
    fun theSecondTapOfADoubleTapOnDealMissesTheMoveThatTakesItsPlace() {
        show(requireNotNull(Table(STARTING_BANKROLL, Shoe(cards("9c 6s 7d Kh"))).addChip(2_500)))

        tap(R.string.deal)
        button(R.string.move_surrender).performClick()

        band(R.string.result_surrendered).assertDoesNotExist()
        button(R.string.move_surrender).assertIsDisplayed()
    }

    @Test
    fun aDoubledHandOffersStandRedoubleAndRescue() {
        show(requireNotNull(stacked("5c 6s 6d Kh 2c", ruleSet = RuleSet.H17_REDOUBLE).play(Move.DOUBLE)))

        listOf(R.string.move_stand, R.string.move_redouble, R.string.move_rescue).forEach { button(it).assertIsDisplayed() }
        button(R.string.move_hit).assertDoesNotExist()
    }

    @Test
    fun theResultAndPayoutWaitForTheDealersLastCardThenTheSameBetGoesDownAgain() {
        show(sixteenVsSix)

        tap(R.string.move_stand)
        band(R.string.result_win).assertDoesNotExist()
        button(R.string.bankroll_description, "975").assertIsDisplayed()

        compose.mainClock.advanceTimeBy(DEALER_CARD_MILLIS)

        band(R.string.result_win).assertIsDisplayed()
        compose.onNodeWithText("+25").assertIsDisplayed()
        button(R.string.bankroll_description, "1,025").assertIsDisplayed()

        tap(R.string.ok)

        band(R.string.place_your_bet).assertIsDisplayed()
        button(R.string.take_back_bet, "25").assertIsDisplayed()
    }

    @Test
    fun namesTheBonusAndWhatItPays() {
        // 2-3-4-5 draws a 7 to a 5-card 21
        show(requireNotNull(stacked("2c Ks 3d 7h 4h 5s 7c").play(Move.HIT)?.play(Move.HIT)))

        tap(R.string.move_hit)

        val bonus = string(R.string.bonus_pays, string(R.string.bonus_five_card_21), string(R.string.odds, 3, 2))
        compose.onNodeWithText("$bonus · +37.50").assertIsDisplayed()
    }

    @Test
    fun stepsThroughEachSplitHandsResult() {
        // 8-8 splits against a 7 and K: the first hand draws 3 and K to 21, which beats the dealer's 17, and the second 9 to 17, a push
        show(stacked("8c 7s 8d Kh 3h Ks 9d"))

        tap(R.string.move_split)
        tap(R.string.move_hit)
        // The first hand's 21 stays on show until NEXT, as in Blackjack Ace, with nothing to decide on it
        compose.onNodeWithContentDescription(string(R.string.card_name, string(R.string.king), string(R.string.spades))).assertIsDisplayed()
        button(R.string.move_stand).assertDoesNotExist()
        button(R.string.show_hint).assertDoesNotExist()
        button(R.string.face_down_card).assertIsDisplayed()
        tap(R.string.next)
        tap(R.string.move_stand)

        band(R.string.result_win).assertIsDisplayed()
        tap(R.string.next)
        band(R.string.result_push).assertIsDisplayed()
        tap(R.string.ok)

        band(R.string.place_your_bet).assertIsDisplayed()
    }

    @Test
    fun aSplitHandThatBustsShowsItAndLosesItsBetAtOnceThenItsResultIsntShownAgain() {
        // 8-8 splits against a 7: the first hand draws 5 and K to bust, and the second a 9 to 17, which pushes the dealer's 17
        show(stacked("8c 7s 8d Kh 5h Ks 9d"))

        tap(R.string.move_split)
        compose.onAllNodesWithText("25").assertCountEquals(2)
        tap(R.string.move_hit)

        band(R.string.result_bust).assertIsDisplayed()
        compose.onNodeWithText("−25").assertIsDisplayed()
        compose.onAllNodesWithText("25").assertCountEquals(1)

        tap(R.string.next)
        band(R.string.result_bust).assertDoesNotExist()
        tap(R.string.move_stand)

        band(R.string.result_push).assertIsDisplayed()
        button(R.string.next).assertDoesNotExist()
        button(R.string.ok).assertIsDisplayed()
    }

    @Test
    fun theBulbRingsTheCorrectMoveThenGoesUntilTheNextDecision() {
        // 12 vs 2 hits, and the 3 it draws makes 15, a decision of its own
        show(stacked("Kc 2s 2d Kh 3s"))

        button(R.string.show_hint).performClick()

        button(R.string.move_hit)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.hint_correct_move)))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        button(R.string.move_stand).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        button(R.string.show_hint).assertDoesNotExist()

        tap(R.string.move_hit)

        button(R.string.show_hint).assertIsDisplayed()
        button(R.string.move_stand).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun theHintButtonSettingHidesTheBulb() {
        show(sixteenVsSix, Settings(hintButton = false))

        button(R.string.show_hint).assertDoesNotExist()
    }

    @Test
    fun aMoveTheStrategyDoesntMakeAsksFirst() {
        show(sixteenVsSix, Settings())
        val question = string(R.string.incorrect_move_message, string(R.string.move_hit))

        tap(R.string.move_hit)
        compose.onNodeWithText(question).assertIsDisplayed()
        // The second tap of a double tap, landing outside the dialog or on Back, doesn't back out of it unread
        Espresso.pressBack()
        compose.onNodeWithText(question).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.cancel)).performClick()

        compose.onNodeWithText(question).assertDoesNotExist()
        compose.onNodeWithContentDescription(string(R.string.card_name, string(R.string.queen), string(R.string.spades))).assertDoesNotExist()
        // Backing out counts as help, as Blackjack Ace counts it
        assertTrue(requireNotNull(table.round?.activeHand).strategy.helped)

        tap(R.string.move_hit)
        // The dialog opens under the finger, so the second tap of a double tap doesn't confirm it
        compose.onNodeWithText(string(R.string.play_move)).performClick()
        compose.onNodeWithText(question).assertIsDisplayed()
        compose.mainClock.advanceTimeBy(TAP_GUARD_MILLIS)
        compose.onNodeWithText(string(R.string.play_move)).performClick()

        band(R.string.result_bust).assertIsDisplayed()
    }

    @Test
    fun theCorrectMovePlaysWithoutAsking() {
        show(sixteenVsSix, Settings())

        tap(R.string.move_stand)

        compose.onNodeWithText(string(R.string.incorrect_move_title)).assertDoesNotExist()
        button(R.string.move_stand).assertDoesNotExist()
    }

    @Test
    fun withTheWarningOffAMoveTheStrategyDoesntMakePlaysWithoutAsking() {
        show(sixteenVsSix, Settings(warnOnIncorrectMove = false))

        tap(R.string.move_hit)

        compose.onNodeWithText(string(R.string.incorrect_move_title)).assertDoesNotExist()
        band(R.string.result_bust).assertIsDisplayed()
    }

    @Test
    fun theBankrollOpensFreeTopUps() {
        show(Table(STARTING_BANKROLL, Shoe(emptyList())))

        button(R.string.bankroll_description, "1,000").performClick()
        button(R.string.add_chips_amount, "500").performClick()
        compose.onNodeWithText(string(R.string.done)).performClick()

        button(R.string.bankroll_description, "1,500").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.add_chips)).assertDoesNotExist()
    }
}
