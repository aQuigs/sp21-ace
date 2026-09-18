package com.aquigs.sp21ace.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** A sub-page's tabs, the chosen one named in the primary colour. [scrollable] is for names too long to share the width evenly. */
@Composable
fun <T> PageTabRow(tabs: List<T>, selected: T, onSelect: (T) -> Unit, title: (T) -> Int, scrollable: Boolean = false) {
    val index = tabs.indexOf(selected)
    val content = @Composable {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(title(tab))) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (scrollable) {
        SecondaryScrollableTabRow(selectedTabIndex = index, edgePadding = 0.dp, minTabWidth = 72.dp, tabs = content)
    } else {
        SecondaryTabRow(selectedTabIndex = index, tabs = content)
    }
}
