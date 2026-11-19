package com.aquigs.sp21ace.ui.indices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.ui.components.ActionRow
import com.aquigs.sp21ace.ui.components.ConfirmDialog
import com.aquigs.sp21ace.ui.components.MenuChoiceRow
import com.aquigs.sp21ace.ui.components.SettingsIntro
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.components.rememberArmed
import java.io.Serializable

// Long enough for any name worth typing, short enough to fit the Preset row
private const val MAX_NAME_LENGTH = 40

private sealed interface PresetDialog : Serializable {
    data object New : PresetDialog

    data class Rename(val id: String) : PresetDialog

    data class Delete(val id: String) : PresetDialog
}

/**
 * Every index preset: choose the one the flashcards drill, which goes back as a choice page does, or rename, duplicate or delete
 * one from its menu, or start a new one. The last preset can't be deleted.
 */
@Composable
fun IndexPresetsScreen(presets: IndexPresets, onChange: (IndexPresets) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    var dialog by rememberSaveable { mutableStateOf<PresetDialog?>(null) }

    SettingsPage(title = stringResource(R.string.index_presets), onBack = onBack, modifier = modifier) {
        SettingsIntro(stringResource(R.string.index_presets_intro))
        Column(modifier = Modifier.selectableGroup()) {
            presets.presets.forEach { preset ->
                val copyName = stringResource(R.string.preset_copy_name, preset.name).take(MAX_NAME_LENGTH)

                MenuChoiceRow(
                    title = preset.name,
                    summary = presetSummary(preset),
                    selected = preset.id == presets.activeId,
                    onSelect = {
                        onChange(presets.activate(preset.id))
                        onBack()
                    },
                    actions = listOf(
                        stringResource(R.string.rename) to { dialog = PresetDialog.Rename(preset.id) },
                        stringResource(R.string.duplicate) to { onChange(presets.duplicate(preset.id, copyName)) },
                        stringResource(R.string.delete) to { dialog = PresetDialog.Delete(preset.id) }.takeIf { presets.canDelete },
                    ),
                )
            }
        }
        HorizontalDivider()
        ActionRow(title = stringResource(R.string.new_preset), onClick = { dialog = PresetDialog.New })
    }

    // A preset deleted meanwhile leaves nothing to ask about
    fun presetOf(id: String) = presets.presets.firstOrNull { it.id == id }
    when (val shown = dialog) {
        null -> {}
        PresetDialog.New -> NewPresetDialog(
            onCreate = { name, start ->
                dialog = null
                onChange(presets.add(name, start))
            },
            onDismiss = { dialog = null },
        )
        is PresetDialog.Rename -> presetOf(shown.id)?.let {
            NameDialog(
                title = stringResource(R.string.rename_preset),
                initial = it.name,
                confirmLabel = stringResource(R.string.save),
                onConfirm = { name ->
                    dialog = null
                    onChange(presets.rename(shown.id, name))
                },
                onDismiss = { dialog = null },
            )
        }
        is PresetDialog.Delete -> presetOf(shown.id)?.takeIf { presets.canDelete }?.let {
            ConfirmDialog(
                title = stringResource(R.string.delete_preset_title),
                message = stringResource(R.string.delete_preset_message, it.name),
                confirmLabel = stringResource(R.string.delete),
                onConfirm = {
                    dialog = null
                    onChange(presets.delete(shown.id))
                },
                onDismiss = { dialog = null },
            )
        }
    }
}

/** "Blackjack, 22 indices", then the preset's two counting conventions. */
@Composable
private fun presetSummary(preset: IndexPreset): String = stringResource(
    R.string.preset_summary,
    stringResource(preset.game.title),
    pluralStringResource(R.plurals.index_count, preset.entries.size, preset.entries.size),
    stringResource(preset.deckEstimate.title),
    stringResource(preset.rounding.title),
)

/** A name, and what the preset starts from: one of the built-in sets, or nothing. */
@Composable
private fun NewPresetDialog(onCreate: (name: String, start: BuiltInIndexSet?) -> Unit, onDismiss: () -> Unit) {
    var start by rememberSaveable { mutableStateOf<BuiltInIndexSet?>(BuiltInIndexSet.entries.first()) }

    NameDialog(
        title = stringResource(R.string.new_preset),
        initial = "",
        confirmLabel = stringResource(R.string.create),
        onConfirm = { onCreate(it, start) },
        onDismiss = onDismiss,
    ) {
        Text(
            text = stringResource(R.string.start_from),
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            style = MaterialTheme.typography.titleSmall,
        )
        Column(modifier = Modifier.selectableGroup()) {
            (BuiltInIndexSet.entries + null).forEach { set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .selectable(selected = set == start, role = Role.RadioButton, onClick = { start = set }),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = set == start, onClick = null)
                    Text(set?.title ?: stringResource(R.string.empty_preset))
                }
            }
        }
    }
}

/**
 * Asks for a preset's name, starting from [initial] selected, so typing replaces it. A blank name can't be confirmed. Like
 * [ConfirmDialog], it waits out a double tap before any way out.
 */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    below: @Composable () -> Unit = {},
) {
    val armed by rememberArmed(Unit)
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { focus.requestFocus() }

    AlertDialog(
        onDismissRequest = { if (armed) onDismiss() },
        confirmButton = {
            TextButton(onClick = { if (armed) onConfirm(name.text.trim()) }, enabled = name.text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = { if (armed) onDismiss() }) { Text(stringResource(R.string.cancel)) } },
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.text.length <= MAX_NAME_LENGTH) name = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    label = { Text(stringResource(R.string.preset_name)) },
                    singleLine = true,
                )
                below()
            }
        },
    )
}

private val Game.title: Int
    get() = when (this) {
        Game.BLACKJACK -> R.string.blackjack
    }
