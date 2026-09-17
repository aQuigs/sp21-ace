package com.aquigs.sp21ace

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
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
}
