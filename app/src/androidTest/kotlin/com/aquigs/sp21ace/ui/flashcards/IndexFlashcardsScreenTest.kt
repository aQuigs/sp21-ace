package com.aquigs.sp21ace.ui.flashcards

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.indices.AnswerTimer
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.Flashcard
import com.aquigs.sp21ace.domain.indices.FlashcardAnswer
import com.aquigs.sp21ace.domain.indices.FlashcardDealer
import com.aquigs.sp21ace.domain.indices.FlashcardRun
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.HandKind
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.indices.answer
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.ui.assertFitsOnOneLine
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
    private var covered by mutableStateOf(false)
    private var onScreen by mutableStateOf(true)

    /** The run as the composition root carries it out, timed by the test's clock, with only [deals] left to deal after [first]. */
    private inner class FakeRun(preset: IndexPreset, first: Flashcard, deals: List<Flashcard>) : IndexFlashcardsActions {
        val presets = IndexPresets(listOf(preset), preset.id)
        val answers = mutableListOf<FlashcardAnswer>()
        var run by mutableStateOf(FlashcardRun(first))
        private val dealer = FlashcardDealer(preset)
        private val timer = AnswerTimer { now }.also { it.dealt(first) }
        private val next = deals.iterator()

        override fun answer(asked: Flashcard, number: Int) {
            run.answer(asked, number, dealer, timer.elapsed(asked), Instant.EPOCH) { next.next() }?.let {
                run = it.run
                timer.dealt(it.run.card)
                answers += it.answer
            }
        }

        override fun shown(card: Flashcard) = timer.shown(card)

        override fun interrupted() = timer.interrupted()
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

    private fun DpRect.overlaps(other: DpRect) = left < other.right && other.left < right && top < other.bottom && other.top < bottom

    private fun show(
        preset: IndexPreset = h17,
        first: Flashcard = sixteenVsKing,
        deals: List<Flashcard> = listOf(insuranceCard),
        modifier: Modifier = Modifier,
        configuration: DeviceConfigurationOverride = DeviceConfigurationOverride { content -> content() },
    ): FakeRun {
        val fake = FakeRun(preset, first, deals)

        compose.setContent {
            DeviceConfigurationOverride(configuration) {
                Sp21AceTheme {
                    if (onScreen) {
                        IndexFlashcardsScreen(
                            state = IndexFlashcardsState(fake.run, fake.presets, fake.answers.toList()),
                            settings = settings,
                            covered = covered,
                            actions = fake,
                            onOpenDrawer = {},
                            modifier = modifier,
                        )
                    }
                }
            }
        }
        return fake
    }

    // A preset of the one index, stand on hard 16 vs 10, at [threshold]
    private fun oneIndex(threshold: Int): Pair<IndexPreset, Flashcard> {
        val index = sixteenVsTen.copy(threshold = threshold)
        return IndexPreset("one", "One", Game.BLACKJACK, basedOn = null, entries = listOf(IndexEntry(index))) to sixteenVsKing.copy(index = index)
    }

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

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Hard 16 vs 10. Stand at 0 or higher").assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs 10 | Stand at 0 or higher").assertIsDisplayed()
        compose.onNodeWithText("INSURANCE at ? or higher").assertIsDisplayed()
    }

    @Test
    fun aWrongAnswerTurnsTheBarWrongAndSaysWhatWasAnswered() {
        show()

        button(2).performClick()

        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. Hard 16 vs 10. Stand at 0 or higher. You answered +2").assertIsDisplayed()
        compose.onNodeWithText("Hard 16 vs 10 | Stand at 0 or higher | You answered +2").assertIsDisplayed()
    }

    @Test
    fun insuranceShowsTheDealersAceAloneAndNamesItselfOnce() {
        show(first = insuranceCard, deals = listOf(sixteenVsKing))

        compose.onNodeWithText("INSURANCE at ? or higher").assertIsDisplayed()
        compose.onNodeWithContentDescription("Ace of spades").assertIsDisplayed()
        compose.onNodeWithText(string(R.string.you)).assertDoesNotExist()

        button(-3).performClick()

        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. Insurance at +3 or higher. You answered −3").assertIsDisplayed()
        compose.onNodeWithText("Insurance at +3 or higher | You answered −3").assertIsDisplayed()
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
        compose.onNodeWithText("Hard 16 vs 10 | Stand at 0 or higher").assertIsDisplayed()
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
    fun withNothingSwitchedOnTheScreenSaysSoAndOffersNoPad() {
        show(preset = h17.copy(entries = h17.entries.map { it.copy(enabled = false) })).run = FlashcardRun(null)

        compose.onNodeWithText(string(R.string.no_indices_switched_on)).assertIsDisplayed()
        button(0).assertDoesNotExist()
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
        assertEquals(2, fake.answers.size)
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

    // The pad as a 360dp-wide phone lays it out: a button for every number, the negatives above zero and the positives, every
    // button at least 40dp across and clear of the cards, the prompt and each other, each label on one line, and none moving
    // as the next card is dealt
    private fun assertPadFits(reach: Int, fontScale: Float = 1f) {
        val (preset, first) = oneIndex(if (reach > 5) -reach else 0)
        show(preset, first, deals = listOf(first.copy(player = cards("10s 6h"))), Modifier.size(360.dp, 640.dp), DeviceConfigurationOverride.FontScale(fontScale))

        val numbers = -reach..reach
        val buttons = numbers.map { button(it).assertIsDisplayed().getBoundsInRoot() }
        val others = listOf(string(R.string.face_down_card), "King of hearts", "9 of clubs", "7 of diamonds", "Stand at what true count or higher?").map(::bounds)

        for ((number, box) in numbers.zip(buttons)) {
            assertTrue("$number is ${box.width} wide", box.width >= 40.dp && box.height >= 40.dp)
            assertTrue("$number ends at ${box.right}", box.left >= 0.dp && box.right <= 360.dp && box.bottom <= 640.dp)
            for (other in others) assertFalse("$number overlaps $other", box.overlaps(other))
        }
        for ((i, a) in buttons.withIndex()) {
            for (b in buttons.drop(i + 1)) assertFalse("$a overlaps $b", a.overlaps(b))
        }
        assertTrue(buttons.take(reach).maxOf { it.bottom } <= buttons.drop(reach).minOf { it.top })
        for (number in numbers) {
            val name = button(number).fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
            compose.onAllNodes(hasAnyAncestor(hasContentDescription(name)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), useUnmergedTree = true)
                .fetchSemanticsNodes().single().textLayout().assertFitsOnOneLine()
        }

        button(0).performClick()
        assertEquals(buttons, numbers.map { button(it).getBoundsInRoot() })
    }

    @Test
    fun onANarrowPhoneThePadOf5EitherSideFits() = assertPadFits(5)

    @Test
    fun onANarrowPhoneThePadOf6EitherSideFits() = assertPadFits(6)

    @Test
    fun onANarrowPhoneThePadOf10EitherSideFits() = assertPadFits(10)

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeThePadOf10EitherSideStillFits() = assertPadFits(10, fontScale = 2f)

    @Test
    fun theLabelsShowASignAndATrueMinusAndAScreenReaderSaysPlusAndMinus() {
        show()

        compose.onNodeWithText("+3", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("−2", useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText("0", useUnmergedTree = true).assertIsDisplayed()
        button(3).assertIsDisplayed()
        button(-2).assertIsDisplayed()
    }
}
