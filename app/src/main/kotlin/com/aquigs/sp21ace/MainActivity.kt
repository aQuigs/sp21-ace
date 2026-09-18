package com.aquigs.sp21ace

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aquigs.sp21ace.data.PracticeHistoryStore
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
import com.aquigs.sp21ace.domain.trainer.dealTrainerHand
import com.aquigs.sp21ace.ui.AppShell
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import java.time.Clock

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app bar is dark in both themes and the drawer stops below the status bar, so the status bar icons stay light
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        val store = TableRulesStore(this)
        val historyStore = PracticeHistoryStore(this)

        setContent { Sp21AceTheme { Sp21AceApp(store, historyStore) } }
    }
}

/**
 * Holds the trainer, the table rules and the practice history, saves the rules and each answer as they change, and grades
 * by the rules. Tests pass their own stores, [deal] and [clock].
 */
@Composable
internal fun Sp21AceApp(
    store: TableRulesStore,
    historyStore: PracticeHistoryStore,
    deal: () -> TrainerHand = ::dealTrainerHand,
    clock: Clock = Clock.systemDefaultZone(),
) {
    var trainer by rememberSaveable { mutableStateOf(TrainerState(deal())) }
    // Saved as they change, so a recreated activity loads them again rather than keeping a copy of its own
    var rules by remember { mutableStateOf(store.load()) }
    var history by remember { mutableStateOf(historyStore.load()) }

    AppShell(
        trainer = trainer,
        rules = rules,
        history = history,
        clock = clock,
        onAnswer = { asked, move ->
            trainer.answer(asked, move, StrategyCharts.forRules(rules.ruleSet), deal)?.let { (next, grade) ->
                trainer = next
                val answer = PracticeAnswer(clock.instant(), rules.ruleSet, grade)
                history = history + answer
                historyStore.append(answer)
            }
        },
        onRulesChange = {
            rules = it
            store.save(it)
        },
    )
}
