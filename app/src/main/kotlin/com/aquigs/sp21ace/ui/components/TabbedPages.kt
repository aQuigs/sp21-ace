package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A sub-page's tabs over their pages, which a swipe moves between as a tap on a tab does, as in Blackjack Ace. It opens on the
 * first tab and keeps the one chosen through recreation. New [tabs] that lack it, such as After doubling: soft once the rules drop
 * redoubling, fall back to the first. The tabs scroll, since the tables after Hard, Soft and Pairs have long names.
 */
@Composable
fun <T : Enum<T>> TabbedPages(tabs: List<T>, title: (T) -> Int, modifier: Modifier = Modifier, page: @Composable (T) -> Unit) {
    var chosen by rememberSaveable { mutableStateOf(tabs.first()) }
    // Not saved itself, so a recreation reopens on the chosen tab rather than wherever a slide was cut short. Rebuilt with new tabs,
    // so the chosen one stays open wherever it now sits.
    val pager = remember(tabs) { PagerState(currentPage = tabs.indexOf(chosen).coerceAtLeast(0)) { tabs.size } }
    val scope = rememberCoroutineScope()

    // The target rather than the current page, so a tap on a tab two away doesn't choose the one it slides past
    LaunchedEffect(pager) {
        snapshotFlow { pager.targetPage }.collect { chosen = tabs[it] }
    }

    Column(modifier = modifier) {
        SecondaryScrollableTabRow(selectedTabIndex = pager.targetPage, edgePadding = 0.dp, minTabWidth = 72.dp) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == pager.targetPage,
                    onClick = {
                        // At once, since a recreation can come before the slide does
                        chosen = tab
                        scope.launch { pager.animateScrollToPage(index) }
                    },
                    text = { Text(stringResource(title(tab))) },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) { index -> page(tabs[index]) }
    }
}
