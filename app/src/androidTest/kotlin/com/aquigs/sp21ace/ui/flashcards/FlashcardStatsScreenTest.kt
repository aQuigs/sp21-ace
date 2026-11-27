package com.aquigs.sp21ace.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.TimeHistogram
import com.aquigs.sp21ace.domain.drill.TimeSpread
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.HandKind
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.ui.accuracy.accuracyCardTexts
import com.aquigs.sp21ace.ui.assertFits
import com.aquigs.sp21ace.ui.assertFitsOnOneLine
import com.aquigs.sp21ace.ui.cardTexts
import com.aquigs.sp21ace.ui.textLayout
import com.aquigs.sp21ace.ui.texts
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class FlashcardStatsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-11-19T12:00:00Z")

    private fun onHand(kind: HandKind, value: Int, upcard: Upcard, play: DeviationPlay, threshold: Int) =
        PlayIndex(Deviation.OnHand(IndexHand(kind, value), upcard, play), threshold)

    private val sixteenVsTen = onHand(HandKind.HARD, 16, Upcard.TEN, DeviationPlay.STAND, 0)
    private val twelveVsThree = onHand(HandKind.HARD, 12, Upcard.THREE, DeviationPlay.STAND, 2)
    private val insurance = PlayIndex(Deviation.Insurance, 3)
    private val tenVsTen = onHand(HandKind.HARD, 10, Upcard.TEN, DeviationPlay.DOUBLE, 4)
    private val fifteenVsTen = onHand(HandKind.HARD, 15, Upcard.TEN, DeviationPlay.SURRENDER, 0)

    // Double 10 vs 10 is switched on but never answered, and surrender 15 vs 10 switched off but answered
    private val preset = IndexPreset(
        "p",
        "P",
        Game.BLACKJACK,
        basedOn = null,
        entries = listOf(IndexEntry(sixteenVsTen), IndexEntry(twelveVsThree), IndexEntry(insurance), IndexEntry(tenVsTen), IndexEntry(fifteenVsTen, enabled = false)),
    )

    private fun answer(index: PlayIndex, right: Boolean, millis: Long?, daysAgo: Long = 0) = FlashcardAnswer(
        now.minus(Duration.ofDays(daysAgo)),
        Game.BLACKJACK,
        index,
        if (right) index.threshold else index.threshold + 1,
        millis?.milliseconds,
        cards = 2,
    )

    // 16 vs 10 right 3 times in 4, 12 vs 3 once in 2 and slowly, insurance right but untimed, 15 vs 10 wrong and quick
    private val history = listOf(
        answer(sixteenVsTen, right = true, millis = 1000),
        answer(sixteenVsTen, right = true, millis = 1500),
        answer(sixteenVsTen, right = false, millis = 2000),
        answer(sixteenVsTen, right = true, millis = 3000),
        answer(twelveVsThree, right = true, millis = 4000),
        answer(twelveVsThree, right = false, millis = 6000),
        answer(insurance, right = true, millis = null),
        answer(fifteenVsTen, right = false, millis = 400),
    )

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun plural(id: Int, count: Int, vararg args: Any) = compose.activity.resources.getQuantityString(id, count, count, *args)

    private fun show(
        history: List<FlashcardAnswer> = this.history,
        modifier: Modifier = Modifier,
        configuration: DeviceConfigurationOverride = DeviceConfigurationOverride { content -> content() },
    ) {
        compose.setContent {
            DeviceConfigurationOverride(configuration) {
                Sp21AceTheme(ColorTheme.LIGHT) { FlashcardStatsScreen(history, preset, onBack = {}, modifier = modifier, now = { now }) }
            }
        }
    }

    private fun accuracyCard() = compose.cardTexts(string(R.string.accuracy), string(R.string.correct))

    private fun speedCard() = compose.onNode(hasText(string(R.string.speed)) and hasText(string(R.string.median_time)))

    // With a 90th percentile, its figure is said in words instead, as the card's description
    private fun speedTexts(median: String, p90: String?, timed: Int) = listOfNotNull(
        string(R.string.speed),
        median,
        string(R.string.median_time),
        p90,
        string(R.string.percentile_90).takeIf { p90 != null },
        "$timed",
        string(R.string.timed_answers),
    )

    // Unclipped, so a row scrolled out of view still has its place
    private fun rowTop(title: String) = compose.onNodeWithText(title).fetchSemanticsNode().positionInRoot.y

    private fun rowOrder(): List<String> = listOf("Hard 16 vs 10", "Hard 12 vs 3", "Insurance", "Hard 10 vs 10", "Hard 15 vs 10").sortedBy(::rowTop)

    private fun boxPlot(median: Double, p25: Double, p75: Double, p10: Double, p90: Double) =
        hasContentDescription(string(R.string.box_plot_description, median, p25, p75, p10, p90))

    private val overallHistogram
        get() = plural(
            R.plurals.histogram_description,
            7,
            listOf(
                plural(R.plurals.bucket_under, 1, 0.5),
                plural(R.plurals.bucket_between, 1, 1.0, 1.5),
                plural(R.plurals.bucket_between, 1, 1.5, 2.0),
                plural(R.plurals.bucket_between, 1, 2.0, 2.5),
                plural(R.plurals.bucket_between, 1, 3.0, 3.5),
                plural(R.plurals.bucket_between, 1, 4.0, 4.5),
                plural(R.plurals.bucket_overflow, 1, 5.0),
            ).joinToString(", "),
        )

    @Test
    fun theCardsCountEveryAnswerToThePresetsIndicesForAccuracyAndOnlyTheTimedOnesForSpeed() {
        show()

        compose.onNodeWithText(string(R.string.today)).assertIsSelected()
        assertEquals(compose.activity.accuracyCardTexts(R.string.accuracy, "62.5%", correct = 5, incorrect = 3), accuracyCard())
        // The median of 7 times is the 4th, 2 s, and the 90th percentile 4.8 s, between the 6th and 7th
        assertEquals(speedTexts("2.0 s", p90 = null, timed = 7), speedCard().texts())
        speedCard().assert(hasContentDescription(string(R.string.percentile_90_spoken, 4.8)))
        compose.onNode(hasContentDescription(overallHistogram)).assertExists()
    }

    @Test
    fun everyIndexSwitchedOnAndEachAnsweredHasARowWithItsFiguresAndItsBoxPlotLeastAccurateFirst() {
        show()

        assertEquals(listOf("Hard 15 vs 10", "Hard 12 vs 3", "Hard 16 vs 10", "Insurance", "Hard 10 vs 10"), rowOrder())
        compose.onNode(hasText("Hard 16 vs 10") and hasClickAction()).performScrollTo().assert(
            hasText("Stand at 0 or higher") and hasText(string(R.string.index_accuracy, "75.0%")) and hasText("4 answers") and
                hasText(string(R.string.index_median, "1.8 s")) and boxPlot(1.75, 1.375, 2.25, 1.15, 2.7),
        )
        compose.onNode(hasText("Hard 15 vs 10")).assert(hasText(string(R.string.index_switched_off, "Surrender at 0 or higher")) and hasText("1 answer"))
        compose.onNode(hasText("Insurance")).assert(hasText(string(R.string.no_timed_answers)))
        compose.onNode(hasText("Hard 10 vs 10")).assert(hasText(string(R.string.no_answers)))
    }

    @Test
    fun slowestPutsTheLongestMedianFirstThenTheUntimedThenTheUnanswered() {
        show()

        compose.onNodeWithText(string(R.string.slowest)).performScrollTo().performClick().assertIsSelected()

        assertEquals(listOf("Hard 12 vs 3", "Hard 16 vs 10", "Hard 15 vs 10", "Insurance", "Hard 10 vs 10"), rowOrder())
    }

    private val twelveVsThreeHistogram
        get() = hasContentDescription(
            plural(
                R.plurals.histogram_description,
                2,
                listOf(plural(R.plurals.bucket_between, 1, 4.0, 4.5), plural(R.plurals.bucket_overflow, 1, 5.0)).joinToString(", "),
            ),
        )

    private fun twelveVsThreeRow() = compose.onNode(hasText("Hard 12 vs 3") and hasClickAction()).performScrollTo()

    @Test
    fun tappingARowOpensItsOwnHistogramAndTappingAgainClosesItAndAScreenReaderHearsWhichItIs() {
        show()
        val row = twelveVsThreeRow()

        compose.onNode(twelveVsThreeHistogram).assertDoesNotExist()
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.collapsed)))
        row.performClick()
        compose.onNode(twelveVsThreeHistogram).assertExists()
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, string(R.string.expanded)))
        row.performClick()
        compose.onNode(twelveVsThreeHistogram).assertDoesNotExist()

        // Insurance has no timed answer, so nothing to open
        compose.onNode(hasText("Insurance")).assert(hasClickAction().not() and SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
    }

    @Test
    fun aRowOpenedStaysOpenThroughAPeriodWithNoAnswers() {
        show(history.map { it.copy(answeredAt = now.minus(Duration.ofDays(3))) })
        compose.onNodeWithText(string(R.string.all_time)).performClick()
        twelveVsThreeRow().performClick()

        compose.onNodeWithText(string(R.string.today)).performScrollTo().performClick()
        compose.onNodeWithText(string(R.string.no_answers_in_period)).assertExists()
        compose.onNodeWithText(string(R.string.all_time)).performScrollTo().performClick()

        compose.onNode(twelveVsThreeHistogram).assertExists()
    }

    @Test
    fun theChartsDrawInAWindowNarrowerThanTheirLabelsAtFontScale2() {
        // A five-digit count and an index past the axis end, in a width of a few dp and of none
        val histogram = TimeHistogram(listOf(12_345) + List(TimeHistogram.BUCKETS) { 1 })
        val spread = TimeSpread(12.seconds, 13.seconds, 14.seconds, 15.seconds, 20.seconds)
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
                Sp21AceTheme(ColorTheme.LIGHT) {
                    Column {
                        for (width in listOf(0.dp, 4.dp)) {
                            TimeHistogramChart(histogram, Modifier.width(width))
                            TimeAxis(10.seconds, Modifier.width(width))
                            TimeSpreadStrip(spread, 10.seconds, Modifier.width(width))
                        }
                    }
                }
            }
        }

        compose.waitForIdle()
        compose.onAllNodes(hasContentDescription(string(R.string.box_plot_description, 14.0, 13.0, 15.0, 12.0, 20.0))).assertCountEquals(2)
    }

    @Test
    fun aPeriodWithNoAnswersShowsNoFiguresAndSaysSo() {
        show(history.map { it.copy(answeredAt = now.minus(Duration.ofDays(3))) })

        assertEquals(compose.activity.accuracyCardTexts(R.string.accuracy, "--", correct = 0, incorrect = 0), accuracyCard())
        assertEquals(speedTexts("--", "--", timed = 0), speedCard().texts())
        compose.onNodeWithText(string(R.string.no_answers_in_period)).assertExists()
        compose.onNodeWithText(string(R.string.least_accurate)).assertDoesNotExist()

        compose.onNodeWithText(string(R.string.week)).performClick()
        assertEquals(compose.activity.accuracyCardTexts(R.string.accuracy, "62.5%", correct = 5, incorrect = 3), accuracyCard())
    }

    @Test
    fun answersWithNoneTimedCountForAccuracyAndTheSpeedCardSaysNoneWasTimed() {
        show(history.map { it.copy(time = null) })

        assertEquals(compose.activity.accuracyCardTexts(R.string.accuracy, "62.5%", correct = 5, incorrect = 3), accuracyCard())
        assertEquals(speedTexts("--", "--", timed = 0) + string(R.string.none_timed), speedCard().texts())
        compose.onNode(hasText("Hard 16 vs 10")).performScrollTo().assert(hasText(string(R.string.no_timed_answers)))
    }

    @Test
    fun atFontScale2OnANarrowPhoneTheRowsFiguresAndTheSortButtonsFit() {
        show(modifier = Modifier.size(360.dp, 640.dp), configuration = DeviceConfigurationOverride.FontScale(2f))

        fun layout(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).performScrollTo().fetchSemanticsNode().textLayout()

        for (text in listOf(string(R.string.least_accurate), string(R.string.slowest))) layout(text).assertFitsOnOneLine()
        for (text in listOf(string(R.string.index_median, "1.8 s"), "4 answers", "Hard 16 vs 10")) layout(text).assertFits()
    }
}
