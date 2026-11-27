package com.aquigs.sp21ace.ui.flashcards

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.AnswerPad
import com.aquigs.sp21ace.domain.drill.DrillCard
import com.aquigs.sp21ace.domain.drill.DrillGrade
import com.aquigs.sp21ace.domain.drill.DrillRun
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.ui.components.CircleButton
import com.aquigs.sp21ace.ui.components.DrillTable
import com.aquigs.sp21ace.ui.components.FeedbackBar
import com.aquigs.sp21ace.ui.components.FeedbackText
import com.aquigs.sp21ace.ui.components.ProvideDefaultFontScale
import com.aquigs.sp21ace.ui.components.percentText
import kotlinx.coroutines.delay
import kotlin.time.Duration

// The time shows tenths of a second
private const val TICK_MILLIS = 100L

private val PadGap = 6.dp
private val MinButton = 40.dp
private val MaxButton = 56.dp

/** What a flashcard drill's page asks for, which the composition root carries out with the drill's history. */
interface DrillActions<C : DrillCard<N>, N> {
    /** [answer] tapped for [asked], the card that frame showed. */
    fun answer(asked: C, answer: N)

    /** [card] is on screen with nothing over it. */
    fun shown(card: C)

    /** The card on screen was covered or left, so its answer will have no time. */
    fun interrupted()

    /** The time on [card] so far, which its answer would record: zero until it first shows, or null when it isn't timed. */
    fun time(card: C): Duration?

    /** Forgets every answer to the drill, which nothing brings back. */
    fun clearHistory()
}

/**
 * A flashcard drill's page: [face] shows the card beside the streak meter and the card's time, as [settings] choose, and
 * [answering] asks for its answer below, or with no card to deal, [empty] says so. Leaving the page or the app, the window losing
 * focus, or the drawer [covered] over it, interrupts the card showing, so its answer records no time.
 */
@Composable
internal fun <C : DrillCard<N>, N> FlashcardPage(
    run: DrillRun<C, N>,
    settings: Settings,
    covered: Boolean,
    actions: DrillActions<C, N>,
    topBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    empty: @Composable ColumnScope.() -> Unit = {},
    face: @Composable RowScope.(card: C) -> Unit,
    answering: @Composable ColumnScope.(card: C) -> Unit,
) {
    val card = run.card
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

    Scaffold(modifier = modifier, topBar = topBar) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (card == null) {
                empty()
            } else {
                DrillTable(
                    streak = run.streak,
                    settings = settings,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    overMeter = if (settings.flashcardTimer) ({ CardTime(time) }) else null,
                ) {
                    face(card)
                }
                answering(card)
            }
        }
    }
}

/**
 * A drill's app bar: its [title] until the first answer, then [parts] of the verdict on the last one, what [bold] says of the
 * first in bold, or all of it.
 */
@Composable
internal fun <C : DrillCard<N>, N> DrillBar(
    lastGrade: DrillGrade<C, N>?,
    title: String,
    showTime: Boolean,
    onOpenDrawer: () -> Unit,
    bold: (DrillGrade<C, N>) -> String? = { null },
    parts: @Composable (DrillGrade<C, N>) -> List<String>,
) {
    FeedbackBar(
        verdict = lastGrade,
        title = title,
        isCorrect = { it.isCorrect },
        spoken = lastGrade?.let { (parts(it) + figures(it, showTime, R.string.seconds_spoken)).joinToString(". ") },
        onOpenDrawer = onOpenDrawer,
    ) { grade ->
        val shown = parts(grade) + figures(grade, showTime, R.string.seconds_value)
        val lead = (bold(grade) ?: shown.first()).unbroken()

        FeedbackText(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lead) }
                append(shown.joinToString(SEPARATOR) { it.unbroken() }.removePrefix(lead))
            },
        )
    }
}

/** For a timed card "2.4 s", as [timeFormat] words it, and "Accuracy 66.6%", how often what it asked has ever been answered right. */
@Composable
private fun figures(grade: DrillGrade<*, *>, showTime: Boolean, @StringRes timeFormat: Int): List<String> = listOfNotNull(
    grade.time?.takeIf { showTime }?.let { stringResource(timeFormat, it.inSeconds) },
    grade.tally?.percentText()?.let { stringResource(R.string.flashcard_accuracy, it) },
)

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

/**
 * A button for every number of [pad], the negatives first, then zero and the positives, each group in rows as even as the width
 * allows, so the buttons stay at least [MinButton] across. They depend only on [pad], so they never move between cards.
 */
@Composable
internal fun NumberPad(pad: AnswerPad, onAnswer: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val rows = padRows(pad.numbers, perRow = ((maxWidth + PadGap) / (MinButton + PadGap)).toInt().coerceAtLeast(1))
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
                        row.forEach { number -> NumberButton(pad, number, size, onClick = { onAnswer(number) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberButton(pad: AnswerPad, number: Int, size: Dp, onClick: () -> Unit) {
    val name = when {
        pad.end(number) != null -> pad.inPlainWords(number)
        number > 0 -> stringResource(R.string.count_plus, number)
        number < 0 -> stringResource(R.string.count_minus, -number)
        else -> "0"
    }

    CircleButton(name = name, onClick = onClick, modifier = Modifier.size(size), label = pad.label(number), maxFontSize = 20.sp)
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
