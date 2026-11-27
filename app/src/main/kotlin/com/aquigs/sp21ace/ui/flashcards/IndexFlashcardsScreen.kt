package com.aquigs.sp21ace.ui.flashcards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.aquigs.sp21ace.domain.indices.flashcardAnswers
import com.aquigs.sp21ace.domain.indices.inPlainWords
import com.aquigs.sp21ace.domain.indices.playName
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.ui.components.CircleButton
import com.aquigs.sp21ace.ui.components.Dissolve
import com.aquigs.sp21ace.ui.components.DrillTable
import com.aquigs.sp21ace.ui.components.FeedbackBar
import com.aquigs.sp21ace.ui.components.FeedbackText
import com.aquigs.sp21ace.ui.components.HandsColumn
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale
import kotlinx.coroutines.delay
import kotlin.time.Duration

// The time shows tenths of a second
private const val TICK_MILLIS = 100L

private val PadGap = 6.dp
private val MinButton = 40.dp
private val MaxButton = 56.dp

/**
 * A flashcard of an index: the cards, the play it asks about, a pad of true counts to answer with and, as [settings] choose, the
 * streak and the card's time, or with no index switched on, a way to the Indices page. Leaving the page or the app, the window
 * losing focus, or the drawer [covered] over it, interrupts the card showing, so its answer records no time.
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
    val card = state.run.card
    val currentActions by rememberUpdatedState(actions)
    // A window without focus, as in split screen, under the notification shade or behind a system dialog, stays resumed but isn't
    // where the player is looking
    val hidden = covered || !LocalWindowInfo.current.isWindowFocused

    LifecycleResumeEffect(Unit) { onPauseOrDispose { currentActions.interrupted() } }
    val time = remember(card) { mutableStateOf(card?.let(actions::time)) }
    LaunchedEffect(card, hidden, settings.flashcardTimer) {
        if (hidden) currentActions.interrupted()
        if (card != null && !hidden) currentActions.shown(card)
        if (card == null || !settings.flashcardTimer) return@LaunchedEffect

        while (true) {
            val shown = currentActions.time(card)
            time.value = shown
            // Once untimed a card stays so, and a hidden one's time stands still
            if (hidden || shown == null) break
            delay(TICK_MILLIS - shown.inWholeMilliseconds % TICK_MILLIS)
        }
    }

    Scaffold(modifier = modifier, topBar = { FlashcardBar(state.run.lastGrade, onOpenDrawer) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (card == null) {
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
            } else {
                DrillTable(
                    streak = state.run.streak,
                    settings = settings,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    overMeter = if (settings.flashcardTimer) ({ CardTime(time) }) else null,
                ) {
                    HandsColumn(card.upcard, card.player, settings.handTotals)
                }
                Prompt(card.index.deviation.playName, card.index.direction)
                // The card this frame shows, even if a tap lands after the next one is dealt but before it is drawn
                NumberPad(state.presets.active.flashcardAnswers) { number -> actions.answer(card, number) }
            }
        }
    }
}

@Composable
private fun FlashcardBar(lastGrade: FlashcardGrade?, onOpenDrawer: () -> Unit) {
    FeedbackBar(
        verdict = lastGrade,
        title = stringResource(R.string.index_flashcards),
        isCorrect = { it.isCorrect },
        spoken = lastGrade?.let { feedbackParts(it).joinToString(". ") },
        onOpenDrawer = onOpenDrawer,
    ) { grade ->
        val deviation = grade.card.index.deviation
        // The hand in bold, or for insurance, which has none, the play
        val bold = (if (deviation is Deviation.OnHand) deviation.title else deviation.playName).unbroken()
        val text = feedbackParts(grade).joinToString(SEPARATOR) { it.unbroken() }

        FeedbackText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                append(text.removePrefix(bold))
            },
        )
    }
}

/** "Hard 16 vs 10", "Stand at 0 or higher" and, after a wrong answer, "You answered +2". Insurance names itself as the play, so only once. */
@Composable
private fun feedbackParts(grade: FlashcardGrade): List<String> {
    val index = grade.card.index
    val answered = if (grade.isCorrect) null else stringResource(R.string.you_answered, signedCount(grade.answer))

    return listOfNotNull(index.deviation.title.takeIf { index.deviation is Deviation.OnHand }, index.inPlainWords, answered)
}

// The bar breaks its lines only after the bar between parts, so a part such as "at −1 or higher" never ends one line and starts the next
private const val SEPARATOR = "\u00A0| "

private fun String.unbroken() = replace(' ', '\u00A0')

/**
 * The card's time under its caption, at the default font size, so it takes no more width than the meter under it. Reads [time]
 * itself, so each tick redraws only this. A screen reader hears whole seconds, which change less often.
 */
@Composable
private fun CardTime(time: State<Duration?>) {
    val shown = time.value
    val seconds = shown?.inWholeSeconds?.toInt()
    val spoken = seconds?.let { pluralStringResource(R.plurals.card_time_spoken, it, it) } ?: stringResource(R.string.card_untimed)
    val color = MaterialTheme.colorScheme.onSurfaceVariant

    ProvideDefaultFontScale {
        Column(modifier = Modifier.clearAndSetSemantics { contentDescription = spoken }, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = stringResource(R.string.card_time), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(
                text = shown?.let { secondsText(it) } ?: stringResource(R.string.no_data),
                color = color,
                fontWeight = FontWeight.Bold,
                // Digits of one width, so a time doesn't shake as it counts
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            )
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
