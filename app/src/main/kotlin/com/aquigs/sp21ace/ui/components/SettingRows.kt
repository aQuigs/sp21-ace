package com.aquigs.sp21ace.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.theme.disabledContent
import kotlin.math.roundToInt

// As in Blackjack Ace, rows leave an icon's width at their start, so every title lines up under a page's intro text
private val IconSpace = 24.dp

// A list item's own start padding and the gap after its leading content, which the intro copies to line up with the rows
private val ListItemInset = 16.dp

private val TitleInset = ListItemInset + IconSpace + ListItemInset

/** A page of settings, opened over another page, whose rows scroll under its app bar. */
@Composable
fun SettingsPage(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    SubPage(title = title, onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()), content = content)
    }
}

/**
 * A page's intro, with an info icon beside its first line, as in Blackjack Ace, where a list item would centre it on the
 * paragraph. [text] is read as HTML, so an intro can put words in bold.
 */
@Composable
fun SettingsIntro(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = ListItemInset, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(ListItemInset),
    ) {
        Icon(
            painterResource(R.drawable.ic_info),
            contentDescription = null,
            modifier = Modifier.size(IconSpace),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = remember(text) { AnnotatedString.fromHtml(text) },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** A heading over a group of settings, lined up with the rows' titles. */
@Composable
fun SettingsHeader(text: String, modifier: Modifier = Modifier) {
    SectionHeading(text, modifier.padding(start = TitleInset, top = 16.dp, end = ListItemInset, bottom = 4.dp))
}

/** A setting with several values: its title over the value chosen, opening a [ChoicePage] of the values. */
@Composable
fun ChoiceRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingItem(title, modifier.clickable(onClick = onClick), supporting = value)
}

/**
 * A yes or no setting. The whole row toggles, so a screen reader announces one switch named by its title. A row that isn't
 * [enabled] greys out and keeps its value.
 */
@Composable
fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    SettingItem(
        title,
        modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
        supporting = summary,
        trailing = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        enabled = enabled,
    )
}

/**
 * A setting with a range of whole numbers: its title over [label], the value chosen, over a slider. As in Blackjack Ace, the label
 * follows the slider as it moves, and each value is kept as the slider reaches it.
 */
@Composable
fun SliderRow(title: String, label: String, value: Int, range: IntRange, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    SettingItem(title, modifier, supporting = label) {
        Slider(
            value = value.toFloat(),
            onValueChange = { position -> position.roundToInt().takeIf { it != value }?.let(onValueChange) },
            modifier = Modifier.semantics {
                contentDescription = title
                stateDescription = label
            },
            // Whole-number steps, but as in Blackjack Ace a plain track rather than a tick for each of them
            colors = SliderDefaults.colors(activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
        )
    }
}

/** Settings under a header that hides or shows them. Every group starts open, as Blackjack Ace's do. */
@Composable
fun SettingsGroup(title: String, summary: String, @DrawableRes icon: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    SettingItem(
        title,
        modifier.clickable(onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand)) { expanded = !expanded },
        supporting = summary,
        icon = icon,
        emphasized = true,
        trailing = {
            Icon(painterResource(R.drawable.ic_expand_less), contentDescription = null, modifier = Modifier.rotate(if (expanded) 0f else 180f))
        },
    )
    if (expanded) content()
}

/**
 * A yes or no setting with more to it on a page of its own, such as an index to drill or not and edit. The row opens the page,
 * named [clickLabel] to a screen reader, and only the switch, named by the row's title, toggles.
 */
@Composable
fun EditableSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    clickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingItem(
        title,
        modifier.clickable(onClickLabel = clickLabel, onClick = onClick),
        supporting = summary,
        trailing = {
            // A divider sets the switch apart from the row it sits on, as Android's own settings do
            Row(horizontalArrangement = Arrangement.spacedBy(ListItemInset), verticalAlignment = Alignment.CenterVertically) {
                VerticalDivider(Modifier.height(32.dp))
                Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.semantics { contentDescription = title })
            }
        },
    )
}

