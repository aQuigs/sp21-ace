package com.aquigs.sp21ace.ui.truecount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.DrillGrade
import com.aquigs.sp21ace.domain.drill.DrillRun
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.truecount.Decks
import com.aquigs.sp21ace.domain.truecount.DecksShown
import com.aquigs.sp21ace.domain.truecount.TRUE_COUNT_PAD
import com.aquigs.sp21ace.domain.truecount.TrueCountAnswer
import com.aquigs.sp21ace.domain.truecount.TrueCountCard
import com.aquigs.sp21ace.domain.truecount.TrueCountOptions
import com.aquigs.sp21ace.ui.components.Dissolve
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale
import com.aquigs.sp21ace.ui.flashcards.DrillActions
import com.aquigs.sp21ace.ui.flashcards.DrillBar
import com.aquigs.sp21ace.ui.flashcards.FlashcardPage
import com.aquigs.sp21ace.ui.flashcards.NumberPad

/** What the true count pages show: the flashcard run, the options it is dealt by and every answer given. */
@Immutable
data class TrueCountState(
    val run: DrillRun<TrueCountCard, Int>,
    val options: TrueCountOptions,
    val history: List<TrueCountAnswer>,
)

/** What the true count pages ask for, which the composition root carries out with the stores. */
interface TrueCountActions : DrillActions<TrueCountCard, Int> {
    fun changeOptions(options: TrueCountOptions)
}

/** A running count and some decks, to convert to a true count on an open-ended pad. [covered] is the drawer over it, as [FlashcardPage] says. */
@Composable
fun TrueCountFlashcardsScreen(
    state: TrueCountState,
    settings: Settings,
    covered: Boolean,
    actions: TrueCountActions,
    onOpenDrawer: () -> Unit,
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
                title = stringResource(R.string.true_count_flashcards),
                showTime = settings.flashcardTimer,
                onOpenDrawer = onOpenDrawer,
                parts = { feedbackParts(it) },
            )
        },
        modifier = modifier,
        face = { card -> Face(card, state.options.decksShown, Modifier.weight(1f).fillMaxHeight()) },
    ) { card ->
        Prompt()
        // The card this frame shows, even if a tap lands after the next one is dealt but before it is drawn
        NumberPad(TRUE_COUNT_PAD) { number -> actions.answer(card, number) }
    }
}

/** "+7 with 3.5 decks left", "True count +2 (+2.00)" and, after a wrong answer, "You answered +1", whichever decks the card showed. */
@Composable
private fun feedbackParts(grade: DrillGrade<TrueCountCard, Int>): List<String> {
    val card = grade.card
    val answered = if (grade.isCorrect) null else stringResource(R.string.you_answered, TRUE_COUNT_PAD.inPlainWords(grade.answer))

    return listOfNotNull(
        stringResource(R.string.count_with_decks, signedCount(card.runningCount), decksLeftText(card.decksLeft)),
        stringResource(R.string.true_count_is, signedCount(card.trueCount), signedCount(card.unroundedTrueCount)),
        answered,
    )
}

/** "3.5 decks left". */
@Composable
internal fun decksLeftText(decks: Decks): String =
    pluralStringResource(R.plurals.decks_left, if (decks == Decks.whole(1)) 1 else 2, decks.label)

// Two figures one over the other, at the default font size
private val StackedHeight = 184.dp

/**
 * The running count over the decks, or beside them where they are too tall, as in split screen. At the default font size, since
 * figures this large need no enlarging.
 */
@Composable
private fun Face(card: TrueCountCard, decksShown: DecksShown, modifier: Modifier = Modifier) {
    ProvideDefaultFontScale {
        BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
            val stacked = maxHeight >= StackedHeight

            Dissolve(card) { shown ->
                val count = @Composable { CardFigure(stringResource(R.string.running_count), signedCount(shown.runningCount)) }
                val decks = @Composable {
                    when (decksShown) {
                        DecksShown.LEFT -> CardFigure(stringResource(R.string.decks_left_caption), shown.decksLeft.label)
                        DecksShown.DISCARDED -> CardFigure(stringResource(R.string.decks_discarded_caption), shown.decksDiscarded.label)
                    }
                }

                if (stacked) {
                    Column(verticalArrangement = Arrangement.spacedBy(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        count()
                        decks()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        count()
                        decks()
                    }
                }
            }
        }
    }
}

/** A [value] under its [caption], one item for a screen reader. */
@Composable
private fun CardFigure(caption: String, value: String) {
    Column(Modifier.semantics(mergeDescendants = true) {}, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = caption, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
        Text(text = value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.displayMedium)
    }
}

@Composable
private fun Prompt() {
    val spoken = stringResource(R.string.true_count_prompt_spoken)

    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.true_count).uppercase()) }
            append(" = ?")
        },
        modifier = Modifier.semantics { contentDescription = spoken },
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleLarge,
    )
}
