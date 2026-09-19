package com.aquigs.sp21ace.ui.hands

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.dealing.HAND_TYPES
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.dealing.HandType
import com.aquigs.sp21ace.domain.dealing.HandsDealt
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.texts
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CustomizeHandsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-09-17T12:00:00Z")
    private val pairsSplit = HandType(ChartTable.PAIRS, Move.SPLIT)
    private val hardSurrender = HandType(ChartTable.HARD, Move.SURRENDER)

    private var customization by mutableStateOf(HandCustomization())

    private fun string(id: Int) = compose.activity.getString(id)

    private fun summary(accuracy: String) = compose.activity.getString(R.string.accuracy_summary, accuracy)

    private fun percentage(value: Double) = compose.activity.getString(R.string.percentage, value)

    private fun showScreen(history: List<PracticeAnswer> = emptyList(), onOpenHandsDealt: () -> Unit = {}) {
        compose.setContent {
            Sp21AceTheme {
                CustomizeHandsScreen(customization, history, onChange = { customization = it }, onOpenHandsDealt = onOpenHandsDealt, onBack = {})
            }
        }
    }

    private fun group(title: Int) = compose.onNodeWithText(string(title))

    private fun switchFor(type: HandType) = compose.handTypeSwitch(compose.activity, type)

    private fun switches(move: Int) = compose.onAllNodes(hasText(string(move)) and isToggleable())

    @Test
    fun everyGroupStartsOpenAndItsHeaderClosesAndReopensIt() {
        showScreen()

        switches(R.string.move_hit).assertCountEquals(3)

        group(R.string.table_hard).performScrollTo().performClick()

        switches(R.string.move_hit).assertCountEquals(2)
        switches(R.string.move_surrender).assertCountEquals(1)

        group(R.string.table_pairs).performScrollTo().performClick()

        switches(R.string.move_hit).assertCountEquals(1)
        switches(R.string.move_split).assertCountEquals(0)

        group(R.string.table_hard).performScrollTo().performClick()

        switches(R.string.move_hit).assertCountEquals(2)
        switches(R.string.move_surrender).assertCountEquals(1)
    }

    @Test
    fun aSwitchTurnsItsTypeOffAndOnAgain() {
        showScreen()

        switchFor(pairsSplit).performScrollTo().assertIsOn().performClick().assertIsOff()

        assertEquals(setOf(pairsSplit), customization.switchedOff)

        switchFor(hardSurrender).performScrollTo().performClick()

        assertEquals(setOf(pairsSplit, hardSurrender), customization.switchedOff)

        switchFor(pairsSplit).performScrollTo().performClick().assertIsOn()

        assertEquals(setOf(hardSurrender), customization.switchedOff)
    }

    @Test
    fun theSwitchForHandsOf3OrMoreCardsShowsTheirAccuracyAndTurnsThemOffAndOnAgain() {
        // Hard 16 vs A hits when the dealer stands on soft 17, with two cards or three
        val history = listOf(
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("9c 4d 3s"), card("As")), Move.HIT, Move.HIT),
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("9c 7d"), card("As")), Move.STAND, Move.HIT),
        )
        showScreen(history)
        val switch = compose.onNode(hasText(string(R.string.multi_card_hands)) and isToggleable())

        assertEquals(listOf(string(R.string.multi_card_hands), summary(percentage(100.0))), switch.texts())

        switch.performScrollTo().assertIsOn().performClick().assertIsOff()

        assertEquals(HandCustomization(multiCardHands = false), customization)

        switch.performClick().assertIsOn()

        assertEquals(HandCustomization(), customization)
    }

    @Test
    fun theCardCountSwitchShowsItsAccuracyTurnsOffAndOnAndGreysOutWithHandsOf3OrMoreCardsOff() {
        // When the dealer stands on soft 17, hard 17 vs A is RH, so with 3 cards the count makes it a hit, while hard 16 vs A always hits
        val history = listOf(
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("9c 4d 4h"), card("As")), Move.HIT, Move.HIT),
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("9c 4d 3s"), card("As")), Move.STAND, Move.HIT),
        )
        showScreen(history)
        val switch = compose.onNode(hasText(string(R.string.card_count_hands)) and isToggleable())

        assertEquals(listOf(string(R.string.card_count_hands), summary(percentage(100.0))), switch.texts())

        switch.performScrollTo().assertIsOn().performClick().assertIsOff()

        assertEquals(HandCustomization(cardCountHands = false), customization)

        switch.performClick().assertIsOn()
        compose.onNode(hasText(string(R.string.multi_card_hands)) and isToggleable()).performClick()

        switch.assertIsNotEnabled().assertIsOn()
        assertEquals(HandCustomization(multiCardHands = false), customization)
    }

    @Test
    fun theBonusSwitchShowsItsAccuracyAndTurnsOffAndOnAgain() {
        // When the dealer stands on soft 17, hard 14 vs 4 is S4*, so any 6-8 hits for a 6-7-8 bonus, while a 5-9 stands
        val history = listOf(
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("6c 8d"), card("4s")), Move.HIT, Move.HIT),
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("6h 8h"), card("4d")), Move.STAND, Move.HIT),
            PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("5c 9d"), card("4s")), Move.STAND, Move.STAND),
        )
        showScreen(history)
        val switch = compose.onNode(hasText(string(R.string.bonus_hands)) and isToggleable())

        assertEquals(listOf(string(R.string.bonus_hands), summary(percentage(50.0))), switch.texts())

        switch.performScrollTo().assertIsOn().performClick().assertIsOff()

        assertEquals(HandCustomization(bonusHands = false), customization)

        switch.performClick().assertIsOn()

        assertEquals(HandCustomization(), customization)
    }

    @Test
    fun theHandsDealtRowShowsRandomAndOpensItsPage() {
        var opened = 0
        showScreen(onOpenHandsDealt = { opened++ })

        compose.onNode(hasText(string(R.string.hands_dealt)) and hasText(string(R.string.random))).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun theHandsDealtPageMarksTheChoiceAndChoosingOneGoesBack() {
        var backs = 0
        compose.setContent { Sp21AceTheme { HandsDealtScreen(customization, onChange = { customization = it }, onBack = { backs++ }) } }

        compose.onNodeWithText(string(R.string.random)).assertIsSelected()

        compose.onNodeWithText(string(R.string.prioritize_worse_hands)).assertIsNotSelected().performClick().assertIsSelected()

        compose.onNodeWithText(string(R.string.random)).assertIsNotSelected()
        assertEquals(HandCustomization(HandsDealt.PRIORITIZE_WORSE), customization)
        assertEquals(1, backs)
    }

    @Test
    fun doubledHandsHaveAGroupForEachAfterDoublingTableWithRedoubleAndRescueSwitches() {
        // Doubled hard 16 vs A is a rescue, and doubled soft 18 vs 4 a redouble with redoubling
        showScreen(
            listOf(
                PracticeAnswer(now, RuleSet.S17, TrainerHand(cards("5c 6d 5h"), card("As"), doubled = true), Move.RESCUE, Move.RESCUE),
                PracticeAnswer(now, RuleSet.H17_REDOUBLE, TrainerHand(cards("As 5d 2c"), card("4h"), doubled = true), Move.STAND, Move.REDOUBLE),
            ),
        )
        val rescue = HandType(ChartTable.AFTER_DOUBLE_HARD, Move.RESCUE)

        assertEquals(listOf(string(R.string.table_after_double_hard), summary(percentage(100.0))), group(R.string.table_after_double_hard).performScrollTo().texts())
        assertEquals(listOf(string(R.string.table_after_double_soft), summary(percentage(0.0))), group(R.string.table_after_double_soft).performScrollTo().texts())
        switches(R.string.move_redouble).assertCountEquals(2)
        switches(R.string.move_rescue).assertCountEquals(1)

        switchFor(rescue).performScrollTo().assertIsOn().performClick().assertIsOff()

        assertEquals(setOf(rescue), customization.switchedOff)
    }

    @Test
    fun eachSubtitleShowsTheAccuracyOfEveryAnswerEverGivenOrNoData() {
        // When the dealer stands on soft 17, hard 16 vs A is a hit, hard 18 vs 6 a stand and a pair of 8s vs 6 a split
        val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
        val eighteenVsSix = TrainerHand(cards("Kc 8d"), card("6h"))
        val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))

        fun answer(hand: TrainerHand, correctMove: Move, right: Boolean, at: Instant = now) =
            PracticeAnswer(at, RuleSet.S17, hand, if (right) correctMove else Move.SURRENDER, correctMove)

        showScreen(
            listOf(
                answer(sixteenVsAce, Move.HIT, right = true),
                answer(sixteenVsAce, Move.HIT, right = true),
                answer(sixteenVsAce, Move.HIT, right = false),
                answer(eighteenVsSix, Move.STAND, right = false),
                // A year ago, which still counts
                answer(eightsVsSix, Move.SPLIT, right = true, at = now.minus(Duration.ofDays(365))),
            ),
        )

        assertEquals(listOf(string(R.string.table_hard), summary(percentage(50.0))), group(R.string.table_hard).texts())
        assertEquals(listOf(string(R.string.table_soft), summary(string(R.string.no_data))), group(R.string.table_soft).texts())
        assertEquals(listOf(string(R.string.table_pairs), summary(percentage(100.0))), group(R.string.table_pairs).texts())

        // Two right of three rounds down, as on Accuracy
        val figures = mapOf(
            HandType(ChartTable.HARD, Move.HIT) to percentage(66.6),
            HandType(ChartTable.HARD, Move.STAND) to percentage(0.0),
            pairsSplit to percentage(100.0),
        )
        for (type in HAND_TYPES) {
            assertEquals("$type", listOf(string(type.move.displayName), summary(figures[type] ?: string(R.string.no_data))), switchFor(type).texts())
        }
    }
}
