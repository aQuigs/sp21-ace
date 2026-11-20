package com.aquigs.sp21ace.ui.indices

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.INDEX_NUMBERS
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.IndexDraft
import com.aquigs.sp21ace.domain.indices.IndexKind
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.indices.inPlainWords
import com.aquigs.sp21ace.domain.indices.indexInPlainWords
import com.aquigs.sp21ace.domain.indices.numberInPlainWords
import com.aquigs.sp21ace.domain.indices.playName
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.ui.components.ActionRow
import com.aquigs.sp21ace.ui.components.ChipChoices
import com.aquigs.sp21ace.ui.components.ConfirmDialog
import com.aquigs.sp21ace.ui.components.InfoRow
import com.aquigs.sp21ace.ui.components.SettingsHeader
import com.aquigs.sp21ace.ui.components.SettingsNote
import com.aquigs.sp21ace.ui.components.SettingsPage

// Blackjack Ace's order for upcards, the ace last
private val UPCARDS = Upcard.entries.filter { it != Upcard.ACE } + Upcard.ACE

/**
 * A new index for the active preset: its hand, or insurance, the dealer's upcard, the play the hand's kind offers, the direction
 * and the number. A deviation the preset already lists can't be added again. Nothing changes until Add, so Back cancels.
 */
@Composable
fun AddIndexScreen(presets: IndexPresets, onChange: (IndexPresets) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var draft by rememberSaveable { mutableStateOf(IndexDraft()) }
    val index = draft.index
    val listed = index?.takeIf { presets.active.has(it.deviation) }
    val toAdd = index?.takeIf { listed == null }

    SettingsPage(title = stringResource(R.string.add_index), onBack = onBack, modifier = modifier) {
        Chips(
            name = stringResource(R.string.index_hand),
            choices = IndexKind.entries.map { it to stringResource(it.title) },
            selected = draft.kind,
            onSelect = { draft = draft.withKind(it) },
        )

        if (draft.kind != IndexKind.INSURANCE) {
            Chips(
                name = stringResource(if (draft.kind == IndexKind.PAIR) R.string.pair_of else R.string.total),
                choices = draft.values.map { it to if (draft.kind == IndexKind.PAIR) Upcard.ofPoints(it).label else "$it" },
                selected = draft.value,
                onSelect = { draft = draft.copy(value = it) },
            )
            Chips(stringResource(R.string.dealers_upcard), UPCARDS.map { it to it.label }, draft.upcard) { draft = draft.copy(upcard = it) }
            Chips(stringResource(R.string.index_play), draft.plays.map { it to it.title }, draft.play) { draft = draft.copy(play = it) }
        }

        NumberFields(
            play = index?.deviation?.playName ?: draft.play?.title,
            direction = draft.direction,
            threshold = draft.threshold,
            onDirection = { draft = draft.copy(direction = it) },
            onThreshold = { draft = draft.copy(threshold = it) },
        )

        if (listed != null) {
            SettingsNote(
                text = stringResource(R.string.index_already_listed, listed.deviation.question),
                modifier = Modifier.padding(top = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error,
            )
        }
        Button(
            onClick = {
                toAdd?.let { added ->
                    onChange(presets.updateActive { it.add(added) })
                    onBack()
                }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            enabled = toAdd != null,
        ) {
            Text(stringResource(R.string.add_index))
        }
    }
}

/**
 * The active preset's index on [deviation]: its direction and number to change, or the index to delete. Nothing changes until
 * Save or a confirmed delete, so Back cancels.
 */
@Composable
fun EditIndexScreen(
    presets: IndexPresets,
    deviation: Deviation,
    onChange: (IndexPresets) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = presets.active.find(deviation)
    if (entry == null) {
        // Gone from under the page, as a restore after a preset swap elsewhere would leave it
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var direction by rememberSaveable(deviation) { mutableStateOf(entry.index.direction) }
    var threshold by rememberSaveable(deviation) { mutableIntStateOf(entry.index.threshold.coerceIn(INDEX_NUMBERS)) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    SettingsPage(title = stringResource(R.string.edit_index), onBack = onBack, modifier = modifier) {
        InfoRow(title = deviation.title, summary = deviation.playName)
        NumberFields(deviation.playName, direction, threshold, onDirection = { direction = it }, onThreshold = { threshold = it })
        Button(
            onClick = {
                onChange(presets.updateActive { it.edit(PlayIndex(deviation, threshold, direction)) })
                onBack()
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Text(stringResource(R.string.save))
        }
        HorizontalDivider()
        ActionRow(title = stringResource(R.string.delete_index), onClick = { confirmingDelete = true })
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_index_title),
            message = stringResource(R.string.delete_index_message, deviation.title, entry.index.inPlainWords),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                confirmingDelete = false
                onChange(presets.updateActive { it.remove(deviation) })
                onBack()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

/**
 * The direction, and the number as chips labeled as the flashcards' pad labels them, since a slider's 21 stops are too close
 * together to hit an exact one. Over the numbers, a line reads as the index will, such as "Stand at +3 or higher", once [play]
 * is known.
 */
@Composable
private fun NumberFields(play: String?, direction: IndexDirection, threshold: Int, onDirection: (IndexDirection) -> Unit, onThreshold: (Int) -> Unit) {
    val number = numberInPlainWords(threshold, direction)
    val name = stringResource(R.string.true_count)

    Chips(stringResource(R.string.direction), IndexDirection.entries.map { it to it.inPlainWords }, direction, onDirection)
    SettingsHeader(name)
    SettingsNote(play?.let { indexInPlainWords(it, threshold, direction) } ?: number)
    ChipChoices(name, INDEX_NUMBERS.map { it to signedCount(it) }, threshold, onThreshold)
}

/** A field of the form: its heading over its values. */
@Composable
private fun <T> Chips(name: String, choices: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit) {
    SettingsHeader(name)
    ChipChoices(name, choices, selected, onSelect)
}

// "Hard 16 vs 10, Stand", or "Insurance", which names itself
private val Deviation.question: String get() = if (this is Deviation.OnHand) "$title, $playName" else title

private val IndexKind.title: Int
    get() = when (this) {
        IndexKind.HARD -> R.string.hard_total
        IndexKind.SOFT -> R.string.soft_total
        IndexKind.PAIR -> R.string.pair
        IndexKind.INSURANCE -> R.string.insurance
    }
