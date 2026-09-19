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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R

private val CHOICES = listOf(false to R.string.not_doubled, true to R.string.already_doubled)

/**
 * [TabbedPages] under a choice between hands not yet doubled and hands already doubled, as Wizard of Odds prints a chart for
 * each, since a doubled hand can't hit and its moves are read from tables of their own. [doubled] says which group a tab is in;
 * only the chosen group's [tabs] show, and choosing the other group opens its first tab. A button rather than tabs, because the
 * tabs below it already swipe.
 */
@Composable
fun <T> DoubledTabbedPages(
    tabs: List<T>,
    doubled: (T) -> Boolean,
    selected: T,
    onSelect: (T) -> Unit,
    title: (T) -> Int,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    page: @Composable (T) -> Unit,
) {
    val chosen = doubled(selected)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).widthIn(max = MaxContentWidth).fillMaxWidth()) {
            CHOICES.forEachIndexed { index, (choice, label) ->
                SegmentedButton(
                    selected = choice == chosen,
                    onClick = { if (choice != chosen) onSelect(tabs.first { doubled(it) == choice }) },
                    shape = SegmentedButtonDefaults.itemShape(index, CHOICES.size),
                    label = { Text(stringResource(label), maxLines = 1, autoSize = autoSizeDownTo(minSize = 8.dp, maxFontSize = LocalTextStyle.current.fontSize)) },
                )
            }
        }

        // A tab the rules have since dropped, such as Already doubled's Soft once redoubling isn't allowed, falls back to the
        // first of its group
        TabbedPages(
            tabs = tabs.filter { doubled(it) == chosen },
            selected = selected,
            onSelect = onSelect,
            title = title,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            scrollable = scrollable,
            page = page,
        )
    }
}
