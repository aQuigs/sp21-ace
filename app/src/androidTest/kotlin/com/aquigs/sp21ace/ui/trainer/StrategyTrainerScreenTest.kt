package com.aquigs.sp21ace.ui.trainer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.LayoutDirection
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
import com.aquigs.sp21ace.ui.assertFits
import com.aquigs.sp21ace.ui.assertFitsOnOneLine
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.textLayout
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrategyTrainerScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    // Hard 16 vs A is a hit when the dealer stands on soft 17
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val eightsVsSix = TrainerHand(cards("8h 8s"), card("6d"))
    private val softSeventeenVsKing = TrainerHand(cards("Ah 6d"), card("Kh"))

    // Hard 17 vs A is RH, so once there are 3 cards surrender is gone and the hit is right
    private val threeCardSeventeenVsAce = TrainerHand(cards("9c 4d 4h"), card("As"))

    // Hard 15 vs 6 is debated, stands, and hits with 6 or more cards or while a spaded 6-7-8 is possible. With nine cards it hits
    // by count, which adds the count of cards to the longest feedback there is.
    private val nineCardFifteenVsSix = TrainerHand(cards("Ac Ad Ah As 2c 2d 2h 2s 3c"), card("6d"))
    private val nineCardNames = listOf("Ace of clubs", "Ace of diamonds", "Ace of hearts", "Ace of spades", "2 of clubs", "2 of diamonds", "2 of hearts", "2 of spades", "3 of clubs")

    // Doubled hard 16 vs 10 is a rescue whatever the rules, doubled from hard 11 with a 5
    private val doubledSixteenVsKing = TrainerHand(cards("5c 6d 5h"), card("Ks"), doubled = true)

    // A button in every place, first-decision moves on a hand not yet doubled
    private val buttons = listOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SPLIT, Move.SURRENDER)

    private var settings by mutableStateOf(Settings())

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun button(move: Move) = compose.onNodeWithContentDescription(string(move.displayName))

    private fun bounds(description: String) = compose.onNodeWithContentDescription(description).getBoundsInRoot()

    private fun DpRect.overlaps(other: DpRect) = left < other.right && other.left < right && top < other.bottom && other.top < bottom

    // The unmerged tree still holds the text a control hides from a screen reader
    private fun textsInside(description: String, count: Int): List<SemanticsNode> = compose.onAllNodes(
        hasAnyAncestor(hasContentDescription(description)) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
        useUnmergedTree = true,
    ).fetchSemanticsNodes().also { assertEquals(description, count, it.size) }

    // Each answer button holds its label, and the chart tile a letter for each of its four colours
    private fun controlTexts() = buttons.map { string(it.displayName) to 1 } + (string(R.string.open_strategy_chart) to 4)

    private fun assertFullSize(description: String) = bounds(description).let {
        assertEquals(description, 64f, it.width.value, 0.5f)
        assertEquals(description, 64f, it.height.value, 0.5f)
    }

    // A screen reader hears the meter as one item, so its numbers and caption are only in the unmerged tree. The caption
    // centres under the track.
    private fun meterNumbersAreRightOfTheTrack(): Boolean {
        fun centre(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot().let { (it.left + it.right) / 2 }

        return centre("256") > centre(string(R.string.streak))
    }

    private fun showTrainer(
        modifier: Modifier = Modifier,
        first: TrainerHand = sixteenVsAce,
        deals: List<TrainerHand> = listOf(eightsVsSix),
        configuration: DeviceConfigurationOverride = DeviceConfigurationOverride { content -> content() },
        rules: RuleSet = RuleSet.S17,
    ) {
        val chart = StrategyCharts.forRules(rules)
        // Only these hands are left to deal, so grading one answer more would throw
        val next = deals.iterator()
        var trainer by mutableStateOf(TrainerState(first))

        compose.setContent {
            DeviceConfigurationOverride(configuration) {
                Sp21AceTheme {
                    StrategyTrainerScreen(
                        state = trainer,
                        settings = settings,
                        redoubling = rules.redoubling,
                        onAnswer = { asked, move -> trainer.answer(asked, move, chart) { next.next() }?.let { trainer = it.state } },
                        onOpenDrawer = {},
                        onOpenChart = {},
                        modifier = modifier,
                    )
                }
            }
        }
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
    fun aScreenReaderHearsEachVerdictFromTheSameNode() {
        showTrainer(deals = listOf(eightsVsSix, eightsVsSix))
        val verdict = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))

        button(Move.HIT).performClick()
        val right = verdict.fetchSemanticsNode()
        assertEquals(listOf("${string(R.string.right_answer)}. Hard 16 vs A. Hit"), right.config[SemanticsProperties.ContentDescription])
        // Standing on a pair of 8s against a 6 is wrong: they split
        button(Move.STAND).performClick()
        val wrong = verdict.fetchSemanticsNode()

        assertEquals(right.id, wrong.id)
        assertEquals(LiveRegionMode.Polite, wrong.config[SemanticsProperties.LiveRegion])
        assertTrue(wrong.config[SemanticsProperties.ContentDescription].single().startsWith("${string(R.string.wrong_answer)}. Pair of 8s vs 6."))
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

        val heights = buttons.map { button(it).getBoundsInRoot().height.value }

        heights.forEach { assertEquals(heights.first(), it, 1f) }
    }

    @Test
    fun theChartTileSitsAboveTheButtonsAndClearOfTheCards() {
        showTrainer()

        val tile = bounds(string(R.string.open_strategy_chart))

        assertTrue(tile.bottom <= button(Move.HIT).getBoundsInRoot().top)
        assertTrue(tile.left >= bounds("Ace of spades").right)
    }

    @Test
    fun rightAnswersClimbTheStreakAndAWrongOneDropsItToZero() {
        showTrainer(deals = listOf(eightsVsSix, sixteenVsAce, eightsVsSix))

        button(Move.HIT).performClick()
        button(Move.SPLIT).performClick()

        compose.onNodeWithContentDescription(string(R.string.streak_count, 2)).assertIsDisplayed()

        button(Move.STAND).performClick()

        compose.onNodeWithContentDescription(string(R.string.streak_count, 0)).assertIsDisplayed()
    }

    @Test
    fun theButtonLocationMovesTheButtonsAndTheTileToTheOtherSideOfTheCards() {
        showTrainer()

        assertTrue(button(Move.HIT).getBoundsInRoot().left >= bounds("Ace of spades").right)

        settings = Settings(buttonLocation = ButtonLocation.LEFT)

        val cardsLeft = bounds(string(R.string.face_down_card)).left
        assertTrue(button(Move.HIT).getBoundsInRoot().right <= cardsLeft)
        assertTrue(bounds(string(R.string.open_strategy_chart)).right <= cardsLeft)
    }

    @Test
    fun theStreakMetersNumbersFaceTheEdgeOfTheScreenOnEitherSide() {
        showTrainer()

        assertFalse(meterNumbersAreRightOfTheTrack())

        settings = Settings(buttonLocation = ButtonLocation.LEFT)

        assertTrue(meterNumbersAreRightOfTheTrack())
    }

    @Test
    fun inARightToLeftLanguageTheButtonsAndTheMetersNumbersKeepToTheSidesTheSettingNames() {
        settings = Settings(buttonLocation = ButtonLocation.LEFT)
        showTrainer(configuration = DeviceConfigurationOverride.LayoutDirection(LayoutDirection.Rtl))

        assertTrue(button(Move.HIT).getBoundsInRoot().right <= bounds(string(R.string.face_down_card)).left)
        assertTrue(meterNumbersAreRightOfTheTrack())

        settings = Settings(buttonLocation = ButtonLocation.RIGHT)

        assertTrue(button(Move.HIT).getBoundsInRoot().left >= bounds("Ace of spades").right)
        assertFalse(meterNumbersAreRightOfTheTrack())
    }

    @Test
    fun handTotalsShowOnTheLabelsLinesAtTheCardsRightEdgeAndHideAgain() {
        // A soft 17 reads as the chart's soft row, and a king as its ten column
        showTrainer(first = softSeventeenVsKing)

        compose.onNodeWithText("A-6").assertDoesNotExist()

        settings = Settings(handTotals = true)

        val you = compose.onNodeWithText(string(R.string.you)).getBoundsInRoot()
        val playerTotal = compose.onNodeWithText("A-6").assertIsDisplayed().getBoundsInRoot()
        assertEquals(bounds("6 of diamonds").right.value, playerTotal.right.value, 1f)
        assertTrue(playerTotal.top < you.bottom && you.top < playerTotal.bottom)
        compose.onNodeWithText("10").assertIsDisplayed()

        settings = Settings(handTotals = false)

        compose.onNodeWithText("A-6").assertDoesNotExist()
        compose.onNodeWithText("10").assertDoesNotExist()
    }

    @Test
    fun theChartTileAndTheStreakMeterHideOnTheirOwnAndTheButtonsKeepTheirPlace() {
        showTrainer()
        val surrender = button(Move.SURRENDER).getBoundsInRoot()
        val tile = compose.onNodeWithContentDescription(string(R.string.open_strategy_chart))
        val meter = compose.onNodeWithContentDescription(string(R.string.streak_count, 0))

        settings = Settings(chartButton = false)

        tile.assertDoesNotExist()
        meter.assertIsDisplayed()
        assertEquals(surrender, button(Move.SURRENDER).getBoundsInRoot())

        settings = Settings(streakMeter = false)

        tile.assertIsDisplayed()
        meter.assertDoesNotExist()
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeTheButtonsOnTheLeftCoverNothingAndTheTotalsKeepToOneLine() {
        // The dealer's two-digit total and the player's soft one are the widest totals there are
        settings = Settings(buttonLocation = ButtonLocation.LEFT, handTotals = true)
        showTrainer(Modifier.size(360.dp, 640.dp), first = softSeventeenVsKing, configuration = DeviceConfigurationOverride.FontScale(2f))

        val controls = buttons.map { button(it).getBoundsInRoot() } + bounds(string(R.string.open_strategy_chart))
        val texts = listOf(string(R.string.dealer), "10", string(R.string.you), "A-6").map { compose.onNodeWithText(it) }
        val others = listOf(string(R.string.face_down_card), "King of hearts", "Ace of hearts", "6 of diamonds", string(R.string.streak_count, 0))
            .map(::bounds) + texts.map { it.getBoundsInRoot() }

        for (control in controls) {
            for (other in others) assertFalse("$control overlaps $other", control.overlaps(other))
        }
        texts.forEach { it.fetchSemanticsNode().textLayout().assertFitsOnOneLine() }
    }

    // The 3 dp ring is drawn inside the circle in the label's colour, so a label has to end a little clear of it
    private fun assertLabelFitsInsideTheRing(move: Move, location: ButtonLocation) {
        val ringInset = with(compose.density) { 4.dp.toPx() }
        val name = string(move.displayName)
        val insideRing = compose.onNodeWithContentDescription(name).fetchSemanticsNode().size.width - 2 * ringInset
        val label = textsInside(name, 1).single().textLayout()

        label.assertFitsOnOneLine()
        assertTrue(
            "With the buttons $location, ${label.layoutInput.text} ends at ${label.getLineRight(0)}px, past the ring's inside at ${insideRing}px",
            label.getLineRight(0) <= insideRing,
        )
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeEveryButtonLabelAndTileLetterFitsInsideItsControlOnEitherSide() {
        showTrainer(Modifier.size(360.dp, 640.dp), configuration = DeviceConfigurationOverride.FontScale(2f))

        for (location in ButtonLocation.entries) {
            settings = Settings(buttonLocation = location)

            textsInside(string(R.string.open_strategy_chart), 4).forEach { it.textLayout().assertFitsOnOneLine() }
            for (move in buttons) assertLabelFitsInsideTheRing(move, location)
        }
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeRedoubleAndRescueFitInsideTheirRingsOnEitherSide() {
        showTrainer(Modifier.size(360.dp, 640.dp), first = doubledSixteenVsKing, configuration = DeviceConfigurationOverride.FontScale(2f), rules = RuleSet.H17_REDOUBLE)

        for (location in ButtonLocation.entries) {
            settings = Settings(buttonLocation = location)

            for (move in listOf(Move.REDOUBLE, Move.RESCUE)) assertLabelFitsInsideTheRing(move, location)
        }
    }

    @Test
    fun atFullSizeEachLabelOfADoubledHandStops7dpShortOfTheCurveOfItsRingAsDoubleDoes() {
        showTrainer(Modifier.size(411.dp, 880.dp), first = doubledSixteenVsKing, rules = RuleSet.H17_REDOUBLE)

        for (move in listOf(Move.HIT, Move.STAND, Move.REDOUBLE, Move.SPLIT, Move.RESCUE)) {
            val name = string(move.displayName)
            assertFullSize(name)
            val circle = bounds(name)
            val label = textsInside(name, 1).single()
            val line = label.textLayout()
            val (left, right) = with(compose.density) { (label.boundsInRoot.left + line.getLineLeft(0)).toDp() to (label.boundsInRoot.left + line.getLineRight(0)).toDp() }

            assertTrue("$name starts ${left - circle.left} in", left - circle.left >= 6.5.dp)
            assertTrue("$name ends ${circle.right - right} in", circle.right - right >= 6.5.dp)
        }
    }

    @Test
    fun atTheDefaultFontSizeTheButtonsAndTheTileAreFullSizeWithTheirLabelsAtTheirLargestAndAtDoubleItTheyLookTheSame() {
        var fontScale by mutableFloatStateOf(1f)
        // Tall enough that five full-size buttons and the tile fit above a recap grown by the double font. The scale is read in
        // composition, so a new scale lays the same screen out again.
        showTrainer(
            Modifier.size(411.dp, 880.dp),
            configuration = DeviceConfigurationOverride { content -> DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(fontScale), content) },
        )

        fun drawn() = controlTexts().associate { (description, count) ->
            assertFullSize(description)
            description to textsInside(description, count).map { it.textLayout().apply { assertFitsOnOneLine() } }
        }
        fun sizes(texts: Map<String, List<TextLayoutResult>>) =
            texts.values.flatten().map { "${it.layoutInput.text} needs ${it.multiParagraph.intrinsics.maxIntrinsicWidth}x${it.multiParagraph.height}px in ${it.size}" }
        val atDefault = drawn()

        // The tree reports a label's style rather than the size autoSize drew it at, so the drawn size shows in how wide its line is
        for (move in buttons) {
            val label = atDefault.getValue(string(move.displayName)).single()
            val input = label.layoutInput
            val atLargest = TextMeasurer(input.fontFamilyResolver, compose.density, input.layoutDirection).measure(input.text, input.style.copy(fontSize = 13.sp), maxLines = 1)
            assertEquals("${input.text}", atLargest.multiParagraph.intrinsics.maxIntrinsicWidth, label.multiParagraph.intrinsics.maxIntrinsicWidth, 0.01f)
        }

        fontScale = 2f

        assertEquals(sizes(atDefault), sizes(drawn()))
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeTheLongestFeedbackFitsTheBar() {
        showTrainer(Modifier.size(360.dp, 640.dp), first = nineCardFifteenVsSix, configuration = DeviceConfigurationOverride.FontScale(2f))

        button(Move.STAND).performClick()

        val feedback = compose.onNodeWithText("9-card hard 15 vs 6 | Hit with 6 or more cards. Otherwise stand, but hit while a spaded 6-7-8 is possible † (debated)")
        feedback.fetchSemanticsNode().textLayout().assertFits()
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeNineCardsShrinkToFitApartFromTheButtonsOnEitherSideWithEachIndexShowing() {
        showTrainer(Modifier.size(360.dp, 640.dp), first = nineCardFifteenVsSix, configuration = DeviceConfigurationOverride.FontScale(2f))

        for (location in ButtonLocation.entries) {
            settings = Settings(buttonLocation = location, handTotals = true)
            val controls = buttons.map { button(it).getBoundsInRoot() } + bounds(string(R.string.open_strategy_chart))
            val cards = nineCardNames.map(::bounds)

            for (control in controls) {
                // The cards stop 8dp short of their space either side, less a hair for bounds read back in dp from whole pixels
                val nearby = DpRect(control.left - 7.9.dp, control.top, control.right + 7.9.dp, control.bottom)
                for (card in cards) assertFalse("$location: $card is within 8dp of $control", nearby.overlaps(card))
            }
            // Dealt left to right, each over most of the one before, so every card's corner index stays uncovered
            assertEquals(cards.sortedBy { it.left }, cards)
            cards.zipWithNext { covered, over -> assertTrue("$over hides $covered's index", over.left - covered.left >= covered.width * 0.19f) }
        }
    }

    @Test
    fun onAHandOf3OrMoreCardsSplitAndSurrenderKeepTheirPlacesButTakeNoAnswer() {
        showTrainer(first = threeCardSeventeenVsAce)
        val places = buttons.map { button(it).getBoundsInRoot() }

        button(Move.SPLIT).assertIsNotEnabled()
        button(Move.SURRENDER).assertIsNotEnabled().performClick()

        compose.onNodeWithText(string(R.string.previous_hand)).assertDoesNotExist()

        button(Move.HIT).assertIsEnabled().performClick()

        compose.onNodeWithText("3-card hard 17 vs A | Hit with 3 or more cards. Otherwise surrender").assertIsDisplayed()
        // The next hand has two cards, so every button takes an answer again, each where it was
        buttons.forEach { button(it).assertIsEnabled() }
        assertEquals(places, buttons.map { button(it).getBoundsInRoot() })
    }

    @Test
    fun onADoubledHandRedoubleAndRescueTakeTheDoubleAndSurrenderPlacesWhileHitAndSplitTakeNoAnswer() {
        showTrainer(first = doubledSixteenVsKing, deals = listOf(sixteenVsAce), rules = RuleSet.H17_REDOUBLE)
        val places = listOf(Move.HIT, Move.STAND, Move.REDOUBLE, Move.SPLIT, Move.RESCUE).map { button(it).getBoundsInRoot() }

        button(Move.DOUBLE).assertDoesNotExist()
        button(Move.SURRENDER).assertDoesNotExist()
        button(Move.HIT).assertIsNotEnabled()
        button(Move.SPLIT).assertIsNotEnabled()
        button(Move.REDOUBLE).assertIsEnabled()
        button(Move.RESCUE).assertIsEnabled().performClick()

        compose.onNodeWithContentDescription("${string(R.string.right_answer)}. Doubled hard 16 vs 10. Rescue").assertIsDisplayed()
        compose.onNode(hasText(string(R.string.action)) and hasText(string(R.string.move_rescue))).assertIsDisplayed()
        // The next hand isn't doubled, so a double and a surrender are back where a redouble and a rescue were
        assertEquals(places, buttons.map { button(it).getBoundsInRoot() })
    }

    @Test
    fun aDoublesCardLiesSidewaysAcrossTheMiddleOfTheHandAfterTheCardsBeforeItLeavingEachIndexShowing() {
        // Hit from hard 7 with a 2, then doubled with a 5
        showTrainer(first = TrainerHand(cards("4c 3d 2h 5s"), card("6s"), doubled = true), rules = RuleSet.H17_REDOUBLE)
        val (four, three, two) = listOf("4 of clubs", "3 of diamonds", "2 of hearts").map(::bounds)
        val five = bounds("5 of spades")

        for (upright in listOf(four, three, two)) assertTrue("$upright", upright.height > upright.width)
        assertEquals("$five", two.height.value, five.width.value, 1f)
        assertEquals("$five", two.width.value, five.height.value, 1f)
        assertEquals("$five", (two.top + two.bottom).value / 2, (five.top + five.bottom).value / 2, 1f)
        for ((under, over) in listOf(four to three, three to two, two to five)) assertTrue("$over", over.left - under.left >= under.width * 0.19f)
    }

    @Test
    fun withoutRedoublingADoubledHandCanOnlyStandOrRescue() {
        showTrainer(first = doubledSixteenVsKing)

        button(Move.REDOUBLE).assertIsNotEnabled().performClick()

        compose.onNodeWithText(string(R.string.previous_hand)).assertDoesNotExist()

        button(Move.STAND).performClick()

        compose.onNodeWithText("Doubled hard 16 vs 10 | Rescue").assertIsDisplayed()
        compose.onNode(hasText(string(R.string.strategy)) and hasText(string(R.string.move_rescue))).assertIsDisplayed()
    }

    @Test
    fun whenTheCardCountMakesTheHandAHitTheFeedbackLeadsWithIt() {
        // Hard 14 vs 4 is S4*, so with 4 cards it hits
        showTrainer(first = TrainerHand(cards("2c 3d 4h 5s"), card("4c")))

        button(Move.STAND).performClick()

        val words = "Hit with 4 or more cards. Otherwise stand, but hit while any 6-7-8 is possible"
        compose.onNodeWithContentDescription("${string(R.string.wrong_answer)}. 4-card hard 14 vs 4. $words").assertIsDisplayed()
        compose.onNode(hasText(string(R.string.strategy)) and hasText(string(R.string.move_hit))).assertIsDisplayed()
    }

    @Test
    fun onANarrowPhoneAtTheLargestFontSizeASurrenderFitsItsRecapTile() {
        showTrainer(Modifier.size(360.dp, 640.dp), configuration = DeviceConfigurationOverride.FontScale(2f))

        button(Move.SURRENDER).performClick()

        compose.onNodeWithText(string(R.string.move_surrender), useUnmergedTree = true).fetchSemanticsNode().textLayout().assertFitsOnOneLine()
    }
}
