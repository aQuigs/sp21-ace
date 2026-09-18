package com.aquigs.sp21ace

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aquigs.sp21ace.data.HandCustomizationStore
import com.aquigs.sp21ace.data.PracticeHistoryStore
import com.aquigs.sp21ace.data.SettingsStore
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.dealing.HandPicker
import com.aquigs.sp21ace.domain.dealing.record
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.ui.AppShell
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import java.time.Instant

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app bar is dark in both themes and the drawer stops below the status bar, so the status bar icons stay light
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        val store = TableRulesStore(this)
        val historyStore = PracticeHistoryStore.forApp(this)
        val handsStore = HandCustomizationStore(this)
        val settingsStore = SettingsStore(this)

        setContent { Sp21AceApp(store, historyStore, handsStore, settingsStore) }
    }
}

/**
 * Holds the trainer, the table rules, how hands are dealt and the settings, saves each as it changes, themes the app by the
 * settings, deals through the rules and the customization, grades by the rules and records each grade. Tests pass their own
 * stores and [deal].
 */
@Composable
internal fun Sp21AceApp(
    store: TableRulesStore,
    historyStore: PracticeHistoryStore,
    handsStore: HandCustomizationStore,
    settingsStore: SettingsStore,
    deal: (picker: HandPicker, history: List<PracticeAnswer>) -> TrainerHand = { picker, history -> picker.pick(history) },
) {
    // Saved as they change, so a recreated activity loads them again rather than keeping a copy of its own
    var rules by remember { mutableStateOf(store.load()) }
    var customization by remember { mutableStateOf(handsStore.load()) }
    var settings by remember { mutableStateOf(settingsStore.load()) }
    val history by historyStore.history.collectAsState()
    // A change of settings applies from the next hand, while the one on the table stays
    val picker = remember(rules.ruleSet, customization) { HandPicker(rules.ruleSet, customization) }
    var trainer by rememberSaveable { mutableStateOf(TrainerState(deal(picker, history.orEmpty()))) }

    Sp21AceTheme(settings.colorTheme) {
        AppShell(
            trainer = trainer,
            rules = rules,
            customization = customization,
            settings = settings,
            history = history.orEmpty(),
            onAnswer = { asked, move ->
                // The store's history, since the collected one can trail a quick second answer
                trainer.record(asked, move, rules.ruleSet, historyStore.history.value.orEmpty(), Instant.now()) { deal(picker, it) }?.let { (next, answer) ->
                    trainer = next
                    historyStore.append(answer)
                }
            },
            onRulesChange = {
                rules = it
                store.save(it)
            },
            onCustomizationChange = {
                customization = it
                handsStore.save(it)
            },
            onSettingsChange = {
                settings = it
                settingsStore.save(it)
            },
            onClearHistory = historyStore::clear,
        )
    }
}
