package com.aquigs.sp21ace.ui.accuracy

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.isPlaced
import com.aquigs.sp21ace.ui.onPlacedNodeWithText
import com.aquigs.sp21ace.ui.swipeToNextTab
import com.aquigs.sp21ace.ui.swipeToPreviousTab
import com.aquigs.sp21ace.ui.theme.HeatmapColors
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class AccuracyScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-09-17T12:00:00Z")

    // Hard 16 vs A and soft 17 vs K are both hits when the dealer stands on soft 17, and hard 16 vs A a surrender when the dealer hits
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val softSeventeenVsKing = TrainerHand(cards("Ah 6d"), card("Kh"))

    private val notDoubledMoves = listOf(R.string.move_split, R.string.move_hit, R.string.move_double, R.string.move_stand, R.string.move_surrender)
    private val doubledMoves = listOf(R.string.move_redouble, R.string.move_stand, R.string.move_rescue)

    // Doubled hard 16 vs A is a rescue whatever the rules, and doubled soft 18 vs 4 a redouble with redoubling
    private val doubledSixteenVsAce = TrainerHand(cards("5c 6d 5h"), card("As"), doubled = true)
    private val doubledSoftEighteenVsFour = TrainerHand(cards("As 5d 2c"), card("4h"), doubled = true)

    private var rules by mutableStateOf(RuleSet.S17)
    private lateinit var heatmap: HeatmapColors
    private var page = Color.Unspecified

    private fun string(id: Int) = compose.activity.getString(id)

    private fun answer(right: Boolean, daysAgo: Long = 0, hand: TrainerHand = sixteenVsAce, correctMove: Move = Move.HIT) = PracticeAnswer(
        now.minus(Duration.ofDays(daysAgo)),
        RuleSet.S17,
        hand,
        if (right) correctMove else Move.entries.first { it != correctMove },
        correctMove,
    )

    private fun showAccuracy(history: List<PracticeAnswer>, clock: () -> Instant = { now }) {
        compose.setContent {
            Sp21AceTheme(ColorTheme.LIGHT) {
                heatmap = Sp21AceTheme.colors.heatmap
                page = MaterialTheme.colorScheme.background
                AccuracyScreen(history, rules, onBack = {}, now = clock)
            }
        }
    }

    // The tabs and chips scroll, so one may start out of view
    private fun tap(title: Int) = compose.onPlacedNodeWithText(string(title)).performScrollTo().performClick()

    private fun chooseDoubled() = compose.onNodeWithText(string(R.string.already_doubled)).performClick()

    private fun accuracyCard(title: Int) = compose.cardTexts(string(title), string(R.string.correct))

    private fun figures(title: Int, accuracy: String, correct: Int, incorrect: Int) = compose.activity.accuracyCardTexts(title, accuracy, correct, incorrect)

    private fun noData(title: Int) = figures(title, "--", correct = 0, incorrect = 0)

    private fun streakCard() = compose.cardTexts(string(R.string.longest_streak))

    private fun streak(longest: Int) = listOf(string(R.string.streak), "$longest", string(R.string.longest_streak))

    // Within a shade, since the capture rounds each channel to 8 bits
    private fun assertColour(expected: Color, actual: Color) {
        val difference = listOf(expected.red - actual.red, expected.green - actual.green, expected.blue - actual.blue).maxOf(::abs)
        assertTrue("expected $expected, was $actual", difference < 2 / 255f)
    }

    @Test
    fun todayIsTheDefaultAndTheChipsSwitchThePeriod() {
        showAccuracy(listOf(answer(right = true), answer(right = false, daysAgo = 3), answer(right = false, daysAgo = 20), answer(right = true, daysAgo = 60)))

        compose.onPlacedNodeWithText(string(R.string.today)).assertIsSelected()
        assertEquals(figures(R.string.overall, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.overall))

        tap(R.string.week).assertIsSelected()
        assertEquals(figures(R.string.overall, "50.0%", correct = 1, incorrect = 1), accuracyCard(R.string.overall))

        tap(R.string.month).assertIsSelected()
        assertEquals(figures(R.string.overall, "33.3%", correct = 1, incorrect = 2), accuracyCard(R.string.overall))

        tap(R.string.all_time).assertIsSelected()
        assertEquals(figures(R.string.overall, "50.0%", correct = 2, incorrect = 2), accuracyCard(R.string.overall))
    }

    @Test
    fun resumingADayLaterDropsTheAnswerFromToday() {
        var clock = now
        showAccuracy(listOf(answer(right = true)), clock = { clock })

        assertEquals(figures(R.string.overall, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.overall))

        // The phone locked, then opened again a day later
        clock = now.plus(Duration.ofHours(24))
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        assertEquals(noData(R.string.overall), accuracyCard(R.string.overall))
    }

    @Test
    fun leftOpenTheScreenLetsAnAnswerAgeOutWithinAMinute() {
        var clock = now
        showAccuracy(listOf(answer(right = true)), clock = { clock })

        clock = now.plus(Duration.ofHours(24))
        compose.mainClock.advanceTimeBy(60_000)

        assertEquals(noData(R.string.overall), accuracyCard(R.string.overall))
    }

    @Test
    fun theTabsSwitchTheKindOfHand() {
        showAccuracy(listOf(answer(right = true), answer(right = false, hand = softSeventeenVsKing)))

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        assertEquals(figures(R.string.overall, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.overall))
        assertEquals(figures(R.string.move_hit, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.move_hit))

        tap(R.string.table_soft).assertIsSelected()
        assertEquals(figures(R.string.overall, "0.0%", correct = 0, incorrect = 1), accuracyCard(R.string.overall))

        tap(R.string.all_hands).assertIsSelected()
        assertEquals(figures(R.string.overall, "50.0%", correct = 1, incorrect = 1), accuracyCard(R.string.overall))
    }

    @Test
    fun aSwipeMovesBetweenTheTabsAndKeepsThePeriod() {
        showAccuracy(listOf(answer(right = true), answer(right = false, daysAgo = 3, hand = softSeventeenVsKing)))
        tap(R.string.week)

        compose.swipeToNextTab()

        compose.onNodeWithText(string(R.string.table_soft)).assertIsSelected()
        compose.onPlacedNodeWithText(string(R.string.week)).assertIsSelected()
        assertEquals(figures(R.string.overall, "0.0%", correct = 0, incorrect = 1), accuracyCard(R.string.overall))

        compose.swipeToPreviousTab()

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        compose.onPlacedNodeWithText(string(R.string.week)).assertIsSelected()
        assertEquals(figures(R.string.overall, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.overall))
    }

    @Test
    fun withNoAnswersEveryAccuracyIsNoDataAndTheStreakIsNought() {
        showAccuracy(emptyList())

        for (title in listOf(R.string.overall, R.string.move_hit, R.string.move_double, R.string.move_stand, R.string.move_surrender)) {
            assertEquals(noData(title), accuracyCard(title))
        }
        // No hard hand calls for a split
        compose.onPlacedNodeWithText(string(R.string.move_split)).assertDoesNotExist()

        tap(R.string.all_hands)

        assertEquals(noData(R.string.move_split), accuracyCard(R.string.move_split))
        assertEquals(streak(longest = 0), streakCard())
    }

    @Test
    fun aPeriodWithNoAnswersShowsNoDataOnEveryCardButStillTheStreak() {
        showAccuracy(List(2) { answer(right = true, daysAgo = 3) })

        tap(R.string.all_hands)

        for (title in listOf(R.string.overall) + notDoubledMoves) {
            assertEquals(noData(title), accuracyCard(title))
        }
        assertEquals(streak(longest = 2), streakCard())

        chooseDoubled()
        tap(R.string.all_hands)

        for (title in listOf(R.string.overall) + doubledMoves) {
            assertEquals(noData(title), accuracyCard(title))
        }
        assertEquals(streak(longest = 2), streakCard())
    }

    @Test
    fun onlyTheAllTabHasTheStreakAndItRunsOverEveryAnswerWhateverThePeriod() {
        // Three right last week, then a wrong answer and two right today, the second a soft hand
        showAccuracy(
            List(3) { answer(right = true, daysAgo = 3) } +
                listOf(answer(right = false), answer(right = true), answer(right = true, hand = softSeventeenVsKing)),
        )

        compose.onPlacedNodeWithText(string(R.string.longest_streak)).assertDoesNotExist()

        tap(R.string.all_hands)

        assertEquals(streak(longest = 3), streakCard())

        // The doubled hands' All tab too, since the streak runs over every answer, doubled or not
        chooseDoubled()
        compose.onPlacedNodeWithText(string(R.string.longest_streak)).assertDoesNotExist()
        tap(R.string.all_hands)

        assertEquals(streak(longest = 3), streakCard())
    }

    @Test
    fun aSquareWithAnswersIsFilledOnTheScaleAndReadsItsAccuracy() {
        showAccuracy(
            listOf(
                answer(right = true),
                answer(right = true, hand = TrainerHand(cards("Kd 6h"), card("Ac"))),
                answer(right = true, hand = TrainerHand(cards("Qs 6c"), card("Ah"))),
                answer(right = false, hand = TrainerHand(cards("Jh 6s"), card("Ad"))),
            ),
        )

        assertColour(heatmap.at(0.75f), compose.square("16 vs A: Hit, 75% right", "H").fill())
    }

    @Test
    fun aSquareWithoutAnswersReadsNoAnswersAndStaysPlain() {
        showAccuracy(listOf(answer(right = false)))

        assertColour(page, compose.square("16 vs 10: Hit, no answers", "H").fill())
        assertColour(heatmap.at(0f), compose.square("16 vs A: Hit, 0% right", "H").fill())
    }

    @Test
    fun aBonusSquareReadsTheChartsWords() {
        // The 8-6 can still make 6-7-8, so it's graded a hit where the other 14s stand, and no one move words the square
        showAccuracy(
            listOf(
                answer(right = true, hand = TrainerHand(cards("Qh 4d"), card("4c")), correctMove = Move.STAND),
                answer(right = true, hand = TrainerHand(cards("Jc 4s"), card("4d")), correctMove = Move.STAND),
                answer(right = true, hand = TrainerHand(cards("9h 5c"), card("4s")), correctMove = Move.STAND),
                answer(right = false, hand = TrainerHand(cards("8h 6d"), card("4h")), correctMove = Move.HIT),
            ),
        )

        compose.square("14 vs 4: Stand, but hit with 4 or more cards or while any 6-7-8 is possible, 75% right", "S4*")
    }

    @Test
    fun theGridShowsOnlyTheRowsTheTrainerDeals() {
        showAccuracy(emptyList())

        // Two ten-value cards are a pair, so only 3 or more cards make hard 20, and 21 leaves nothing to decide
        compose.square("20 vs 2: Stand, no answers", "S")
        compose.onNode(hasContentDescription("21 vs ", substring = true) and isPlaced).assertDoesNotExist()
    }

    @Test
    fun eachKindOfHandHasItsOwnGridAndTheAllTabHasNone() {
        showAccuracy(
            listOf(
                answer(right = true),
                answer(right = true, hand = softSeventeenVsKing),
                answer(right = true, hand = TrainerHand(cards("8h 8s"), card("6d")), correctMove = Move.SPLIT),
            ),
        )

        compose.square("16 vs A: Hit, 100% right", "H")

        tap(R.string.table_soft)

        compose.square("A-6 vs 10: Hit, 100% right", "H")

        tap(R.string.table_pairs)

        compose.square("8-8 vs 6: Split, 100% right", "P")

        tap(R.string.all_hands)

        compose.onNode(hasContentDescription(" vs ", substring = true) and isPlaced).assertDoesNotExist()
    }

    @Test
    fun theChoiceSplitsTheAnswersIntoHandsNotYetDoubledAndThoseAlreadyDoubled() {
        showAccuracy(listOf(answer(right = true), answer(right = false, hand = doubledSixteenVsAce, correctMove = Move.RESCUE)))

        compose.onNodeWithText(string(R.string.not_doubled)).assertIsSelected()
        tap(R.string.all_hands)

        assertEquals(figures(R.string.overall, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.overall))
        compose.onPlacedNodeWithText(string(R.string.move_rescue)).assertDoesNotExist()

        chooseDoubled().assertIsSelected()

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_pairs)).assertDoesNotExist()
        assertEquals(figures(R.string.overall, "0.0%", correct = 0, incorrect = 1), accuracyCard(R.string.overall))

        tap(R.string.all_hands)

        assertEquals(figures(R.string.overall, "0.0%", correct = 0, incorrect = 1), accuracyCard(R.string.overall))
        compose.onPlacedNodeWithText(string(R.string.move_surrender)).assertDoesNotExist()
    }

    @Test
    fun withoutRedoublingDoubledHandsHaveOneHardTabOverTheirAnswers() {
        // Doubled hard 12 vs 8 is a rescue when the dealer stands on soft 17, and 16 vs 6 a stand
        showAccuracy(
            listOf(
                answer(right = true, hand = doubledSixteenVsAce, correctMove = Move.RESCUE),
                answer(right = false, hand = TrainerHand(cards("5c 6d As"), card("8s"), doubled = true), correctMove = Move.RESCUE),
            ),
        )

        chooseDoubled()

        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_soft)).assertDoesNotExist()
        compose.square("16 vs A: Rescue, 100% right", "R")
        compose.square("12 vs 8: Rescue, 0% right", "R")
        compose.onNode(hasContentDescription("16 vs 6: Stand, no answers") and isPlaced).assertExists()
        assertEquals(figures(R.string.move_rescue, "50.0%", correct = 1, incorrect = 1), accuracyCard(R.string.move_rescue))

        tap(R.string.all_hands)

        assertEquals(figures(R.string.move_rescue, "50.0%", correct = 1, incorrect = 1), accuracyCard(R.string.move_rescue))
    }

    @Test
    fun withRedoublingDoubledHandsHaveAHardAndASoftTabAndOneGoneWithTheRulesFallsBackToHard() {
        rules = RuleSet.H17_REDOUBLE
        showAccuracy(
            listOf(
                answer(right = true, hand = doubledSixteenVsAce, correctMove = Move.RESCUE),
                answer(right = false, hand = doubledSoftEighteenVsFour, correctMove = Move.REDOUBLE),
            ),
        )

        chooseDoubled()

        compose.square("16 vs A: Rescue, 100% right", "R")

        tap(R.string.table_soft)

        compose.square("A-7 vs 4: Redouble, 0% right", "D")
        assertEquals(figures(R.string.move_redouble, "0.0%", correct = 0, incorrect = 1), accuracyCard(R.string.move_redouble))

        rules = RuleSet.S17

        compose.onNodeWithText(string(R.string.already_doubled)).assertIsSelected()
        compose.onNodeWithText(string(R.string.table_hard)).assertIsSelected()
    }

    @Test
    fun theDoubledHandsAllTabStaysOpenWhenRedoublingAddsASoftTabBeforeIt() {
        rules = RuleSet.H17
        showAccuracy(listOf(answer(right = true, hand = doubledSixteenVsAce, correctMove = Move.RESCUE)))
        chooseDoubled()
        tap(R.string.all_hands)

        rules = RuleSet.H17_REDOUBLE

        compose.onNodeWithText(string(R.string.table_soft)).assertExists()
        compose.onNodeWithText(string(R.string.all_hands)).assertIsSelected()
        assertEquals(streak(longest = 1), streakCard())
    }

    @Test
    fun theGridFollowsTheRulesAndKeepsEachAnswerAsItWasGraded() {
        showAccuracy(listOf(answer(right = true)))

        compose.square("16 vs A: Hit, 100% right", "H")

        rules = RuleSet.H17

        compose.square("16 vs A: Surrender, otherwise hit, 100% right", "RH")
    }
}
