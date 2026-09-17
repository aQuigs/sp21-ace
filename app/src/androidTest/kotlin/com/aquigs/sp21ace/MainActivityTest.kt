package com.aquigs.sp21ace

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensOnTheStrategyTrainerWithAHandDealt() {
        // The closed drawer also carries the Strategy Trainer label, so match only the app bar's heading
        val title = hasText(compose.activity.getString(R.string.strategy_trainer)) and isHeading()

        compose.onNode(title).assertIsDisplayed()
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.face_down_card)).assertIsDisplayed()
    }

    @Test
    fun keepsTheHandTheVerdictAndThePreviousHandWhenRecreated() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.move_hit)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.action)).assertIsDisplayed()
        val answered = everythingOnScreen()

        // What a theme or font size change does to the activity
        compose.activityRule.scenario.recreate()

        assertEquals(answered, everythingOnScreen())
    }

    @Test
    fun keepsTheOpenChartAndItsTabWhenRecreated() {
        compose.onNodeWithContentDescription(compose.activity.getString(R.string.open_strategy_chart)).performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.table_pairs)).performClick()

        compose.activityRule.scenario.recreate()

        compose.onNode(hasText(compose.activity.getString(R.string.strategy_chart)) and isHeading()).assertIsDisplayed()
        compose.onNodeWithText(compose.activity.getString(R.string.table_pairs)).assertIsSelected()
    }

    // Hands are dealt at random, so compare every card, bar and recap word rather than expected values
    private fun everythingOnScreen(): List<String> =
        compose.onAllNodes(SemanticsMatcher("any node") { true }, useUnmergedTree = true).fetchSemanticsNodes().flatMap { node ->
            node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() } +
                node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.map { it.text }
        }
}
