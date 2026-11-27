package com.aquigs.sp21ace.ui.truecount

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.AnswerTimer
import com.aquigs.sp21ace.domain.drill.DrillRun
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.truecount.Decks
import com.aquigs.sp21ace.domain.truecount.DecksShown
import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import com.aquigs.sp21ace.domain.truecount.TrueCountCard
import com.aquigs.sp21ace.domain.truecount.TrueCountDealer
import com.aquigs.sp21ace.domain.truecount.TrueCountOptions
import com.aquigs.sp21ace.ui.flashcardBarText
import com.aquigs.sp21ace.ui.overlaps
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class TrueCountFlashcardsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val plusSeven = TrueCountCard(6, Decks(14), CountRounding.FLOOR, 7)
    private val oneDeck = TrueCountCard(6, Decks(4), CountRounding.FLOOR, -8)

    private var now = 0L

    /** The run as the composition root carries it out, timed by the test's clock, with [oneDeck] dealt after every answer. */
    private inner class FakeRun(first: TrueCountCard) : TrueCountActions {
        val answers = mutableListOf<TrueCountAnswer>()
        var options by mutableStateOf(TrueCountOptions())
        var run by mutableStateOf(DrillRun(first))
        private val dealer = TrueCountDealer(options)
        private val timer = AnswerTimer<TrueCountCard> { now }.also { it.dealt(first) }

        override fun answer(asked: TrueCountCard, number: Int) {
            run.answer(asked, number, dealer, timer.elapsed(asked), Instant.EPOCH, answers) { _, _ -> oneDeck }?.let {
                run = it.run
                timer.dealt(it.run.card)
                answers += it.answer
            }
        }

        override fun shown(card: TrueCountCard) = timer.shown(card)

        override fun interrupted() = timer.interrupted()

        override fun time(card: TrueCountCard) = timer.display(card)

        override fun clearHistory() = Unit

        override fun changeOptions(options: TrueCountOptions) {
            this.options = options
        }
    }

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun button(name: String) = compose.onNodeWithContentDescription(name)

    private fun show(
        first: TrueCountCard = plusSeven,
        configuration: DeviceConfigurationOverride = DeviceConfigurationOverride { content -> content() },
    ): FakeRun {
        val fake = FakeRun(first)

        compose.setContent {
            DeviceConfigurationOverride(configuration) {
                Sp21AceTheme {
                    TrueCountFlashcardsScreen(
                        state = TrueCountState(fake.run, fake.options, history = emptyList()),
                        settings = Settings(),
                        covered = false,
                        actions = fake,
                        onOpenDrawer = {},
                    )
                }
            }
        }
        return fake
    }

    private fun figure(caption: Int, value: String) = compose.onNode(hasText(string(caption)) and hasText(value))

    @Test
    fun theCardGivesTheRunningCountAndTheDecksLeftAndNotTheShoe() {
        show()

        figure(R.string.running_count, "+7").assertIsDisplayed()
        figure(R.string.decks_left_caption, "3.5").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.true_count_prompt_spoken)).assertIsDisplayed()
        compose.onNodeWithText("6 decks").assertDoesNotExist()
    }

    @Test
    fun theCardCanGiveTheDecksInTheDiscardTrayInstead() {
        val fake = show()

        fake.changeOptions(TrueCountOptions(decksShown = DecksShown.DISCARDED))

        figure(R.string.decks_discarded_caption, "2.5").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.decks_left_caption)).assertDoesNotExist()
    }

    @Test
    fun aRightAnswerTurnsTheBarRightTimesItAndDealsTheNextCard() {
        val fake = show()
        compose.waitForIdle()
        now += 1_200

        button(string(R.string.count_plus, 2)).performClick()

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. +7 with 3.5 decks left. True count +2. 1.2 seconds. Accuracy 100.0%").assertIsDisplayed()
        compose.onNodeWithText(flashcardBarText("+7 with 3.5 decks left", "True count +2", "1.2 s", "Accuracy 100.0%")).assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.streak_count, 1)).assertIsDisplayed()
        figure(R.string.running_count, "−8").assertIsDisplayed()
        assertEquals(listOf(TrueCountAnswer(Instant.EPOCH, plusSeven, 2, 1_200.milliseconds)), fake.answers)
    }

    @Test
    fun theLastButtonsStandForEveryCountPastThemAndAWrongAnswerSaysWhichWasGiven() {
        show(first = oneDeck)

        button("−6 or lower").assertIsDisplayed()
        button("+6 or higher").performClick()

        compose.onNodeWithText(flashcardBarText("−8 with 1 deck left", "True count −8", "You answered +6 or higher", "0.0 s", "Accuracy 0.0%")).assertIsDisplayed()
        button("−6 or lower").performClick()
        compose.onNodeWithText(flashcardBarText("−8 with 1 deck left", "True count −8", "0.0 s", "Accuracy 50.0%")).assertIsDisplayed()
    }

    // Split screen leaves the streak meter no room
    private fun assertFiguresFit(size: DpSize, meterShows: Boolean = true) {
        show(configuration = DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(2f))

        val nodes = listOf(figure(R.string.running_count, "+7"), figure(R.string.decks_left_caption, "3.5"))
        val figures = nodes.map { it.getBoundsInRoot() }
        val others = listOfNotNull(
            compose.onNodeWithContentDescription(string(R.string.streak_count, 0)).takeIf { meterShows },
            compose.onNodeWithContentDescription(string(R.string.true_count_prompt_spoken)),
            button("−6 or lower"),
            button("+6 or higher"),
        ).map { it.assertIsDisplayed().getBoundsInRoot() }

        assertEquals("cut short", nodes.map { it.getUnclippedBoundsInRoot() }, figures)
        assertFalse(figures[0].overlaps(figures[1]))
        for (figure in figures) for (other in others) assertFalse("$figure over $other", figure.overlaps(other))
        // Both as tall as each other, so neither was squeezed by the one before it
        assertEquals((figures[0].bottom - figures[0].top).value, (figures[1].bottom - figures[1].top).value, 1f)
    }

    @Test
    fun onASmallScreenAtALargeFontTheFiguresClearTheReadoutsAndThePad() = assertFiguresFit(DpSize(360.dp, 640.dp))

    @Test
    fun inHalfASplitScreenTheFiguresSitSideBySideUncut() = assertFiguresFit(DpSize(412.dp, 400.dp), meterShows = false)
}
