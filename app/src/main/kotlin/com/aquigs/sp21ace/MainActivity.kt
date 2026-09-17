package com.aquigs.sp21ace

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.trainer.TrainerState
import com.aquigs.sp21ace.domain.trainer.answer
import com.aquigs.sp21ace.domain.trainer.dealTrainerHand
import com.aquigs.sp21ace.ui.AppShell
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import com.aquigs.sp21ace.ui.trainer.trainerStateSaver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app bar is dark in both themes and the drawer stops below the status bar, so the status bar icons stay light
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))

        // Dealer stands on soft 17 until the table rules let the player choose
        val chart = StrategyCharts.forRules(RuleSet.S17)
        val trainerSaver = trainerStateSaver(chart)

        setContent {
            var trainer by rememberSaveable(stateSaver = trainerSaver) { mutableStateOf(TrainerState(dealTrainerHand())) }

            Sp21AceTheme {
                AppShell(trainer = trainer, onAnswer = { asked, move -> trainer = trainer.answer(asked, move, chart) })
            }
        }
    }
}
