package com.aquigs.sp21ace.ui.trainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.dealerTotal
import com.aquigs.sp21ace.domain.trainer.playerTotal
import com.aquigs.sp21ace.ui.chart.ChartTile
import com.aquigs.sp21ace.ui.components.DealerHand
import com.aquigs.sp21ace.ui.components.Dissolve
import com.aquigs.sp21ace.ui.components.DissolvingHand
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale
import com.aquigs.sp21ace.ui.components.appBarColors
import com.aquigs.sp21ace.ui.components.autoSizeDownTo
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import com.aquigs.sp21ace.ui.theme.disabledContent

private val ButtonSize = 64.dp

// Blackjack Ace's verdict changes in about a quarter of a second, quicker than its cards dissolve
private const val FEEDBACK_MILLIS = 250

/**
 * The cards, with the answer buttons down one edge and, as [settings] choose, the hand totals, the chart tile and the streak
 * meter. [redoubling] is whether the table rules let a doubled hand redouble.
 */
@Composable
fun StrategyTrainerScreen(
    state: TrainerState,
    settings: Settings,
    redoubling: Boolean,
    onAnswer: (asked: TrainerHand, move: Move) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonsOnLeft = settings.buttonLocation == ButtonLocation.LEFT

    Scaffold(
        modifier = modifier,
        topBar = { FeedbackBar(state.lastGrade, onOpenDrawer) },
        bottomBar = { PreviousHandPanel(state.lastGrade) },
    ) { padding ->
        val meter: @Composable RowScope.() -> Unit = {
            // As in Blackjack Ace, the numbers face the screen's edge
            if (settings.streakMeter) StreakMeter(state.streak, modifier = Modifier.fillMaxHeight(), numbersOnRight = buttonsOnLeft)
        }
        val meterGap: @Composable RowScope.() -> Unit = {
            if (settings.streakMeter) Spacer(Modifier.width(8.dp))
        }
        val cards: @Composable RowScope.() -> Unit = {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().wrapContentWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HandArea(
                    label = stringResource(R.string.dealer),
                    total = state.hand.dealerTotal.takeIf { settings.handTotals },
                    modifier = Modifier.weight(1f),
                ) { fan ->
                    DealerHand(state.hand.upcard, fan)
                }
                HandArea(
                    label = stringResource(R.string.you),
                    total = state.hand.playerTotal.takeIf { settings.handTotals },
                    modifier = Modifier.weight(1f),
                ) { fan ->
                    DissolvingHand(state.hand.player, fan, sideways = state.hand.doubled)
                }
            }
        }
        val controls: @Composable RowScope.() -> Unit = {
            Controls(
                showChartTile = settings.chartButton,
                alignment = if (buttonsOnLeft) AbsoluteAlignment.Left else AbsoluteAlignment.Right,
                buttons = if (state.hand.doubled) AFTER_DOUBLING_BUTTONS else BUTTONS,
                moves = state.hand.moves(redoubling),
                onOpenChart = onOpenChart,
                // The hand this frame shows, even if a tap lands after the next one is dealt but before it is drawn
                onAnswer = { move -> onAnswer(state.hand, move) },
            )
        }
        // Left to right. The meter trades edges with the buttons, so the buttons stay at the screen's edge under the thumb.
        val parts = listOf(meter, meterGap, cards, controls).let { if (buttonsOnLeft) it.reversed() else it }

        // Left and Right name sides of the screen, so a right-to-left language mustn't reverse the row
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalArrangement = Arrangement.Absolute.Left) {
            parts.forEach { part -> part() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackBar(lastGrade: Grade?, onOpenDrawer: () -> Unit) {
    val colors = Sp21AceTheme.colors
    // The app bar fades to a new colour on its own, about as quickly as Blackjack Ace's
    val container = when (lastGrade?.isCorrect) {
        null -> colors.appBar
        true -> colors.correct
        false -> colors.wrong
    }

    // Right and wrong otherwise differ only in colour and mark, which a screen reader can't announce
    val spoken = lastGrade?.let {
        "${stringResource(if (it.isCorrect) R.string.right_answer else R.string.wrong_answer)}. ${it.hand.matchup}. ${it.inPlainWords()}"
    }

    TopAppBar(
        title = {
            Dissolve(
                lastGrade,
                // Each verdict dissolves in on its own, so a screen reader hears every one from the node the dissolve keeps
                Modifier.semantics(mergeDescendants = true) {
                    if (spoken == null) {
                        heading()
                    } else {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = spoken
                    }
                },
                durationMillis = FEEDBACK_MILLIS,
            ) { grade ->
                if (grade == null) Text(stringResource(R.string.strategy_trainer)) else FeedbackText(grade)
            }
        },
        navigationIcon = {
            Dissolve(lastGrade?.isCorrect, durationMillis = FEEDBACK_MILLIS) { correct ->
                // The title speaks the verdict, so the mark stays silent
                if (correct != null) {
                    Icon(
                        painterResource(if (correct) R.drawable.ic_check_circle else R.drawable.ic_cancel),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 4.dp, end = 8.dp).size(52.dp),
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenDrawer) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.open_menu))
            }
        },
        colors = appBarColors(container),
    )
}

