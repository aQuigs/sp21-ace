package com.aquigs.sp21ace.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.ui.accuracy.AccuracyScreen
import com.aquigs.sp21ace.ui.chart.StrategyChartScreen
import com.aquigs.sp21ace.ui.components.SectionHeading
import com.aquigs.sp21ace.ui.hands.CustomizeHandsScreen
import com.aquigs.sp21ace.ui.hands.HandsDealtScreen
import com.aquigs.sp21ace.ui.play.PlayScreen
import com.aquigs.sp21ace.ui.rules.Soft17Screen
import com.aquigs.sp21ace.ui.rules.TableRulesScreen
import com.aquigs.sp21ace.ui.settings.ButtonLocationScreen
import com.aquigs.sp21ace.ui.settings.ColorThemeScreen
import com.aquigs.sp21ace.ui.settings.SettingsScreen
import com.aquigs.sp21ace.ui.statistics.PlayStatisticsScreen
import com.aquigs.sp21ace.ui.trainer.StrategyTrainerScreen
import kotlinx.coroutines.launch

/** Root screens carry the menu. Every other destination opens over one as a sub-page with a back arrow. */
enum class Destination(@StringRes val title: Int, val isRoot: Boolean) {
    StrategyTrainer(R.string.strategy_trainer, isRoot = true),
    Play(R.string.play_spanish_21, isRoot = true),
    StrategyChart(R.string.strategy_chart, isRoot = false),
    TableRules(R.string.table_rules, isRoot = false),
    Soft17(R.string.soft_17, isRoot = false),
    CustomizeHands(R.string.customize_hands, isRoot = false),
    HandsDealt(R.string.hands_dealt, isRoot = false),
    Accuracy(R.string.accuracy, isRoot = false),
    PlayStatistics(R.string.statistics, isRoot = false),
    Settings(R.string.settings, isRoot = false),
    ColorTheme(R.string.color_theme, isRoot = false),
    ButtonLocation(R.string.button_location, isRoot = false),
}

// In Blackjack Ace's order. The drawer keeps its own list, because not every destination belongs in it, such as a picker
// opened from another page.
private val BASIC_STRATEGY_ITEMS = listOf(
    Destination.StrategyTrainer to R.drawable.ic_home,
    Destination.TableRules to R.drawable.ic_table_rules,
    Destination.StrategyChart to R.drawable.ic_chart,
    Destination.CustomizeHands to R.drawable.ic_customize_hands,
    Destination.Accuracy to R.drawable.ic_accuracy,
)

// Blackjack Ace's Play section also lists its own Table Rules and Strategy Chart, but here the table plays by the trainer's.
private val PLAY_ITEMS = listOf(Destination.Play to R.drawable.ic_home, Destination.PlayStatistics to R.drawable.ic_accuracy)

// Blackjack Ace's drawer leaves about a third of the screen uncovered; Material's 360dp default covers almost all of it.
private val DrawerWidth = 280.dp

