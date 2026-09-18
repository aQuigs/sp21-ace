package com.aquigs.sp21ace.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R

// As in Blackjack Ace, rows leave an icon's width at their start, so every title lines up under a page's intro text
private val IconSpace = 24.dp

// A list item's own start padding and the gap after its leading content, which the intro copies to line up with the rows
private val ListItemInset = 16.dp

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

/** A heading over a group of settings, lined up with the rows' titles and coloured as the drawer's section headings are. */
@Composable
fun SettingsHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .padding(start = ListItemInset + IconSpace + ListItemInset, top = 16.dp, end = ListItemInset, bottom = 4.dp)
            .semantics { heading() },
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.titleSmall,
    )
}

/** A setting with several values: its title over the value chosen, opening a [ChoicePage] of the values. */
@Composable
fun ChoiceRow(title: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingItem(title, modifier.clickable(onClick = onClick), supporting = value)
}

/** A yes or no setting. The whole row toggles, so a screen reader announces one switch named by its title. */
@Composable
fun SwitchRow(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    SettingItem(
        title,
        modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        supporting = summary,
        trailing = { Switch(checked = checked, onCheckedChange = null) },
    )
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

/** A setting that does something rather than holding a value, such as clearing a history. As in Blackjack Ace, its title takes the accent colour. */
@Composable
fun ActionRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SettingItem(title, modifier.clickable(role = Role.Button, onClick = onClick), color = MaterialTheme.colorScheme.secondary)
}

/** A setting's values on a page of their own, named after the setting. As in Blackjack Ace, choosing a value goes back. */
@Composable
fun <T> ChoicePage(
    title: String,
    choices: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(title = title, onBack = onBack, modifier = modifier) {
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
) {
    ListItem(
        headlineContent = { Text(title, color = color, fontWeight = if (emphasized) FontWeight.Bold else null) },
        modifier = modifier,
        supportingContent = supporting?.let { text -> { Text(text) } },
        leadingContent = {
            if (icon == null) Spacer(Modifier.size(IconSpace)) else Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(IconSpace))
        },
        trailingContent = trailing,
    )
}
