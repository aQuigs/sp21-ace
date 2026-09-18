package com.aquigs.sp21ace.ui.trainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.inPlainWords
import com.aquigs.sp21ace.domain.trainer.Grade
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.ui.chart.ChartTile
import com.aquigs.sp21ace.ui.components.CardBack
import com.aquigs.sp21ace.ui.components.OverlappingCards
import com.aquigs.sp21ace.ui.components.PlayingCard
import com.aquigs.sp21ace.ui.components.appBarColors
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

private val ButtonSize = 64.dp

@Composable
fun StrategyTrainerScreen(
    state: TrainerState,
    onAnswer: (asked: TrainerHand, move: Move) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { FeedbackBar(state.lastGrade, onOpenDrawer) },
        bottomBar = { PreviousHandPanel(state.lastGrade) },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            StreakMeter(state.streak, modifier = Modifier.fillMaxHeight().padding(end = 8.dp))

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().wrapContentWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HandArea(label = stringResource(R.string.dealer), modifier = Modifier.weight(1f)) {
                    CardBack()
                    PlayingCard(state.hand.upcard)
                }
                HandArea(label = stringResource(R.string.you), modifier = Modifier.weight(1f)) {
                    state.hand.player.forEach { PlayingCard(it) }
                }
            }

            // As wide as a button, the tile takes no room from the cards
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                ChartTile(onClick = onOpenChart, modifier = Modifier.size(ButtonSize))
                AnswerButtons(
                    // The hand this frame shows, even if a tap lands after the next one is dealt but before it is drawn
                    onAnswer = { move -> onAnswer(state.hand, move) },
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackBar(lastGrade: Grade?, onOpenDrawer: () -> Unit) {
    val colors = Sp21AceTheme.colors
    val container = when (lastGrade?.isCorrect) {
        null -> colors.appBar
        true -> colors.correct
        false -> colors.wrong
    }

    TopAppBar(
        title = {
            if (lastGrade == null) {
                Text(text = stringResource(R.string.strategy_trainer), modifier = Modifier.semantics { heading() })
            } else {
                FeedbackText(lastGrade)
            }
        },
        navigationIcon = {
            lastGrade?.let {
                // FeedbackText speaks the verdict, so the mark stays silent
                Icon(
                    painterResource(if (it.isCorrect) R.drawable.ic_check_circle else R.drawable.ic_cancel),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp, end = 8.dp).size(52.dp),
                )
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

@Composable
private fun FeedbackText(grade: Grade) {
    val verdict = stringResource(if (grade.isCorrect) R.string.right_answer else R.string.wrong_answer)
    val words = grade.play.inPlainWords(grade.correctMove)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(grade.hand.matchup) }
            append(" | ")
            append(words)
        },
        // Right and wrong otherwise differ only in colour and mark, which a screen reader can't announce
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = "$verdict. ${grade.hand.matchup}. $words"
        },
        // The longest squares need three lines, and an em line height keeps them inside the bar as autoSize shrinks the text
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp),
        maxLines = 3,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.2.em),
    )
}

@Composable
private fun HandArea(label: String, modifier: Modifier = Modifier, cards: @Composable () -> Unit) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
        )
        OverlappingCards(modifier = Modifier.weight(1f, fill = false), content = cards)
    }
}

@Composable
private fun AnswerButtons(onAnswer: (Move) -> Unit, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Move.entries.forEach { move ->
            val name = stringResource(move.displayName)

            OutlinedButton(
                onClick = { onAnswer(move) },
                // Shrink evenly on a screen too short for five, such as a small phone at a large font size, rather than squeezing out the last
                modifier = Modifier
                    .weight(1f, fill = false)
                    .sizeIn(maxWidth = ButtonSize, maxHeight = ButtonSize)
                    .aspectRatio(1f)
                    .semantics { contentDescription = name },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
                border = BorderStroke(3.dp, color),
                contentPadding = PaddingValues(0.dp),
            ) {
                // The capitals and SURR. are for the eye; a screen reader says the move's name
                Text(
                    text = if (move == Move.SURRENDER) stringResource(R.string.surrender_short) else name.uppercase(),
                    fontWeight = FontWeight.Bold,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
