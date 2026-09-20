package com.aquigs.sp21ace.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.ui.isPlaced
import com.aquigs.sp21ace.ui.onScreen
import com.aquigs.sp21ace.ui.swipeToNextTab
import com.aquigs.sp21ace.ui.swipeToPreviousTab
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TabbedPagesTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val screen = compose.onScreen

    // The chart's names, so the last tabs run off a phone's width as the chart's do
    private enum class Table(val title: Int) {
        HARD(R.string.table_hard),
        SOFT(R.string.table_soft),
        PAIRS(R.string.table_pairs),
        AFTER_DOUBLE_HARD(R.string.table_after_double_hard),
        AFTER_DOUBLE_SOFT(R.string.table_after_double_soft),
    }

    private var tabs by mutableStateOf(Table.entries.toList())

    private val pages = @Composable { Sp21AceTheme { TabbedPages(tabs, title = { it.title }) { Text("${it.name} page") } } }

    private fun tab(table: Table) = screen.onNodeWithText(compose.activity.getString(table.title))

    private fun page(table: Table) = screen.onNodeWithText("${table.name} page")

    // Every page placed, whether on screen or beside it, ready to slide in
    private fun placedPages() = compose.onAllNodes(hasText(" page", substring = true) and isPlaced).fetchSemanticsNodes().size

    private fun assertOpen(table: Table) {
        tab(table).assertIsSelected()
        page(table).assertIsDisplayed()
    }

    @Test
    fun opensOnTheFirstTab() {
        compose.setContent(pages)

        assertOpen(Table.HARD)
    }

    @Test
    fun aSwipeOpensTheNextTabAndASwipeBackTheOneBefore() {
        compose.setContent(pages)

        screen.swipeToNextTab()

        assertOpen(Table.SOFT)
        page(Table.HARD).assertDoesNotExist()

        screen.swipeToPreviousTab()

        assertOpen(Table.HARD)
    }

    @Test
    fun aTapOnATabOpensItsPage() {
        compose.setContent(pages)

        tab(Table.PAIRS).performClick()

        assertOpen(Table.PAIRS)
    }

    @Test
    fun swipingToALastTabOffScreenScrollsItIntoView() {
        compose.setContent(pages)
        val last = tab(Table.AFTER_DOUBLE_SOFT)
        val screenRight = compose.onRoot().getBoundsInRoot().right

        // Unclipped, since the clipped bounds stop at the screen's edge whether the tab does or not
        assertTrue("the last tab starts off screen", last.getUnclippedBoundsInRoot().right > screenRight)

        repeat(Table.entries.size - 1) { screen.swipeToNextTab() }

        assertOpen(Table.AFTER_DOUBLE_SOFT)
        assertTrue("the last tab is in view", last.getUnclippedBoundsInRoot().right <= screenRight)
    }

    @Test
    fun everyPageIsBuiltOnceAndKeptSoASwitchOnlySlidesIt() {
        val built = mutableListOf<Table>()
        compose.setContent {
            Sp21AceTheme {
                TabbedPages(tabs, title = { it.title }) { table ->
                    LaunchedEffect(Unit) { built += table }
                    Text("${table.name} page")
                }
            }
        }

        screen.swipeToNextTab()
        screen.swipeToPreviousTab()
        tab(Table.AFTER_DOUBLE_SOFT).performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals(Table.entries, built.sorted())
    }

    @Test
    fun theOpenPageShowsAFrameBeforeThoseBesideItAlsoWhenTheTabsChange() {
        compose.mainClock.autoAdvance = false
        compose.setContent(pages)

        assertEquals(1, placedPages())

        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        assertEquals(Table.entries.size, placedPages())

        compose.mainClock.autoAdvance = false
        tabs = tabs - Table.SOFT
        compose.mainClock.advanceTimeByFrame()

        assertEquals(1, placedPages())
    }

    @Test
    fun theChosenTabOutlivesRecreation() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent(pages)
        tab(Table.PAIRS).performClick()

        restoration.emulateSavedInstanceStateRestore()

        assertOpen(Table.PAIRS)
    }

    @Test
    fun newTabsKeepTheChosenOneOpenWhereverItNowSits() {
        compose.setContent(pages)
        tab(Table.AFTER_DOUBLE_HARD).performScrollTo().performClick()

        tabs = tabs - Table.SOFT

        assertOpen(Table.AFTER_DOUBLE_HARD)
    }

    @Test
    fun newTabsWithoutTheChosenOneOpenTheFirst() {
        compose.setContent(pages)
        tab(Table.AFTER_DOUBLE_SOFT).performScrollTo().performClick()

        tabs = tabs - Table.AFTER_DOUBLE_SOFT

        assertOpen(Table.HARD)
    }
}
