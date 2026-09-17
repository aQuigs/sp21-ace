package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

// As in Blackjack Ace, a row leaves an icon's width at its start, so every title lines up under a page's intro text
private val IconSpace = 24.dp

/** A setting with several values: its title over the value chosen, opening a [ChoicePage] of the values. */
@Composable
fun ChoiceRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier.clickable(onClick = onClick),
        supportingContent = { Text(value) },
        leadingContent = { Spacer(Modifier.size(IconSpace)) },
    )
}

/** A yes or no setting. The whole row toggles, so a screen reader announces one switch named by its title. */
@Composable
fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        supportingContent = { Text(summary) },
        leadingContent = { Spacer(Modifier.size(IconSpace)) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

/** A setting's values on a page of their own, named after the setting, with a radio button beside each. */
@Composable
fun <T> ChoicePage(
    title: String,
    choices: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubPage(title = title, onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).selectableGroup()) {
            choices.forEach { (value, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    modifier = Modifier.selectable(selected = value == selected, role = Role.RadioButton, onClick = { onSelect(value) }),
                    leadingContent = { Spacer(Modifier.size(IconSpace)) },
                    trailingContent = { RadioButton(selected = value == selected, onClick = null) },
                )
            }
        }
    }
}
