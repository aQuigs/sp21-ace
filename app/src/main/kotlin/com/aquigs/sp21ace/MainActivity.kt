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
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.dealing.HandPicker
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
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

        setContent { Sp21AceTheme { Sp21AceApp(store, historyStore, handsStore) } }
    }
}

/**
 * Holds the trainer, the table rules and how hands are dealt, saves the settings as they change, deals through them, grades
 * by the rules and records each grade. Tests pass their own stores and [deal].
 */
@Composable
internal fun Sp21AceApp(
    store: TableRulesStore,
    historyStore: PracticeHistoryStore,
    handsStore: HandCustomizationStore,
    deal: (HandPicker) -> TrainerHand = { it.pick() },
) {
    // Saved as they change, so a recreated activity loads them again rather than keeping a copy of its own
    var rules by remember { mutableStateOf(store.load()) }
    var customization by remember { mutableStateOf(handsStore.load()) }
    val history by historyStore.history.collectAsState()
    // Reads the settings as it deals, so a change applies from the next hand while the one on the table stays
    val dealNext = { deal(HandPicker(rules.ruleSet, customization, history.orEmpty())) }
    var trainer by rememberSaveable { mutableStateOf(TrainerState(dealNext())) }

    AppShell(
        trainer = trainer,
        rules = rules,
        customization = customization,
        history = history.orEmpty(),
        onAnswer = { asked, move ->
            trainer.answer(asked, move, StrategyCharts.forRules(rules.ruleSet), dealNext)?.let { (next, grade) ->
                trainer = next
                historyStore.append(PracticeAnswer(Instant.now(), rules.ruleSet, grade))
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
    )
}
