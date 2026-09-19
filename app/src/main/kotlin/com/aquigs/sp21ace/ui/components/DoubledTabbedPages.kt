package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R

private val CHOICES = listOf(false to R.string.not_doubled, true to R.string.already_doubled)

/**
 * [TabbedPages] under a choice between hands not yet doubled and hands already doubled, as Wizard of Odds prints a chart for
 * each, since a doubled hand can't hit and its moves are read from tables of their own. [doubled] says which group a tab is in.
 * It opens on hands not yet doubled and keeps the group chosen through recreation. Only the chosen group's [tabs] show, and
 * choosing the other group opens its first tab. A button rather than tabs, because the tabs below it already swipe.
 */
@Composable
fun <T : Enum<T>> DoubledTabbedPages(
    tabs: List<T>,
    doubled: (T) -> Boolean,
    title: (T) -> Int,
    modifier: Modifier = Modifier,
    page: @Composable (T) -> Unit,
) {
    var chosen by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = MaxContentWidth).fillMaxWidth()) {
            CHOICES.forEachIndexed { index, (choice, label) ->
                SegmentedButton(
                    selected = choice == chosen,
                    onClick = { chosen = choice },
                    shape = SegmentedButtonDefaults.itemShape(index, CHOICES.size),
                    label = { Text(stringResource(label), maxLines = 1, autoSize = autoSizeDownTo(minSize = 8.dp, maxFontSize = LocalTextStyle.current.fontSize)) },
                )
            }
        }

        // A tab of the other group, or one the rules have dropped, isn't among these, so the pages fall back to the group's first
        TabbedPages(tabs = tabs.filter { doubled(it) == chosen }, title = title, modifier = Modifier.weight(1f).fillMaxWidth(), page = page)
    }
}