/**
 * One of a page's values to choose from, as on a [ChoicePage], that also has [actions] of its own in a menu, each a label and
 * what it does, or null for none just now. Choosing the value goes back.
 */
@Composable
fun MenuChoiceRow(
    title: String,
    summary: String,
    selected: Boolean,
    onSelect: () -> Unit,
    actions: List<Pair<String, (() -> Unit)?>>,
    modifier: Modifier = Modifier,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    SettingItem(
        title,
        modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        supporting = summary,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = selected, onClick = null)
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.more_options_for, title))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        actions.forEach { (label, action) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    menuOpen = false
                                    action?.invoke()
                                },
                                enabled = action != null,
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * A setting's values as chips under a [SettingsHeader] saying [name], lined up with the rows' titles, none selected while
 * [selected] is null. A screen reader hears each chip with the setting's name, since two settings can share a value, such as a
 * total of 10 and an upcard of 10.
 */
@Composable
fun <T> ChipChoices(name: String, choices: List<Pair<T, String>>, selected: T?, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.padding(start = TitleInset, end = ListItemInset).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        choices.forEach { (value, label) ->
            FilterChip(
                selected = value == selected,
                onClick = { onSelect(value) },
                // At least as wide as its touch target, which would otherwise center a one-digit chip out of line with the rest
                label = { Text(label, modifier = Modifier.widthIn(min = 16.dp), textAlign = TextAlign.Center) },
                modifier = Modifier.semantics { contentDescription = "$name, $label" },
            )
        }
    }
}

/** A line of a form's own, such as what it will make or why it can't, lined up with the rows' titles. */
@Composable
fun SettingsNote(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(
        text = text,
        modifier = modifier.padding(start = TitleInset, top = 4.dp, end = ListItemInset, bottom = 8.dp),
        color = color,
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** A value shown but not changed here, such as the hand of an index being edited. */
@Composable
fun InfoRow(title: String, summary: String, modifier: Modifier = Modifier) {
    SettingItem(title, modifier.semantics(mergeDescendants = true) {}, supporting = summary)
}

/** A setting that does something rather than holding a value, such as clearing a history. As in Blackjack Ace, its title takes the accent colour. */
@Composable
fun ActionRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingItem(title, modifier.clickable(role = Role.Button, onClick = onClick), color = MaterialTheme.colorScheme.secondary)
}

/** A setting's values on a page of their own, named after the setting, under an [intro] if given. As in Blackjack Ace, choosing a value goes back. */
@Composable
fun <T> ChoicePage(
    title: String,
    choices: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    intro: String? = null,
) {
    SettingsPage(title = title, onBack = onBack, modifier = modifier) {
        intro?.let { SettingsIntro(it) }
        Column(modifier = Modifier.selectableGroup()) {
            choices.forEach { (value, label) ->
                val isSelected = value == selected

                SettingItem(
                    label,
                    Modifier.selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = {
                            onSelect(value)
                            onBack()
                        },
                    ),
                    trailing = { RadioButton(selected = isSelected, onClick = null) },
                )
            }
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    modifier: Modifier,
    supporting: String? = null,
    @DrawableRes icon: Int? = null,
    emphasized: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    color: Color = Color.Unspecified,
    enabled: Boolean = true,
    below: (@Composable () -> Unit)? = null,
) {
    // A list item has no disabled state of its own to apply it
    val disabled = MaterialTheme.colorScheme.disabledContent

    ListItem(
        headlineContent = { Text(title, color = if (enabled) color else disabled, fontWeight = if (emphasized) FontWeight.Bold else null) },
        modifier = modifier,
        supportingContent = if (supporting == null && below == null) null else {
            {
                Column {
                    supporting?.let { Text(it, color = if (enabled) Color.Unspecified else disabled) }
                    below?.invoke()
                }
            }
        },
        leadingContent = {
            if (icon == null) Spacer(Modifier.size(IconSpace)) else Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(IconSpace))
        },
        trailingContent = trailing,
    )
}