@Composable
fun AppShell(
    trainer: TrainerState,
    table: Table,
    rules: TableRules,
    customization: HandCustomization,
    settings: Settings,
    history: List<PracticeAnswer>,
    playHistory: List<PlayedHand>,
    onAnswer: (asked: TrainerHand, move: Move) -> Unit,
    onTableUpdate: ((Table) -> Table?) -> Unit,
    onDeal: () -> Unit,
    onRulesChange: (TableRules) -> Unit,
    onCustomizationChange: (HandCustomization) -> Unit,
    onSettingsChange: (Settings) -> Unit,
    onClearHistory: () -> Unit,
    onClearPlayHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A root screen, then the sub-pages opened over it, so Back retraces the way in
    var backStack by rememberSaveable { mutableStateOf(listOf(Destination.StrategyTrainer)) }
    val destination = backStack.last()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // A page already open is returned to rather than stacked again, as a double tap would
    fun open(page: Destination) {
        backStack = backStack.takeWhile { it != page } + page
    }

    // Two backs before a redraw, such as a double tap on the arrow, would otherwise pop the root screen too
    fun back() {
        if (backStack.size > 1) backStack = backStack.dropLast(1)
    }

    // Back closes the drawer, then retraces the sub-pages. A root screen has nothing behind it, so there Back opens the
    // drawer instead of leaving the app. targetValue, not isOpen, so a Back pressed mid-animation reverses it.
    BackHandler {
        when {
            drawerState.targetValue == DrawerValue.Open -> scope.launch { drawerState.close() }
            backStack.size > 1 -> back()
            else -> scope.launch { drawerState.open() }
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            Drawer(
                selected = destination,
                onSelect = {
                    // Straight over the root, so a second pick before the drawer has closed replaces the first instead of
                    // stacking on it
                    backStack = if (it.isRoot) listOf(it) else listOf(backStack.first(), it)
                    scope.launch { drawerState.close() }
                },
            )
        },
        modifier = modifier,
        drawerState = drawerState,
        // A sub-page has no menu button, so an edge swipe mustn't open the drawer over it either
        gesturesEnabled = destination.isRoot,
    ) {
        when (destination) {
            Destination.StrategyTrainer -> StrategyTrainerScreen(
                state = trainer,
                settings = settings,
                redoubling = rules.ruleSet.redoubling,
                onAnswer = onAnswer,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenChart = { open(Destination.StrategyChart) },
            )
            Destination.Play -> PlayScreen(
                table = table,
                settings = settings,
                onUpdate = onTableUpdate,
                onDeal = onDeal,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenChart = { open(Destination.StrategyChart) },
            )
            // Over the table, the rules the round was dealt under, which a change mid-round leaves in play
            Destination.StrategyChart -> StrategyChartScreen(
                table.round?.ruleSet?.takeIf { backStack.first() == Destination.Play } ?: rules.ruleSet,
                onBack = { back() },
            )
            Destination.TableRules -> TableRulesScreen(rules, onRulesChange, onOpenSoft17 = { open(Destination.Soft17) }, onBack = { back() })
            Destination.Soft17 -> Soft17Screen(rules, onRulesChange, onBack = { back() })
            Destination.CustomizeHands -> CustomizeHandsScreen(
                customization,
                history,
                onCustomizationChange,
                onOpenHandsDealt = { open(Destination.HandsDealt) },
                onBack = { back() },
            )
            Destination.HandsDealt -> HandsDealtScreen(customization, onCustomizationChange, onBack = { back() })
            Destination.Accuracy -> AccuracyScreen(history, rules.ruleSet, onBack = { back() })
            Destination.PlayStatistics -> PlayStatisticsScreen(playHistory, onBack = { back() })
            Destination.Settings -> SettingsScreen(
                settings,
                onSettingsChange,
                onOpenColorTheme = { open(Destination.ColorTheme) },
                onOpenButtonLocation = { open(Destination.ButtonLocation) },
                onClearHistory = onClearHistory,
                onClearPlayHistory = onClearPlayHistory,
                onBack = { back() },
            )
            Destination.ColorTheme -> ColorThemeScreen(settings, onSettingsChange, onBack = { back() })
            Destination.ButtonLocation -> ButtonLocationScreen(settings, onSettingsChange, onBack = { back() })
        }
    }
}

@Composable
private fun Drawer(selected: Destination, onSelect: (Destination) -> Unit) {
    // The overload taking drawerState registers its own back handler, which would compete with AppShell's.
    // Stopping below the status bar keeps the dark app bar behind its light icons, which a light drawer would hide.
    ModalDrawerSheet(modifier = Modifier.width(DrawerWidth).windowInsetsPadding(WindowInsets.statusBars)) {
        // Scrolls, as Blackjack Ace's does, so Settings at the bottom stays in reach on a short screen at a large font size
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            // The extra 16dp is the item's own start padding, so the header lines up with the icons
            SectionHeading(
                text = stringResource(R.string.basic_strategy),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 16.dp, top = 18.dp, bottom = 18.dp),
            )

            BASIC_STRATEGY_ITEMS.forEach { (destination, icon) -> DrawerItem(destination, icon, selected, onSelect) }

            DrawerDivider()
            SectionHeading(
                text = stringResource(R.string.play),
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 16.dp, top = 10.dp, bottom = 18.dp),
            )
            PLAY_ITEMS.forEach { (destination, icon) -> DrawerItem(destination, icon, selected, onSelect) }

            // As in Blackjack Ace, Settings belongs to no section, so it sits below them all, inset as far as the header
            DrawerDivider()
            DrawerItem(Destination.Settings, R.drawable.ic_settings, selected, onSelect)
        }
    }
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
private fun DrawerItem(destination: Destination, @DrawableRes icon: Int, selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationDrawerItem(
        label = { Text(stringResource(destination.title)) },
        selected = destination == selected,
        onClick = { onSelect(destination) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        icon = { Icon(painterResource(icon), contentDescription = null) },
    )
}
