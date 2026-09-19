package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A sub-page's tabs over their pages, which a swipe moves between as a tap on a tab does, as in Blackjack Ace. [onSelect] hears of
 * each tab tapped or swiped towards, for the caller to keep. The pages open on [selected], or the first tab if [tabs] lacks it, and
 * again whenever [tabs] change. [scrollable] is for names too long to share the width evenly.
 */
@Composable
fun <T> TabbedPages(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    title: (T) -> Int,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
    page: @Composable (T) -> Unit,
) {
    val currentOnSelect by rememberUpdatedState(onSelect)
    // Not saved, so a recreation reopens on the caller's choice rather than wherever a slide was cut short. New tabs, such as the
    // rules adding or dropping one, reopen on the chosen tab rather than on whichever sits at its old index.
    val pager = remember(tabs) { PagerState(currentPage = tabs.indexOf(selected).coerceAtLeast(0)) { tabs.size } }
    val scope = rememberCoroutineScope()

    // The target rather than the current page, so a tap on a tab two away doesn't select the one it slides past
    LaunchedEffect(pager) {
        snapshotFlow { pager.targetPage }.collect { currentOnSelect(tabs[it]) }
    }

    Column(modifier = modifier) {
        PageTabRow(
            tabs = tabs,
            selected = tabs[pager.targetPage],
            onSelect = { tab ->
                currentOnSelect(tab)
                scope.launch { pager.animateScrollToPage(tabs.indexOf(tab)) }
            },
            title = title,
            scrollable = scrollable,
        )
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) { index -> page(tabs[index]) }
    }
}

/** The chosen tab named in the primary colour. */
@Composable
private fun <T> PageTabRow(tabs: List<T>, selected: T, onSelect: (T) -> Unit, title: (T) -> Int, scrollable: Boolean) {
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
