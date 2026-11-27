package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.FlashcardGrade
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.NoIndex
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.indices.flashcardPad
import com.aquigs.sp21ace.domain.indices.inPlainWords
import com.aquigs.sp21ace.domain.indices.playName
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.ui.components.Dissolve
import com.aquigs.sp21ace.ui.components.HandsColumn
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale
import com.aquigs.sp21ace.ui.components.RingButton

/**
 * A flashcard of an index: the cards, the play it asks about and a pad of true counts to answer with, or with no index switched
 * on, a way to the Indices page. [covered] is the drawer over it, as [FlashcardPage] says.
 */
@Composable
fun IndexFlashcardsScreen(
    state: IndexFlashcardsState,
    settings: Settings,
    covered: Boolean,
    actions: IndexFlashcardsActions,
    onOpenDrawer: () -> Unit,
    onOpenIndices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlashcardPage(
        run = state.run,
        settings = settings,
        covered = covered,
        actions = actions,
        topBar = {
            DrillBar(
                state.run.lastGrade,
                title = stringResource(R.string.index_flashcards),
                showTime = settings.flashcardTimer,
                onOpenDrawer = onOpenDrawer,
                // The hand, or for insurance, which has none, the play
                bold = { grade -> grade.card.fact.deviation.let { if (it is Deviation.OnHand) it.title else it.playName } },
                parts = { feedbackParts(it) },
            )
        },
        modifier = modifier,
        empty = {
            Column(
                modifier = Modifier.weight(1f).wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.no_indices_switched_on),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = onOpenIndices) { Text(stringResource(R.string.choose_indices)) }
            }
        },
        face = { card -> HandsColumn(card.upcard, card.player, settings.handTotals) },
    ) { card ->
        Prompt(card.fact.deviation.playName, card.fact.direction)
        // The card this frame shows, even if a tap lands after the next one is dealt but before it is drawn
        NumberPad(state.presets.active.flashcardPad) { number -> actions.answer(card, number) }
        if (state.options.noIndexCards) NoIndexButton { actions.answer(card, null) }
    }
}

/**
 * "Hard 16 vs 10", "Stand at 0 or higher" or "No index" and, after a wrong answer, "You answered +2". Insurance names itself as the
 * play, so only once.
 */
@Composable
private fun feedbackParts(grade: FlashcardGrade): List<String> {
    val fact = grade.card.fact
    val right = when (fact) {
        is PlayIndex -> fact.inPlainWords
        is NoIndex -> stringResource(R.string.no_index)
    }
    val answered = grade.answer?.let { stringResource(R.string.you_answered, signedCount(it)) } ?: stringResource(R.string.you_answered_no_index)

    return listOfNotNull(fact.deviation.title.takeIf { fact.deviation is Deviation.OnHand }, right, answered.takeIf { !grade.isCorrect })
}

/** Ringed and at a fixed size like the pad's numbers. */
@Composable
private fun NoIndexButton(onClick: () -> Unit) {
    val name = stringResource(R.string.no_index)

    ProvideDefaultFontScale {
        RingButton(name = name, onClick = onClick) { Text(text = name.uppercase(), fontWeight = FontWeight.Bold) }
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
