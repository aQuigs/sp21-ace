package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.Flashcard
import com.aquigs.sp21ace.domain.indices.FlashcardGrade
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.atCount
import com.aquigs.sp21ace.domain.indices.flashcardAnswers
import com.aquigs.sp21ace.domain.indices.inPlainWords
import com.aquigs.sp21ace.domain.indices.playName
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.totalLabel
import com.aquigs.sp21ace.ui.components.CircleButton
import com.aquigs.sp21ace.ui.components.DealerHand
import com.aquigs.sp21ace.ui.components.Dissolve
import com.aquigs.sp21ace.ui.components.DissolvingHand
import com.aquigs.sp21ace.ui.components.FeedbackBar
import com.aquigs.sp21ace.ui.components.FeedbackText
import com.aquigs.sp21ace.ui.components.HandArea
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale

private val PadGap = 6.dp
private val MinButton = 40.dp
private val MaxButton = 56.dp

/**
 * A flashcard of an index: the cards, the play it asks about, and a pad of true counts to answer with. Leaving the page or the
 * app, or the drawer [covered] over it, interrupts the card showing, so its answer records no time.
 */
@Composable
fun IndexFlashcardsScreen(
    state: IndexFlashcardsState,
    settings: Settings,
    covered: Boolean,
    actions: IndexFlashcardsActions,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = state.run.card
    val currentActions by rememberUpdatedState(actions)

    LifecycleResumeEffect(Unit) { onPauseOrDispose { currentActions.interrupted() } }
    LaunchedEffect(covered) { if (covered) currentActions.interrupted() }
    LaunchedEffect(card, covered) { if (card != null && !covered) currentActions.shown(card) }

    Scaffold(modifier = modifier, topBar = { FlashcardBar(state.run.lastGrade, onOpenDrawer) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (card == null) {
                Text(
                    text = stringResource(R.string.no_indices_switched_on),
                    modifier = Modifier.weight(1f).wrapContentHeight(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Hands(card, settings.handTotals, Modifier.weight(1f))
                Prompt(card.index.deviation.playName, card.index.direction)
                // The card this frame shows, even if a tap lands after the next one is dealt but before it is drawn
                NumberPad(state.presets.active.flashcardAnswers) { number -> actions.answer(card, number) }
            }
        }
    }
}

@Composable
private fun FlashcardBar(lastGrade: FlashcardGrade?, onOpenDrawer: () -> Unit) {
    val answered = lastGrade?.takeUnless { it.isCorrect }?.let { stringResource(R.string.you_answered, signedCount(it.answer)) }

    FeedbackBar(
        verdict = lastGrade,
        title = stringResource(R.string.index_flashcards),
        isCorrect = { it.isCorrect },
        spoken = lastGrade?.let { grade ->
            val deviation = grade.card.index.deviation
            listOfNotNull(deviation.title.takeIf { deviation is Deviation.OnHand }, grade.card.index.inPlainWords, answered).joinToString(". ")
        },
        onOpenDrawer = onOpenDrawer,
    ) { grade ->
        val index = grade.card.index
        val wrong = if (grade.isCorrect) null else stringResource(R.string.you_answered, signedCount(grade.answer))

        FeedbackText(
            buildAnnotatedString {
                // Insurance names itself as the play, so it isn't named twice
                if (index.deviation is Deviation.OnHand) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(index.deviation.title.unbroken()) }
                    append("$SEPARATOR${index.inPlainWords.unbroken()}")
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(index.deviation.playName) }
                    append("\u00A0${index.atCount.unbroken()}")
                }
                wrong?.let { append("$SEPARATOR${it.unbroken()}") }
            },
        )
    }
}

// The bar breaks its lines only after a part's bar, so a part such as "at −1 or higher" never ends one line and starts the next
private const val SEPARATOR = "\u00A0| "

private fun String.unbroken() = replace(' ', '\u00A0')

/** The dealer's hand over the player's, or for insurance the dealer's alone, with the player's space left empty so the cards don't move. */
@Composable
private fun Hands(card: Flashcard, handTotals: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().wrapContentWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        HandArea(
            label = stringResource(R.string.dealer),
            total = card.upcard.upcard.label.takeIf { handTotals },
            modifier = Modifier.weight(1f),
        ) { fan ->
            DealerHand(card.upcard, fan)
        }
        if (card.player.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else {
            HandArea(
                label = stringResource(R.string.you),
                total = totalLabel(card.player).takeIf { handTotals },
                modifier = Modifier.weight(1f),
            ) { fan ->
                DissolvingHand(card.player, fan)
            }
        }
    }
}

/** "STAND at ? or higher", the play in capitals. */
@Composable
private fun Prompt(play: String, direction: IndexDirection) {
    val spoken = stringResource(R.string.flashcard_prompt_spoken, play, direction.inPlainWords)

    // A screen reader would say the question mark, so it hears a question instead
    Dissolve(play to direction, Modifier.semantics(mergeDescendants = true) { contentDescription = spoken }) { (shownPlay, shownDirection) ->
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(shownPlay.uppercase()) }
                append(" at ? ${shownDirection.inPlainWords}")
            },
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/**
 * A button for every number in [answers], the negatives first, then zero and the positives, each group in rows as even as the
 * width allows, so the buttons stay at least [MinButton] across. They depend only on [answers], so they never move between cards.
 */
@Composable
private fun NumberPad(answers: IntRange, onAnswer: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rows = padRows(answers, perRow = ((maxWidth + PadGap) / (MinButton + PadGap)).toInt().coerceAtLeast(1))
        val columns = rows.maxOf { it.size }
        val size = minOf(MaxButton, (maxWidth - PadGap * (columns - 1)) / columns)

        // The labels keep their size in a button that doesn't grow, as the trainer's do
        ProvideDefaultFontScale {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(PadGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(PadGap)) {
                        row.forEach { number -> NumberButton(number, size, onClick = { onAnswer(number) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberButton(number: Int, size: Dp, onClick: () -> Unit) {
    val name = when {
        number > 0 -> stringResource(R.string.count_plus, number)
        number < 0 -> stringResource(R.string.count_minus, -number)
        else -> "0"
    }

    CircleButton(name = name, onClick = onClick, modifier = Modifier.size(size), label = signedCount(number), maxFontSize = 20.sp)
}

/** [answers]' negatives, then zero and its positives, each split into rows of at most [perRow] that differ in length by at most one. */
internal fun padRows(answers: IntRange, perRow: Int): List<List<Int>> = listOf(answers.first..-1, 0..answers.last).flatMap { group ->
    val numbers = group.toList()
    val rows = (numbers.size + perRow - 1) / perRow

    // The longer rows first, each one longer than the rest by the remainder's share
    var from = 0
    List(rows) { row ->
        val length = numbers.size / rows + if (row < numbers.size % rows) 1 else 0
        numbers.subList(from, from + length).also { from += length }
    }
}
