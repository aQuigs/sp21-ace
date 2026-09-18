package com.aquigs.sp21ace.ui.accuracy

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class AccuracyScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-09-17T12:00:00Z")

    // Hard 16 vs A and soft 17 vs K are both hits when the dealer stands on soft 17
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val softSeventeenVsKing = TrainerHand(cards("Ah 6d"), card("Kh"))

    private fun string(id: Int) = compose.activity.getString(id)

    private fun answer(right: Boolean, daysAgo: Long = 0, hand: TrainerHand = sixteenVsAce) =
        PracticeAnswer(now.minus(Duration.ofDays(daysAgo)), RuleSet.S17, hand, if (right) Move.HIT else Move.STAND, Move.HIT)

    private fun showAccuracy(history: List<PracticeAnswer>) {
        compose.setContent { Sp21AceTheme { AccuracyScreen(history, Clock.fixed(now, ZoneOffset.UTC), onBack = {}) } }
    }

    private fun tap(title: Int) = compose.onNodeWithText(string(title)).performClick()

    // A card is one item for a screen reader, so its texts come together: the title, then each figure before its label
    private fun cardTexts(vararg texts: String): List<String> =
        compose.onNode(texts.map(::hasText).reduce(SemanticsMatcher::and)).fetchSemanticsNode().config[SemanticsProperties.Text].map { it.text }

    private fun accuracyCard(title: Int) = cardTexts(string(title), string(R.string.correct))

    private fun figures(title: Int, accuracy: String, correct: Int, incorrect: Int) =
        listOf(string(title), accuracy, string(R.string.accuracy), "$correct", string(R.string.correct), "$incorrect", string(R.string.incorrect))

    private fun streakCard() = cardTexts(string(R.string.longest_streak))

    @Test
    fun todayIsTheDefaultAndTheChipsSwitchThePeriod() {
        showAccuracy(listOf(answer(right = true), answer(right = false, daysAgo = 3), answer(right = false, daysAgo = 20), answer(right = true, daysAgo = 60)))

        compose.onNodeWithText(string(R.string.today)).assertIsSelected()
        assertEquals(figures(R.string.overall, "100.0%", correct = 1, incorrect = 0), accuracyCard(R.string.overall))

        tap(R.string.week).assertIsSelected()
        assertEquals(figures(R.string.overall, "50.0%", correct = 1, incorrect = 1), accuracyCard(R.string.overall))

        tap(R.string.month).assertIsSelected()
        assertEquals(figures(R.string.overall, "33.3%", correct = 1, incorrect = 2), accuracyCard(R.string.overall))

        tap(R.string.all_time).assertIsSelected()
        assertEquals(figures(R.string.overall, "50.0%", correct = 2, incorrect = 2), accuracyCard(R.string.overall))
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
    fun withNoAnswersEveryFigureIsNoData() {
        showAccuracy(emptyList())

        for (title in listOf(R.string.overall, R.string.move_hit, R.string.move_double, R.string.move_stand, R.string.move_surrender)) {
            assertEquals(figures(title, "--", correct = 0, incorrect = 0), accuracyCard(title))
        }
        // No hard hand calls for a split
        compose.onNodeWithText(string(R.string.move_split)).assertDoesNotExist()

        tap(R.string.all_hands)

        assertEquals(figures(R.string.move_split, "--", correct = 0, incorrect = 0), accuracyCard(R.string.move_split))
        assertEquals(listOf(string(R.string.streak), "--", string(R.string.longest_streak)), streakCard())
    }

    @Test
    fun onlyTheAllTabHasTheLongestStreakInThePeriod() {
        // Three right last week, then a wrong answer and two right today, the second a soft hand
        showAccuracy(
            List(3) { answer(right = true, daysAgo = 3) } +
                listOf(answer(right = false), answer(right = true), answer(right = true, hand = softSeventeenVsKing)),
        )

        compose.onNodeWithText(string(R.string.longest_streak)).assertDoesNotExist()

        tap(R.string.all_hands)

        assertEquals(listOf(string(R.string.streak), "2", string(R.string.longest_streak)), streakCard())

        tap(R.string.week)

        assertEquals(listOf(string(R.string.streak), "3", string(R.string.longest_streak)), streakCard())
    }
}
