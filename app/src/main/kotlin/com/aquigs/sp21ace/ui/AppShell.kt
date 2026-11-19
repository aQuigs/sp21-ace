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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.ui.accuracy.AccuracyScreen
import com.aquigs.sp21ace.ui.chart.StrategyChartScreen
import com.aquigs.sp21ace.ui.components.SectionHeading
import com.aquigs.sp21ace.ui.flashcards.IndexFlashcardsActions
import com.aquigs.sp21ace.ui.flashcards.IndexFlashcardsScreen
import com.aquigs.sp21ace.ui.flashcards.IndexFlashcardsState
import com.aquigs.sp21ace.ui.hands.CustomizeHandsScreen
import com.aquigs.sp21ace.ui.hands.HandsDealtScreen
import com.aquigs.sp21ace.ui.indices.AddIndexScreen
import com.aquigs.sp21ace.ui.indices.CardsShownScreen
import com.aquigs.sp21ace.ui.indices.CountRoundingScreen
import com.aquigs.sp21ace.ui.indices.DeckEstimateScreen
import com.aquigs.sp21ace.ui.indices.EditIndexScreen
import com.aquigs.sp21ace.ui.indices.IndexPresetsScreen
import com.aquigs.sp21ace.ui.indices.IndicesScreen
import com.aquigs.sp21ace.ui.play.PlayScreen
import com.aquigs.sp21ace.ui.rules.Soft17Screen
import com.aquigs.sp21ace.ui.rules.TableRulesScreen
import com.aquigs.sp21ace.ui.settings.ButtonLocationScreen
import com.aquigs.sp21ace.ui.settings.ColorThemeScreen
import com.aquigs.sp21ace.ui.settings.SettingsScreen
import com.aquigs.sp21ace.ui.statistics.PlayStatisticsScreen
import com.aquigs.sp21ace.ui.trainer.StrategyTrainerScreen
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Root screens carry the menu. Every other destination opens over one as a sub-page with a back arrow. */
enum class Destination(@StringRes val title: Int, val isRoot: Boolean) {
    StrategyTrainer(R.string.strategy_trainer, isRoot = true),
    IndexFlashcards(R.string.index_flashcards, isRoot = true),
    Play(R.string.play_spanish_21, isRoot = true),
    StrategyChart(R.string.strategy_chart, isRoot = false),
    TableRules(R.string.table_rules, isRoot = false),
    Soft17(R.string.soft_17, isRoot = false),
    CustomizeHands(R.string.customize_hands, isRoot = false),
    HandsDealt(R.string.hands_dealt, isRoot = false),
    Accuracy(R.string.accuracy, isRoot = false),
    Indices(R.string.indices, isRoot = false),
    IndexPresets(R.string.index_presets, isRoot = false),
    DeckEstimate(R.string.deck_estimate, isRoot = false),
    CountRounding(R.string.true_count_rounding, isRoot = false),
    CardsShown(R.string.cards_shown, isRoot = false),
    AddIndex(R.string.add_index, isRoot = false),
    EditIndex(R.string.edit_index, isRoot = false),
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

private val CARD_COUNTING_ITEMS = listOf(Destination.IndexFlashcards to R.drawable.ic_home, Destination.Indices to R.drawable.ic_customize_hands)

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
    flashcards: IndexFlashcardsState,
    flashcardActions: IndexFlashcardsActions,
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
    // What Edit index edits, as a destination carries nothing of its own
    var editingIndex by rememberSaveable { mutableStateOf<Deviation?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Anywhere short of fully closed, so a card it covers for any time is untimed, even under a drag let spring back, which
    // never changes the drawer's target. Closed lies the drawer's width off screen, and open at 0.
    val closedOffset = with(LocalDensity.current) { DrawerWidth.toPx() }
    val drawerShowing by remember(closedOffset) {
        derivedStateOf { drawerState.currentOffset.let { !it.isNaN() && abs(it) < closedOffset - 1 } }
    }

    // A page already open is returned to rather than stacked again, as a double tap would
    fun open(page: Destination) {
        backStack = backStack.takeWhile { it != page } + page
    }

    // Only from the page on top, and never from the root screen: a second back before the redraw, such as a double tap on the
    // arrow or on a choice that goes back, would otherwise pop the page under it too
    fun backFrom(page: Destination) {
        if (backStack.size > 1 && backStack.last() == page) backStack = backStack.dropLast(1)
    }
    val back = { backFrom(destination) }

    // Back closes the drawer, then retraces the sub-pages. A root screen has nothing behind it, so there Back opens the
    // drawer instead of leaving the app. targetValue, not isOpen, so a Back pressed mid-animation reverses it.
    BackHandler {
        when {
            drawerState.targetValue == DrawerValue.Open -> scope.launch { drawerState.close() }
            backStack.size > 1 -> backFrom(backStack.last())
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
            Destination.IndexFlashcards -> IndexFlashcardsScreen(
                state = flashcards,
                settings = settings,
                covered = drawerShowing,
                actions = flashcardActions,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onOpenIndices = { open(Destination.Indices) },
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
                onBack = back,
            )
            Destination.TableRules -> TableRulesScreen(rules, onRulesChange, onOpenSoft17 = { open(Destination.Soft17) }, onBack = back)
            Destination.Soft17 -> Soft17Screen(rules, onRulesChange, onBack = back)
            Destination.CustomizeHands -> CustomizeHandsScreen(
                customization,
                history,
                onCustomizationChange,
                onOpenHandsDealt = { open(Destination.HandsDealt) },
                onBack = back,
            )
            Destination.HandsDealt -> HandsDealtScreen(customization, onCustomizationChange, onBack = back)
            Destination.Accuracy -> AccuracyScreen(history, rules.ruleSet, onBack = back)
            Destination.Indices -> IndicesScreen(
                presets = flashcards.presets,
                options = flashcards.options,
                onPresetsChange = flashcardActions::changePresets,
                onOptionsChange = flashcardActions::changeOptions,
                onOpenPresets = { open(Destination.IndexPresets) },
                onOpenDeckEstimate = { open(Destination.DeckEstimate) },
                onOpenRounding = { open(Destination.CountRounding) },
                onOpenCardsShown = { open(Destination.CardsShown) },
                onAddIndex = { open(Destination.AddIndex) },
                onEditIndex = {
                    editingIndex = it
                    open(Destination.EditIndex)
                },
                onBack = back,
            )
            Destination.IndexPresets -> IndexPresetsScreen(flashcards.presets, flashcardActions::changePresets, onBack = back)
            Destination.DeckEstimate -> DeckEstimateScreen(flashcards.presets, flashcardActions::changePresets, onBack = back)
            Destination.CountRounding -> CountRoundingScreen(flashcards.presets, flashcardActions::changePresets, onBack = back)
            Destination.CardsShown -> CardsShownScreen(flashcards.options, flashcardActions::changeOptions, onBack = back)
            Destination.AddIndex -> AddIndexScreen(flashcards.presets, flashcardActions::changePresets, onBack = back)
            Destination.EditIndex -> editingIndex?.let { EditIndexScreen(flashcards.presets, it, flashcardActions::changePresets, onBack = back) }
            Destination.PlayStatistics -> PlayStatisticsScreen(playHistory, onBack = back)
            Destination.Settings -> SettingsScreen(
                settings,
                onSettingsChange,
                onOpenColorTheme = { open(Destination.ColorTheme) },
                onOpenButtonLocation = { open(Destination.ButtonLocation) },
                onClearHistory = onClearHistory,
                onClearPlayHistory = onClearPlayHistory,
                onBack = back,
            )
            Destination.ColorTheme -> ColorThemeScreen(settings, onSettingsChange, onBack = back)
            Destination.ButtonLocation -> ButtonLocationScreen(settings, onSettingsChange, onBack = back)
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
            // The first section starts further down, clear of the sheet's top edge, and each after it below a divider
            DrawerSection(R.string.basic_strategy, BASIC_STRATEGY_ITEMS, selected, onSelect, top = 18.dp)
            DrawerDivider()
            DrawerSection(R.string.card_counting, CARD_COUNTING_ITEMS, selected, onSelect)
            DrawerDivider()
            DrawerSection(R.string.play, PLAY_ITEMS, selected, onSelect)

            // As in Blackjack Ace, Settings belongs to no section, so it sits below them all, inset as far as the header
            DrawerDivider()
            DrawerItem(Destination.Settings, R.drawable.ic_settings, selected, onSelect)
        }
    }
}

@Composable
private fun DrawerSection(
    @StringRes title: Int,
    items: List<Pair<Destination, Int>>,
    selected: Destination,
    onSelect: (Destination) -> Unit,
    top: Dp = 10.dp,
) {
    // The extra 16dp is the item's own start padding, so the header lines up with the icons
    SectionHeading(
        text = stringResource(title),
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(start = 16.dp, top = top, bottom = 18.dp),
    )
    items.forEach { (destination, icon) -> DrawerItem(destination, icon, selected, onSelect) }
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
