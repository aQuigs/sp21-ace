package com.aquigs.sp21ace.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.drill.AnswerTimer
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.Flashcard
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.FlashcardDealer
import com.aquigs.sp21ace.domain.indices.FlashcardOptions
import com.aquigs.sp21ace.domain.indices.FlashcardRun
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.HandKind
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.NoIndex
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.ui.assertFits
import com.aquigs.sp21ace.ui.assertFitsOnOneLine
import com.aquigs.sp21ace.ui.flashcardBarText
import com.aquigs.sp21ace.ui.overlaps
import com.aquigs.sp21ace.ui.textLayout
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(AndroidJUnit4::class)
class IndexFlashcardsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Shoe H17's numbers: stand on hard 16 vs 10 at 0 or higher, and insure at +3 or higher
    private val h17 = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_H17, "h17")
    private val sixteenVsTen = h17.entries.first { (it.index.deviation as? Deviation.OnHand)?.hand == IndexHand(HandKind.HARD, 16) && it.index.deviation.upcard == Upcard.TEN }.index
    private val insurance = PlayIndex(Deviation.Insurance, 3)
    private val sixteenVsKing = Flashcard(sixteenVsTen, cards("9c 7d"), card("Kh"))
    private val insuranceCard = Flashcard(insurance, emptyList(), card("As"))

    private var now = 0L
    private var settings by mutableStateOf(Settings())
    private var options by mutableStateOf(FlashcardOptions())
    private var covered by mutableStateOf(false)
    private var onScreen by mutableStateOf(true)
    private var windowFocused by mutableStateOf(true)
    private var indicesOpened = 0

    /** The run as the composition root carries it out, timed by the test's clock, with only [deals] left to deal after [first]. */
    private inner class FakeRun(preset: IndexPreset, first: Flashcard, deals: List<Flashcard>) : IndexFlashcardsActions {
        val presets = IndexPresets(listOf(preset), preset.id)
        val answers = mutableListOf<FlashcardAnswer>()
        var run by mutableStateOf(FlashcardRun(first))
        private val dealer = FlashcardDealer(preset, options)
        private val timer = AnswerTimer<Flashcard> { now }.also { it.dealt(first) }
        private val next = deals.iterator()

        override fun answer(asked: Flashcard, answer: Int?) {
            run.answer(asked, answer, dealer, timer.elapsed(asked), Instant.EPOCH, answers) { _, _ -> next.next() }?.let {
                run = it.run
                timer.dealt(it.run.card)
                answers += it.answer
            }
        }

        override fun shown(card: Flashcard) = timer.shown(card)

        override fun interrupted() = timer.interrupted()

        override fun time(card: Flashcard) = timer.display(card)

        override fun changePresets(presets: IndexPresets) = Unit

        override fun changeOptions(options: FlashcardOptions) = Unit

        override fun clearHistory() = Unit
    }

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun button(number: Int) = compose.onNodeWithContentDescription(
        when {
            number > 0 -> string(R.string.count_plus, number)
            number < 0 -> string(R.string.count_minus, -number)
            else -> "0"
        },
    )

    private fun bounds(description: String) = compose.onNodeWithContentDescription(description).getBoundsInRoot()

    private fun show(
        preset: IndexPreset = h17,
        first: Flashcard = sixteenVsKing,
        deals: List<Flashcard> = listOf(insuranceCard),
        modifier: Modifier = Modifier,
        configuration: DeviceConfigurationOverride = DeviceConfigurationOverride { content -> content() },
    ): FakeRun {
        val fake = FakeRun(preset, first, deals)

        compose.setContent {
            // The window's focus as the test sets it, everything else about the window as it is
            val window = LocalWindowInfo.current
            val focus = remember(window) {
                object : WindowInfo {
                    override val isWindowFocused get() = windowFocused
                    override val containerSize get() = window.containerSize
                }
            }

            DeviceConfigurationOverride(configuration) {
                CompositionLocalProvider(LocalWindowInfo provides focus) {
                    Sp21AceTheme {
                        if (onScreen) {
                            IndexFlashcardsScreen(
                                state = IndexFlashcardsState(fake.run, fake.presets, options, history = emptyList()),
                                settings = settings,
                                covered = covered,
                                actions = fake,
                                onOpenDrawer = {},
                                onOpenIndices = { indicesOpened++ },
                                modifier = modifier,
                            )
                        }
                    }
                }
            }
        }
        return fake
    }

    private fun oneIndex(index: PlayIndex) = IndexPreset("one", "One", Game.BLACKJACK, basedOn = null, entries = listOf(IndexEntry(index)))

    @Test
    fun theCardAsksForThePlaysNumberInCapitals() {
        show()

        compose.onNodeWithText("STAND at ? or higher").assertIsDisplayed()
        compose.onNodeWithContentDescription("Stand at what true count or higher?").assertIsDisplayed()
        compose.onNodeWithContentDescription("9 of clubs").assertIsDisplayed()
        compose.onNodeWithContentDescription("King of hearts").assertIsDisplayed()
        compose.onNodeWithContentDescription(string(R.string.face_down_card)).assertIsDisplayed()
    }

    @Test
    fun aRightAnswerTurnsTheBarRightAndDealsTheNextCard() {
        show()

        button(0).performClick()

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs 10. Stand at 0 or higher. 0.0 seconds. Accuracy 100.0%").assertIsDisplayed()
        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "0.0 s", "Accuracy 100.0%")).assertIsDisplayed()
        compose.onNodeWithText("INSURANCE at ? or higher").assertIsDisplayed()
    }

    @Test
    fun aWrongAnswerTurnsTheBarWrongAndSaysWhatWasAnswered() {
        show()

        button(2).performClick()

        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. Hard 16 vs 10. Stand at 0 or higher. You answered +2. 0.0 seconds. Accuracy 0.0%").assertIsDisplayed()
        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "You answered +2", "0.0 s", "Accuracy 0.0%")).assertIsDisplayed()
    }

    @Test
    fun theBarSaysHowOftenTheIndexHasBeenAnsweredRightCountingThisAnswer() {
        val otherSixteen = sixteenVsKing.copy(player = cards("10s 6h"))
        show(deals = listOf(insuranceCard, otherSixteen, insuranceCard))

        // 16 vs 10 right, insurance wrong, then 16 vs 10 wrong
        for (number in listOf(0, 1, 2)) button(number).performClick()

        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "You answered +2", "0.0 s", "Accuracy 50.0%")).assertIsDisplayed()
    }

    @Test
    fun theBarSaysHowLongTheAnswerTookAndKeepsItWhileTheNextCardsTimeRuns() {
        show()
        compose.waitForIdle()
        now += 2_400

        button(0).performClick()
        now += 5_000
        tick()

        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "2.4 s", "Accuracy 100.0%")).assertIsDisplayed()
        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs 10. Stand at 0 or higher. 2.4 seconds. Accuracy 100.0%").assertIsDisplayed()
    }

    @Test
    fun theBarLeavesOutTheTimeOfACardThatWasntTimed() {
        show()
        covered = true
        compose.waitForIdle()
        covered = false

        button(0).performClick()

        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "Accuracy 100.0%")).assertIsDisplayed()
    }

    @Test
    fun switchingTheTimerOffTakesTheTimeOffTheBar() {
        show()
        button(0).performClick()
        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "0.0 s", "Accuracy 100.0%")).assertIsDisplayed()

        settings = Settings(flashcardTimer = false)

        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "Accuracy 100.0%")).assertIsDisplayed()
    }

    @Test
    fun insuranceShowsTheDealersAceAloneAndNamesItselfOnce() {
        show(first = insuranceCard, deals = listOf(sixteenVsKing))

        compose.onNodeWithText("INSURANCE at ? or higher").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ace of spades").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.you)).assertDoesNotExist()

        button(-3).performClick()

        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. Insurance at +3 or higher. You answered −3. 0.0 seconds. Accuracy 0.0%").assertIsDisplayed()
        compose.onNodeWithText(flashcardBarText("Insurance at +3 or higher", "You answered −3", "0.0 s", "Accuracy 0.0%")).assertIsDisplayed()
    }

    @Test
    fun aScreenReaderHearsEachVerdictFromTheSameNode() {
        show(deals = listOf(insuranceCard, sixteenVsKing))
        val verdict = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))

        button(0).performClick()
        val right = verdict.fetchSemanticsNode()
        button(1).performClick()
        val wrong = verdict.fetchSemanticsNode()

        assertEquals(right.id, wrong.id)
        assertEquals(LiveRegionMode.Polite, wrong.config[SemanticsProperties.LiveRegion])
        assertTrue(wrong.config[SemanticsProperties.ContentDescription].single().startsWith(string(R.string.wrong_answer)))
    }

    @Test
    fun aSecondTapBeforeTheNextCardIsDrawnIsIgnored() {
        val fake = show()
        compose.mainClock.autoAdvance = false

        button(0).performSemanticsAction(SemanticsActions.OnClick)
        button(1).performSemanticsAction(SemanticsActions.OnClick)
        compose.mainClock.autoAdvance = true

        assertEquals(listOf(0), fake.answers.map { it.answer })
        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "0.0 s", "Accuracy 100.0%")).assertIsDisplayed()
    }

    private val sixteenVsEight = Flashcard(
        NoIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 16), Upcard.EIGHT, DeviationPlay.STAND)),
        cards("9c 7d"),
        card("8h"),
    )

    private fun noIndexButton() = compose.onNodeWithContentDescription(string(R.string.no_index))

    @Test
    fun theNoIndexButtonShowsOnlyWhileHandsWithNoIndexAreOn() {
        show()
        noIndexButton().assertDoesNotExist()

        options = FlashcardOptions(noIndexCards = true)

        noIndexButton().assertIsDisplayed()
    }

    @Test
    fun aHandWithNoIndexAsksAsAnIndexDoesAndNoIndexIsItsRightAnswer() {
        options = FlashcardOptions(noIndexCards = true)
        val fake = show(first = sixteenVsEight)
        compose.onNodeWithText("STAND at ? or higher").assertIsDisplayed()

        noIndexButton().performClick()

        compose.onNodeWithText(flashcardBarText("Hard 16 vs 8", "No index", "0.0 s", "Accuracy 100.0%")).assertIsDisplayed()
        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs 8. No index. 0.0 seconds. Accuracy 100.0%").assertIsDisplayed()
        assertEquals(listOf(null), fake.answers.map { it.answer })
    }

    @Test
    fun aNumberForAHandWithNoIndexAndNoIndexForAnIndexAreWrong() {
        options = FlashcardOptions(noIndexCards = true)
        show(first = sixteenVsEight, deals = listOf(sixteenVsKing, insuranceCard))

        button(2).performClick()
        compose.onNodeWithText(flashcardBarText("Hard 16 vs 8", "No index", "You answered +2", "0.0 s", "Accuracy 0.0%")).assertIsDisplayed()

        noIndexButton().performClick()
        compose.onNodeWithText(flashcardBarText("Hard 16 vs 10", "Stand at 0 or higher", "You answered no index", "0.0 s", "Accuracy 0.0%")).assertIsDisplayed()
    }

    @Test
    fun handTotalsShowAndHide() {
        show()
        compose.onNodeWithText("16").assertDoesNotExist()

        settings = Settings(handTotals = true)

        compose.onNodeWithText("16").assertIsDisplayed()
        compose.onNodeWithText("10").assertIsDisplayed()
    }

    @Test
    fun withNothingSwitchedOnTheScreenSaysSoAndOffersTheWayToIndicesInPlaceOfThePad() {
        show(preset = h17.copy(entries = h17.entries.map { it.copy(enabled = false) })).run = FlashcardRun(null)

        compose.onNodeWithText(string(R.string.no_indices_switched_on)).assertIsDisplayed()
        button(0).assertDoesNotExist()

        compose.onNodeWithText(string(R.string.choose_indices)).assertIsDisplayed().performClick()

        assertEquals(1, indicesOpened)
    }

    @Test
    fun anAnswerIsTimedFromWhenTheCardFirstShows() {
        val fake = show()
        now += 2_350

        button(0).performClick()

        assertEquals(2_350.milliseconds, fake.answers.single().time)
    }

    private fun assertInterruptionLeavesTheAnswerUntimed(interrupt: () -> Unit) {
        val fake = show(deals = listOf(insuranceCard, sixteenVsKing))
        now += 1_000
        interrupt()
        now += 1_000
        button(0).performClick()

        // The next card is timed afresh, from its first frame
        compose.waitForIdle()
        now += 500
        button(3).performClick()

        assertEquals(listOf<Duration?>(null, 500.milliseconds), fake.answers.map { it.time })
    }

    @Test
    fun theDrawerOverTheCardLeavesItsAnswerUntimed() = assertInterruptionLeavesTheAnswerUntimed {
        covered = true
        compose.waitForIdle()
        covered = false
        compose.waitForIdle()
    }

    @Test
    fun theAppPausedLeavesTheCardsAnswerUntimed() = assertInterruptionLeavesTheAnswerUntimed {
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
    }

    @Test
    fun theWindowLosingFocusLeavesTheCardsAnswerUntimed() = assertInterruptionLeavesTheAnswerUntimed {
        // As under the notification shade or in split screen, where the activity stays resumed
        windowFocused = false
        compose.waitForIdle()
        windowFocused = true
        compose.waitForIdle()
    }

    @Test
    fun anotherPageOpenedLeavesTheCardsAnswerUntimed() = assertInterruptionLeavesTheAnswerUntimed {
        onScreen = false
        compose.waitForIdle()
        onScreen = true
        compose.waitForIdle()
    }

    @Test
    fun aCardFirstShownUnderTheDrawerIsTimedOnceItCloses() {
        // As when the drawer opens the page, and is still closing as the card first shows
        covered = true
        val fake = show()
        now += 1_000
        covered = false
        compose.waitForIdle()
        now += 700

        button(0).performClick()

        assertEquals(700.milliseconds, fake.answers.single().time)
    }

    private fun cardTime(seconds: Int) = compose.onNodeWithContentDescription(compose.activity.resources.getQuantityString(R.plurals.card_time_spoken, seconds, seconds))

    private fun untimed() = compose.onNodeWithContentDescription(string(R.string.card_untimed))

    private fun streak(count: Int) = compose.onNodeWithContentDescription(string(R.string.streak_count, count))

    // Long enough for the time to be read again
    private fun tick() {
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
    }

    @Test
    fun theTimerShowsTheTimeTheAnswerRecordsAndStartsOverWithTheNextCard() {
        val fake = show()
        cardTime(0).assertIsDisplayed()

        now += 2_300
        tick()
        cardTime(2).assertIsDisplayed()

        button(0).performClick()
        cardTime(0).assertIsDisplayed()
        assertEquals(2_300.milliseconds, fake.answers.single().time)

        now += 1_000
        tick()
        cardTime(1).assertIsDisplayed()
    }

    @Test
    fun theTimerSaysAnInterruptedCardIsNotTimedAndTimesTheNext() {
        show()
        now += 1_000
        tick()
        covered = true
        tick()
        untimed().assertIsDisplayed()
        covered = false
        tick()
        untimed().assertIsDisplayed()

        button(0).performClick()
        compose.waitForIdle()
        now += 1_000
        tick()
        cardTime(1).assertIsDisplayed()
    }

    @Test
    fun theTimerWaitsAtZeroForTheDrawerToClose() {
        covered = true
        show()
        now += 1_000
        tick()
        cardTime(0).assertIsDisplayed()

        covered = false
        compose.waitForIdle()
        now += 1_000
        tick()
        cardTime(1).assertIsDisplayed()
    }

    @Test
    fun theTimerSaysACardIsNotTimedAsTheDrawerClosesOverThePageReopened() {
        show()
        onScreen = false
        compose.waitForIdle()
        covered = true
        onScreen = true
        compose.waitForIdle()

        untimed().assertIsDisplayed()
    }

    @Test
    fun theTimerSaysACardIsNotTimedWhileAnotherWindowHasFocus() {
        show()
        now += 1_000
        windowFocused = false
        tick()

        untimed().assertIsDisplayed()
    }

    @Test
    fun rightAnswersClimbTheStreakAndAWrongOneDropsItToZero() {
        show(deals = listOf(insuranceCard, sixteenVsKing, insuranceCard))
        streak(0).assertIsDisplayed()

        button(0).performClick()
        button(3).performClick()
        streak(2).assertIsDisplayed()

        button(1).performClick()
        streak(0).assertIsDisplayed()
    }

    @Test
    fun theTimerAndTheStreakMeterHideOnTheirOwnAndAHiddenTimerStillTimesTheAnswer() {
        settings = Settings(flashcardTimer = false)
        val fake = show()
        cardTime(0).assertDoesNotExist()
        streak(0).assertIsDisplayed()

        settings = Settings(streakMeter = false)
        tick()
        cardTime(0).assertIsDisplayed()
        streak(0).assertDoesNotExist()

        settings = Settings(flashcardTimer = false, streakMeter = false)
        compose.waitForIdle()
        cardTime(0).assertDoesNotExist()
        now += 900
        button(0).performClick()
        assertEquals(900.milliseconds, fake.answers.single().time)
    }

    @Test
    fun theStreakMeterKeepsTheTrainersEdgeWithTheTimeAboveIt() {
        show()
        fun assertMeterIs(leftOfTheCards: Boolean) {
            val meter = streak(0).getBoundsInRoot()
            val time = cardTime(0).getBoundsInRoot()
            val cards = listOf(string(R.string.face_down_card), "King of hearts", "9 of clubs", "7 of diamonds").map(::bounds)

            for (card in cards) assertTrue("$meter, $card", if (leftOfTheCards) meter.right <= card.left else meter.left >= card.right)
            assertTrue("$time, $meter", time.bottom <= meter.top && time.left < meter.right && meter.left < time.right)
        }

        assertMeterIs(leftOfTheCards = true)
        settings = Settings(buttonLocation = ButtonLocation.LEFT)
        compose.waitForIdle()
        assertMeterIs(leftOfTheCards = false)
    }

    // The pad as a 360dp-wide phone lays it out: a button for every number, the negatives above zero and the positives, every
    // button at least 40dp across and clear of the cards, the prompt and each other, each label on one line, and none moving
    // as the next card is dealt
    private fun assertPadFits(reach: Int, fontScale: Float = 1f, noIndex: Boolean = false) {
        // Stand on hard 16 vs 10 at the far end of the pad, or at 0 for the pad's least reach
        val index = sixteenVsTen.copy(threshold = if (reach > 5) -reach else 0)
        val first = sixteenVsKing.copy(fact = index)
        options = FlashcardOptions(noIndexCards = noIndex)
        settings = Settings(handTotals = true)
        show(oneIndex(index), first, deals = listOf(first.copy(player = cards("10s 6h"))), Modifier.size(360.dp, 640.dp), DeviceConfigurationOverride.FontScale(fontScale))

        val numbers = -reach..reach
        val buttons = numbers.map { button(it).assertIsDisplayed().getBoundsInRoot() }
        val cards = listOf(string(R.string.face_down_card), "King of hearts", "9 of clubs", "7 of diamonds")
        val others = (cards + "Stand at what true count or higher?" + listOf(string(R.string.no_index)).filter { noIndex }).map(::bounds)

        for ((number, box) in numbers.zip(buttons)) {
            assertTrue("$number is ${box.width} wide", box.width >= 40.dp && box.height >= 40.dp)
            assertTrue("$number ends at ${box.right}", box.left >= 0.dp && box.right <= 360.dp && box.bottom <= 640.dp)
            for (other in others) assertFalse("$number overlaps $other", box.overlaps(other))
        }
        for ((i, a) in buttons.withIndex()) {
            for (b in buttons.drop(i + 1)) assertFalse("$a overlaps $b", a.overlaps(b))
        }
        assertTrue(buttons.take(reach).maxOf { it.bottom } <= buttons.drop(reach).minOf { it.top })
        val dealer = listOf(string(R.string.dealer), "10").map { compose.onNodeWithText(it).getBoundsInRoot() }
        for (readout in listOf(streak(0), cardTime(0)).map { it.getBoundsInRoot() }) {
            for (other in others + buttons + dealer) assertFalse("$readout overlaps $other", readout.overlaps(other))
        }
        assertFalse(streak(0).getBoundsInRoot().overlaps(cardTime(0).getBoundsInRoot()))
        for (number in numbers) {
            val name = button(number).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
            compose.onAllNodes(hasAnyAncestor(hasContentDescription(name)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes().single().textLayout().assertFitsOnOneLine()
        }

        if (noIndex) noIndexButton().assertIsDisplayed().getBoundsInRoot().let { assertTrue("$it", it.bottom <= 640.dp && it.height >= 40.dp) }

        button(0).performClick()
        assertEquals(buttons, numbers.map { button(it).getBoundsInRoot() })
    }

    // A wrong answer to a surrender, the longest play, at −10, after a card left up for over a quarter of an hour, on a narrow phone
    private fun assertLongestBarFits(hand: IndexHand, title: String, player: List<Card>) {
        val index = PlayIndex(Deviation.OnHand(hand, Upcard.TEN, DeviationPlay.SURRENDER), -10)
        val first = Flashcard(index, player, card("Kh"))
        options = FlashcardOptions(noIndexCards = true)
        show(oneIndex(index), first, deals = listOf(first), Modifier.size(360.dp, 640.dp), DeviceConfigurationOverride.FontScale(2f))
            .answers += List(999) { FlashcardAnswer(Instant.EPOCH, Game.BLACKJACK, index, index.threshold, null, cards = 2) }
        compose.waitForIdle()
        now += 999_900

        noIndexButton().performClick()

        val bar = flashcardBarText(title, "Surrender at −10 or higher", "You answered no index", "999.9 s", "Accuracy 99.9%")
        compose.onNodeWithText(bar, useUnmergedTree = true).fetchSemanticsNode().textLayout().assertFits()
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeTheLongestHardHandsBarFits() =
        assertLongestBarFits(IndexHand(HandKind.HARD, 15), "Hard 15 vs 10", cards("9c 6d"))

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeTheLongestPairsBarFits() =
        assertLongestBarFits(IndexHand(HandKind.PAIR, 1), "Pair of aces vs 10", cards("Ac Ad"))

    @Test
    fun onANarrowPhoneThePadOf5EitherSideFits() = assertPadFits(5)

    @Test
    fun onANarrowPhoneThePadOf6EitherSideFits() = assertPadFits(6)

    @Test
    fun onANarrowPhoneThePadOf10EitherSideFits() = assertPadFits(10)

    // The tallest pad there is
    @Test
    fun onANarrowPhoneAtTheLargestFontSizeThePadOf9EitherSideStillFits() = assertPadFits(9, fontScale = 2f)

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeThePadOf10EitherSideStillFits() = assertPadFits(10, fontScale = 2f)

    @Test
    fun onANarrowPhoneThePadOf5EitherSideFitsWithNoIndex() = assertPadFits(5, noIndex = true)

    // The tallest there is
    @Test
    fun onANarrowPhoneAtTheLargestFontSizeThePadOf10EitherSideStillFitsWithNoIndex() = assertPadFits(10, fontScale = 2f, noIndex = true)

    @Test
    fun theLabelsShowASignAndATrueMinusAndAScreenReaderSaysPlusAndMinus() {
        show()

        compose.onNodeWithText("+3", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("−2", useUnmergedTree = true).assertIsDisplayed()
        // The streak is a 0 too
        compose.onNode(hasText("0") and hasAnyAncestor(hasContentDescription("0")), useUnmergedTree = true).assertIsDisplayed()
        button(3).assertIsDisplayed()
        button(-2).assertIsDisplayed()
    }
}
