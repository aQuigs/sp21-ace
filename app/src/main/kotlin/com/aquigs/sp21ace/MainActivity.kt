package com.aquigs.sp21ace

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aquigs.sp21ace.data.TableRulesStore
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
import com.aquigs.sp21ace.domain.trainer.dealTrainerHand
import com.aquigs.sp21ace.ui.AppShell
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app bar is dark in both themes and the drawer stops below the status bar, so the status bar icons stay light
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        val store = TableRulesStore(this)

        setContent {
            var trainer by rememberSaveable { mutableStateOf(TrainerState(dealTrainerHand())) }
            // Saved as they change, so a recreated activity loads them again rather than keeping a copy of its own
            var rules by remember { mutableStateOf(store.load()) }

            Sp21AceTheme {
                AppShell(
                    trainer = trainer,
                    rules = rules,
                    onAnswer = { asked, move -> trainer = trainer.answer(asked, move, StrategyCharts.forRules(rules.ruleSet)) },
                    onRulesChange = {
                        rules = it
                        store.save(it)
                    },
                )
            }
        }
    }
}