private fun Grade.inPlainWords() = play.inPlainWords(correctMove, cards = hand.player.size, afterDoubling = hand.doubled)

@Composable
private fun FeedbackText(grade: Grade) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(grade.hand.matchup) }
            append(" | ")
            append(grade.inPlainWords())
        },
        // The longest squares need three lines, and an em line height keeps them inside the bar as autoSize shrinks the text
        autoSize = autoSizeDownTo(minSize = 10.dp, maxFontSize = 16.sp),
        maxLines = 3,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.2.em),
    )
}

/**
 * A hand's label over its [cards], with its [total], when given, on the label's line at the cards' right edge, as in Blackjack Ace.
 * The cards take the modifier they are given, which fits them to the space.
 */
@Composable
private fun HandArea(label: String, total: String?, modifier: Modifier = Modifier, cards: @Composable (Modifier) -> Unit) {
    val color = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier) {
        // A hand of 3 or more cards can fill the space, which sits centred, so its cards stop 8dp short either side and clear of the
        // buttons. The label and the total keep the whole width, which a narrow phone at a large font size needs for one line.
        val cardsMaxWidth = (maxWidth - 16.dp).coerceAtLeast(0.dp)

        // As wide as the cards, unless the label and the total need more, so the total ends where the cards do
        Column(modifier = Modifier.width(IntrinsicSize.Max), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = label,
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                total?.let {
                    // A total broken over two lines would read as two numbers
                    Text(
                        text = it,
                        modifier = Modifier.alignByBaseline(),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        softWrap = false,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            cards(Modifier.weight(1f, fill = false).widthIn(max = cardsMaxWidth))
        }
    }
}

/** The chart tile over the answer buttons. As wide as a button, the tile takes no room from the cards. */
@Composable
private fun Controls(
    showChartTile: Boolean,
    alignment: Alignment.Horizontal,
    buttons: List<Move>,
    moves: Set<Move>,
    onOpenChart: () -> Unit,
    onAnswer: (Move) -> Unit,
) {
    Column(modifier = Modifier.fillMaxHeight(), horizontalAlignment = alignment) {
        if (showChartTile) ChartTile(onClick = onOpenChart, modifier = Modifier.size(ButtonSize))
        // Holds the buttons at the bottom whether or not the tile shows
        Spacer(Modifier.weight(1f))
        AnswerButtons(buttons = buttons, moves = moves, onAnswer = onAnswer, modifier = Modifier.padding(top = 8.dp))
    }
}

private val BUTTONS = listOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SPLIT, Move.SURRENDER)

// A redouble and a rescue take the places of a double and a surrender, which a doubled hand can't make
private val AFTER_DOUBLING_BUTTONS = listOf(Move.HIT, Move.STAND, Move.REDOUBLE, Move.SPLIT, Move.RESCUE)

/** A button in every place, the moves the hand doesn't allow greyed out in theirs, so the rest never move under the thumb. */
@Composable
private fun AnswerButtons(buttons: List<Move>, moves: Set<Move>, onAnswer: (Move) -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    // An outlined button greys out its label but not its border
    val disabledColor = MaterialTheme.colorScheme.disabledContent

    ProvideDefaultFontScale {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            buttons.forEach { move ->
                val name = stringResource(move.displayName)
                val enabled = move in moves

                OutlinedButton(
                    onClick = { onAnswer(move) },
                    // Shrink evenly on a screen too short for five, such as a small phone at a large font size, rather than squeezing out the last
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .sizeIn(maxWidth = ButtonSize, maxHeight = ButtonSize)
                        .aspectRatio(1f)
                        .semantics { contentDescription = name },
                    enabled = enabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
                    border = BorderStroke(3.dp, if (enabled) color else disabledColor),
                    // The ring is drawn inside the circle in the label's colour, so the label is fitted inside the ring rather than across it
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    // The capitals and SURR. are for the eye; a screen reader says the move's name
                    Text(
                        text = if (move == Move.SURRENDER) stringResource(R.string.surrender_short) else name.uppercase(),
                        // A label as wide as a full-size circle allows touches the curve of its ring with its end letters, so it stops
                        // 7dp short, as DOUBLE does at its largest. The smallest circles have no room to spare for that.
                        modifier = Modifier.widthIn(max = ButtonSize - 14.dp),
                        fontWeight = FontWeight.Bold,
                        // DOUBLE already needs 8 sp inside the ring of a small phone's circles at a large font size, so a shorter screen, or REDOUBLE, needs less
                        autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 13.sp),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
