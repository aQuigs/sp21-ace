package com.aquigs.sp21ace.ui.accuracy

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.ui.theme.HeatmapColors
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class AccuracyHeatmapTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-09-17T12:00:00Z")

    // Hard 16 vs A is a hit when the dealer stands on soft 17, and a surrender when the dealer hits
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))

    private var rules by mutableStateOf(RuleSet.S17)
    private lateinit var heatmap: HeatmapColors
    private var page = Color.Unspecified

    private fun string(id: Int) = compose.activity.getString(id)

    private fun answer(hand: TrainerHand, right: Boolean, correctMove: Move = Move.HIT) =
        PracticeAnswer(now, RuleSet.S17, hand, if (right) correctMove else Move.entries.first { it != correctMove }, correctMove)

    private fun showAccuracy(history: List<PracticeAnswer>) {
        compose.setContent {
            Sp21AceTheme(darkTheme = false) {
                heatmap = Sp21AceTheme.colors.heatmap
                page = MaterialTheme.colorScheme.background
                AccuracyScreen(history, rules, onBack = {}, now = { now })
            }
        }
    }

    private fun tap(title: Int) = compose.onNodeWithText(string(title)).performClick()

    // Many squares print the same code, so a square is found by what it reads out
    private fun square(description: String, code: String) = compose.onNode(hasContentDescription(description) and hasText(code)).performScrollTo()

    // Near a corner, clear of the code in the middle
    private fun SemanticsNodeInteraction.fill(): Color = captureToImage().toPixelMap().let { it[it.width / 8, it.height / 8] }

    // Within a shade, since the capture rounds each channel to 8 bits
    private fun assertColour(expected: Color, actual: Color) {
        val difference = listOf(expected.red - actual.red, expected.green - actual.green, expected.blue - actual.blue).maxOf(::abs)
        assertTrue("expected $expected, was $actual", difference < 2 / 255f)
    }

    @Test
    fun aSquareWithAnswersIsFilledOnTheScaleAndReadsItsAccuracy() {
        showAccuracy(
            listOf(
                answer(sixteenVsAce, right = true),
                answer(TrainerHand(cards("Kd 6h"), card("Ac")), right = true),
                answer(TrainerHand(cards("Qs 6c"), card("Ah")), right = true),
                answer(TrainerHand(cards("Jh 6s"), card("Ad")), right = false),
            ),
        )

        assertColour(heatmap.at(0.75f), square("16 vs A: Hit, 75% right", "H").fill())
    }

    @Test
    fun aSquareWithoutAnswersReadsNoAnswersAndStaysPlain() {
        showAccuracy(listOf(answer(sixteenVsAce, right = false)))

        assertColour(page, square("16 vs 10: Hit, no answers", "H").fill())
        assertColour(heatmap.at(0f), square("16 vs A: Hit, 0% right", "H").fill())
    }

    @Test
    fun eachKindOfHandHasItsOwnGridAndTheAllTabHasNone() {
        showAccuracy(
            listOf(
                answer(sixteenVsAce, right = true),
                answer(TrainerHand(cards("As 6d"), card("Kh")), right = true),
                answer(TrainerHand(cards("8h 8s"), card("6d")), right = true, correctMove = Move.SPLIT),
            ),
        )

        square("16 vs A: Hit, 100% right", "H")

        tap(R.string.table_soft)

        square("A-6 vs 10: Hit, 100% right", "H")

        tap(R.string.table_pairs)

        square("8-8 vs 6: Split, 100% right", "P")

        tap(R.string.all_hands)

        compose.onNodeWithText(string(R.string.dealers_upcard)).assertDoesNotExist()
        compose.onNode(hasContentDescription(" vs ", substring = true)).assertDoesNotExist()
    }

    @Test
    fun theGridFollowsTheRulesAndKeepsEachAnswerAsItWasGraded() {
        showAccuracy(listOf(answer(sixteenVsAce, right = true)))

        square("16 vs A: Hit, 100% right", "H")

        rules = RuleSet.H17

        square("16 vs A: Surrender, 100% right", "RH")
        compose.onNode(hasContentDescription("16 vs A: Hit, 100% right")).assertDoesNotExist()
    }
}
