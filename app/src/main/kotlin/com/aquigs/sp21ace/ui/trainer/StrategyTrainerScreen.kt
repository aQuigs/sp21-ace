package com.aquigs.sp21ace.ui.trainer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.ui.components.DrillTable
import com.aquigs.sp21ace.ui.components.EdgeControls
import com.aquigs.sp21ace.ui.components.FeedbackBar
import com.aquigs.sp21ace.ui.components.FeedbackText
import com.aquigs.sp21ace.ui.components.HandsColumn
import com.aquigs.sp21ace.ui.components.MoveButton

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
    Scaffold(
        modifier = modifier,
        topBar = { GradeBar(state.lastGrade, onOpenDrawer) },
        bottomBar = { PreviousHandPanel(state.lastGrade) },
    ) { padding ->
        DrillTable(
            streak = state.streak,
            settings = settings,
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            controls = {
                EdgeControls(settings.buttonsOnLeft, settings.chartButton, onOpenChart) {
                    AnswerButtons(
                        buttons = if (state.hand.doubled) AFTER_DOUBLING_BUTTONS else BUTTONS,
                        moves = state.hand.moves(redoubling),
                        // The hand this frame shows, even if a tap lands after the next one is dealt but before it is drawn
                        onAnswer = { move -> onAnswer(state.hand, move) },
                    )
                }
            },
        ) {
            HandsColumn(state.hand.upcard, state.hand.player, settings.handTotals, state.hand.doubled)
        }
    }
}

@Composable
private fun GradeBar(lastGrade: Grade?, onOpenDrawer: () -> Unit) {
    FeedbackBar(
        verdict = lastGrade,
        title = stringResource(R.string.strategy_trainer),
        isCorrect = { it.isCorrect },
        spoken = lastGrade?.let { "${it.hand.matchup}. ${it.inPlainWords()}" },
        onOpenDrawer = onOpenDrawer,
    ) { grade ->
        FeedbackText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(grade.hand.matchup) }
                append(" | ")
                append(grade.inPlainWords())
            },
        )
    }
}

private fun Grade.inPlainWords() = play.inPlainWords(correctMove, cards = hand.player.size, afterDoubling = hand.doubled)

private val BUTTONS = listOf(Move.HIT, Move.STAND, Move.DOUBLE, Move.SPLIT, Move.SURRENDER)

// A redouble and a rescue take the places of a double and a surrender, which a doubled hand can't make
private val AFTER_DOUBLING_BUTTONS = listOf(Move.HIT, Move.STAND, Move.REDOUBLE, Move.SPLIT, Move.RESCUE)

/** A button in every place, the moves the hand doesn't allow greyed out in theirs, so the rest never move under the thumb. */
@Composable
private fun ColumnScope.AnswerButtons(buttons: List<Move>, moves: Set<Move>, onAnswer: (Move) -> Unit) {
    buttons.forEach { move ->
        MoveButton(
            move,
            onClick = { onAnswer(move) },
            // Shrink evenly on a screen too short for five, such as a small phone at a large font size, rather than squeezing out the last
            modifier = Modifier.weight(1f, fill = false),
            enabled = move in moves,
        )
    }
}
