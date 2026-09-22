package com.aquigs.sp21ace

import android.app.Activity
import android.app.UiModeManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aquigs.sp21ace.data.HandCustomizationStore
import com.aquigs.sp21ace.data.PlayHistoryStore
import com.aquigs.sp21ace.data.PracticeHistoryStore
import com.aquigs.sp21ace.data.SettingsStore
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.data.TableStore
import com.aquigs.sp21ace.domain.dealing.HandPicker
import com.aquigs.sp21ace.domain.dealing.record
import com.aquigs.sp21ace.domain.game.Shoe
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.playedHandsSince
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.ui.AppShell
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import com.aquigs.sp21ace.ui.theme.isDark
import com.aquigs.sp21ace.ui.trainer.AnswerSounds
import com.aquigs.sp21ace.ui.trainer.rememberAnswerSounds
import java.time.Instant
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val settingsStore = SettingsStore(this)
        showInWindow(settingsStore.load().colorTheme)
        super.onCreate(savedInstanceState)

        val store = TableRulesStore(this)
        val historyStore = PracticeHistoryStore.forApp(this)
        val playHistoryStore = PlayHistoryStore.forApp(this)
        val handsStore = HandCustomizationStore(this)
        val tableStore = TableStore(this)

        setContent { Sp21AceApp(store, historyStore, playHistoryStore, handsStore, settingsStore, tableStore) }
    }
}

/** The composition root. Tests pass their own stores, [deal], [sounds] and the [random] that shuffles the table's shoe. */
@Composable
internal fun Sp21AceApp(
    store: TableRulesStore,
    historyStore: PracticeHistoryStore,
    playHistoryStore: PlayHistoryStore,
    handsStore: HandCustomizationStore,
    settingsStore: SettingsStore,
    tableStore: TableStore,
    deal: (picker: HandPicker, history: List<PracticeAnswer>) -> TrainerHand = { picker, history -> picker.pick(history) },
    sounds: AnswerSounds? = null,
    random: Random = Random.Default,
) {
    val activity = LocalActivity.current
    // Saved as they change, so a recreated activity loads them again rather than keeping a copy of its own
    var rules by remember { mutableStateOf(store.load()) }
    var customization by remember { mutableStateOf(handsStore.load()) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    val answerSounds = sounds ?: rememberAnswerSounds(settings.soundEffects)
    val history by historyStore.history.collectAsState()
    val playHistory by playHistoryStore.history.collectAsState()
    // Each deal reads the rules, the customization and the history, so a change applies from the next hand while the one on the table stays
    val picker = remember(rules.ruleSet, customization) { HandPicker(rules.ruleSet, customization) }
    var trainer by rememberSaveable { mutableStateOf(TrainerState(deal(picker, history.orEmpty()))) }
    var table by rememberSaveable { mutableStateOf(Table(tableStore.loadChips(), Shoe.shuffled(random))) }

    fun seat(next: Table) {
        if (next.chips != table.chips) tableStore.saveChips(next.chips)
        playHistoryStore.append(next.playedHandsSince(table, Instant.now()))
        table = next
    }
    val dark = settings.colorTheme.isDark()

    DisposableEffect(activity, dark) {
        // The app bar is dark in both themes and the drawer stops below the status bar, so the status bar icons stay light.
        // Behind 3-button navigation the system draws its own scrim, so neither scrim given here is ever used.
        (activity as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
        )
        onDispose {}
    }

    Sp21AceTheme(settings.colorTheme) {
        AppShell(
            trainer = trainer,
            table = table,
            rules = rules,
            customization = customization,
            settings = settings,
            history = history.orEmpty(),
            playHistory = playHistory.orEmpty(),
            onAnswer = { asked, move ->
                // The store's history, since the collected one can trail a quick second answer
                trainer.record(asked, move, rules.ruleSet, historyStore.history.value.orEmpty(), Instant.now()) { deal(picker, it) }?.let { (next, answer) ->
                    trainer = next
                    historyStore.append(answer)
                    if (settings.soundEffects) answerSounds?.play(answer.isCorrect)
                }
            },
            onTableUpdate = { change -> change(table)?.let(::seat) },
            // A round keeps the rules it was dealt under, so a change applies from the next deal
            onDeal = { table.deal(rules.ruleSet, random, rules.insurance)?.let(::seat) },
            onRulesChange = {
                rules = it
                store.save(it)
            },
            onCustomizationChange = {
                customization = it
                handsStore.save(it)
            },
            onSettingsChange = {
                if (it.colorTheme != settings.colorTheme) activity?.showInWindow(it.colorTheme)
                settings = it
                settingsStore.save(it)
            },
            onClearHistory = {
                historyStore.clear()
                // The meter's streak lives in the trainer, not in the history, so the clear has to reach it too
                trainer = trainer.copy(streak = 0)
            },
            onClearPlayHistory = { playHistoryStore.clear() },
        )
    }
}

// The window draws before Compose does, on a cold start and on every recreation, so the system has to hear of the choice too.
// From Android 12 it keeps a night mode for the app, which styles the launch window as well and recreates the activity when it
// changes. Android 11 keeps none, so there only the window of an activity created from here on can follow.
private fun Activity.showInWindow(colorTheme: ColorTheme) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(UiModeManager::class.java).setApplicationNightMode(
            when (colorTheme) {
                ColorTheme.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
                ColorTheme.LIGHT -> UiModeManager.MODE_NIGHT_NO
                ColorTheme.DARK -> UiModeManager.MODE_NIGHT_YES
            },
        )
    } else {
        setTheme(
            when (colorTheme) {
                ColorTheme.SYSTEM -> R.style.Theme_Sp21Ace
                ColorTheme.LIGHT -> R.style.Theme_Sp21Ace_Light
                ColorTheme.DARK -> R.style.Theme_Sp21Ace_Dark
            },
        )
    }
}
