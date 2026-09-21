package com.aquigs.sp21ace.ui.statistics

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Duration
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class PlayStatisticsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val now = Instant.parse("2026-09-20T12:00:00Z")

    private fun string(id: Int, vararg args: Any) = compose.activity.getString(id, *args)

    private fun hand(net: Long, grade: StrategyGrade = StrategyGrade.CORRECT, daysAgo: Long = 0) = PlayedHand(
        now.minus(Duration.ofDays(daysAgo)),
        RuleSet.S17,
        when {
            net > 0 -> Outcome.WIN
            net < 0 -> Outcome.LOSE
            else -> Outcome.PUSH
        },
        net,
        grade,
    )

    // Today: a win of 25, a push, a loss of 50 and one of 25. Three days ago: a win of 100
    private val history = listOf(
        hand(2_500),
        hand(0, StrategyGrade.CORRECT_WITH_HINTS),
        hand(-5_000, StrategyGrade.INCORRECT),
        hand(-2_500, StrategyGrade.NO_ACTION_REQUIRED),
        hand(10_000, daysAgo = 3),
    )

    private fun show(history: List<PlayedHand>) {
        compose.setContent { Sp21AceTheme(ColorTheme.LIGHT) { PlayStatisticsScreen(history, onBack = {}, now = { now }) } }
    }

    private fun hands(won: Int, wonShare: String, pushed: Int, pushedShare: String, lost: Int, lostShare: String) =
        compose.onNode(hasContentDescription(string(R.string.hands_description, won, wonShare, pushed, pushedShare, lost, lostShare)))

    @Test
    fun todayCountsTheHandsWonPushedAndLostWhatTheyCameToAndHowTheyWerePlayed() {
        show(history)

        hands(1, "25%", 1, "25%", 2, "50%").assertExists()
        compose.onNode(hasText(string(R.string.profit_loss)) and hasText("−50") and hasText("25") and hasText("75")).assertExists()
        // From 0 up to 25, level for the push, then down to −50
        compose.onNode(hasContentDescription(string(R.string.profit_chart_description, 4, "−50", "−50", "25"))).assertExists()
        compose.onNode(hasText(string(R.string.hands_played)) and hasText("4") and hasText("25%") and hasText(string(R.string.no_action_required)))
            .assertExists()
    }

    @Test
    fun allTimeCountsEveryHand() {
        show(history)

        compose.onNodeWithText(string(R.string.all_time)).performClick()

        hands(2, "40%", 1, "20%", 2, "40%").assertExists()
        compose.onNode(hasText(string(R.string.profit_loss)) and hasText("50") and hasText("125") and hasText("75")).assertExists()
    }

    @Test
    fun withNothingPlayedTheSharesReadAsNoData() {
        show(emptyList())

        hands(0, "--", 0, "--", 0, "--").assertExists()
        compose.onNode(hasText(string(R.string.profit_loss)) and hasText("0")).assertExists()
        compose.onNode(hasContentDescription(string(R.string.profit_chart_description, 0, "0", "0", "0"))).assertExists()
    }
}
