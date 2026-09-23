package com.aquigs.sp21ace.ui.statistics

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.ui.cardTexts
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
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

    private fun chart(hands: Int, end: String, lowest: String, highest: String) = compose.onNode(
        hasContentDescription(compose.activity.resources.getQuantityString(R.plurals.profit_chart_description, hands, hands, end, lowest, highest)),
    )

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

    private fun bankrollCard() = compose.cardTexts(string(R.string.bankroll))

    private fun strategyCard() = compose.cardTexts(string(R.string.strategy), string(R.string.hands_played))

    @Test
    fun todayCountsTheHandsWonPushedAndLostWhatTheyCameToAndHowTheyWerePlayed() {
        show(history)

        hands(1, "25%", 1, "25%", 2, "50%").assertExists()
        assertEquals(compose.activity.bankrollCardTexts("−50", "25", "75"), bankrollCard())
        // From 0 up to 25, level for the push, then down to −50
        chart(4, "−50", "−50", "25").assertExists()
        assertEquals(compose.activity.strategyCardTexts(4, 1 to "25%", 1 to "25%", 1 to "25%", 1 to "25%"), strategyCard())
    }

    @Test
    fun allTimeCountsEveryHand() {
        show(history)

        compose.onNodeWithText(string(R.string.all_time)).performClick()

        hands(2, "40%", 1, "20%", 2, "40%").assertExists()
        assertEquals(compose.activity.bankrollCardTexts("50", "125", "75"), bankrollCard())
    }

    @Test
    fun withNothingPlayedTheSharesReadAsNoData() {
        show(emptyList())

        hands(0, "--", 0, "--", 0, "--").assertExists()
        assertEquals(compose.activity.bankrollCardTexts("0", "0", "0"), bankrollCard())
        chart(0, "0", "0", "0").assertExists()
        assertEquals(compose.activity.strategyCardTexts(0, 0 to "--", 0 to "--", 0 to "--", 0 to "--"), strategyCard())
    }
}
