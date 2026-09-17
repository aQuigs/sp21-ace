package com.aquigs.sp21ace.ui.trainer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.aquigs.sp21ace.domain.trainer.Trainer
import com.aquigs.sp21ace.ui.components.CardBack
import com.aquigs.sp21ace.ui.components.OverlappingCards
import com.aquigs.sp21ace.ui.components.PlayingCard
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

@Composable
fun StrategyTrainerScreen(trainer: Trainer, onOpenDrawer: () -> Unit, modifier: Modifier = Modifier) {
    var hand by remember(trainer) { mutableStateOf(trainer.hand) }
    var lastGrade by remember(trainer) { mutableStateOf<Grade?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { FeedbackBar(lastGrade, onOpenDrawer) },
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().wrapContentWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HandArea(label = stringResource(R.string.dealer), modifier = Modifier.weight(1f)) {
                    CardBack()
                    PlayingCard(hand.upcard)
                }
                HandArea(label = stringResource(R.string.you), modifier = Modifier.weight(1f)) {
                    hand.player.forEach { PlayingCard(it) }
                }
            }

            AnswerButtons(
                onAnswer = { move ->
                    lastGrade = trainer.answer(move)
                    hand = trainer.hand
                },
                modifier = Modifier.align(Alignment.Bottom),
            )
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
    val content = colors.onAppBar

    TopAppBar(
        title = {
            if (lastGrade == null) {
                Text(text = stringResource(R.string.strategy_trainer), modifier = Modifier.semantics { heading() })
            } else {
                FeedbackText(lastGrade)
            }
        },
        navigationIcon = { lastGrade?.let { AnswerMark(it.isCorrect, circle = content, mark = container) } },
        actions = {
            IconButton(onClick = onOpenDrawer) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.open_menu))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = container,
            titleContentColor = content,
            actionIconContentColor = content,
        ),
    )
}

@Composable
private fun FeedbackText(grade: Grade) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(grade.hand.matchup) }
            append(" | ")
            append(grade.play.inPlainWords())
        },
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        // The longest squares need three lines, and an em line height keeps them inside the bar as autoSize shrinks the text
        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 16.sp),
        maxLines = 3,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 1.2.em),
    )
}

@Composable
private fun AnswerMark(correct: Boolean, circle: Color, mark: Color) {
    Canvas(
        modifier = Modifier.padding(start = 8.dp, end = 12.dp).size(44.dp),
        contentDescription = stringResource(if (correct) R.string.right_answer else R.string.wrong_answer),
    ) {
        drawCircle(circle)

        val path = Path().apply {
            if (correct) {
                moveTo(size.width * 0.27f, size.height * 0.52f)
                lineTo(size.width * 0.43f, size.height * 0.68f)
                lineTo(size.width * 0.74f, size.height * 0.36f)
            } else {
                moveTo(size.width * 0.33f, size.height * 0.33f)
                lineTo(size.width * 0.67f, size.height * 0.67f)
                moveTo(size.width * 0.67f, size.height * 0.33f)
                lineTo(size.width * 0.33f, size.height * 0.67f)
            }
        }
        drawPath(path, mark, style = Stroke(width = size.width * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
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
    val surrender = stringResource(R.string.surrender)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Move.entries.forEach { move ->
            OutlinedButton(
                onClick = { onAnswer(move) },
                modifier = Modifier.size(64.dp).semantics { if (move == Move.SURRENDER) contentDescription = surrender },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
                border = BorderStroke(3.dp, color),
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = stringResource(move.label),
                    fontWeight = FontWeight.Bold,
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

private val Move.label: Int
    get() = when (this) {
        Move.HIT -> R.string.hit
        Move.STAND -> R.string.stand
        Move.DOUBLE -> R.string.double_down
        Move.SPLIT -> R.string.split
        Move.SURRENDER -> R.string.surrender_short
    }
