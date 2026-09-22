package com.aquigs.sp21ace.ui.rules

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.PENETRATIONS
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.ui.theme.Sp21AceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableRulesScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var rules by mutableStateOf(TableRules())

    private fun string(id: Int) = compose.activity.getString(id)

    private fun showRules(onOpenSoft17: () -> Unit = {}) {
        compose.setContent { Sp21AceTheme { TableRulesScreen(rules, onChange = { rules = it }, onOpenSoft17 = onOpenSoft17, onBack = {}) } }
    }

    @Test
    fun theRedoublingRowAppearsOnlyWhileTheDealerHits() {
        showRules()

        compose.onNodeWithText(string(R.string.redoubling)).assertDoesNotExist()

        rules = TableRules(dealerHitsSoft17 = true)

        compose.onNodeWithText(string(R.string.redoubling)).assertIsDisplayed()

        rules = TableRules(dealerHitsSoft17 = false, redoubling = true)

        compose.onNodeWithText(string(R.string.redoubling)).assertDoesNotExist()
    }

    @Test
    fun theSoft17RowShowsTheDealerStandingAndOpensItsPage() {
        var opened = 0
        showRules(onOpenSoft17 = { opened++ })

        compose.onNode(hasText(string(R.string.soft_17)) and hasText(string(R.string.dealer_stands))).performClick()

        assertEquals(1, opened)
    }

    @Test
    fun theRedoublingSwitchTurnsRedoublingOn() {
        rules = TableRules(dealerHitsSoft17 = true)
        showRules()

        compose.onNode(hasText(string(R.string.redoubling)) and isToggleable()).assertIsOff().performClick().assertIsOn()

        assertEquals(TableRules(dealerHitsSoft17 = true, redoubling = true), rules)
    }

    @Test
    fun theInsuranceSwitchSaysWhetherTheTableOffersIt() {
        showRules()

        compose.onNodeWithText(string(R.string.insurance_not_offered)).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.insurance)) and isToggleable()).assertIsOff().performClick().assertIsOn()

        compose.onNodeWithText(string(R.string.insurance_offered)).assertIsDisplayed()
        assertEquals(TableRules(insurance = true), rules)
    }

    @Test
    fun theSplitBonusesSwitchSaysWhetherSplitHandsEarnThem() {
        showRules()

        compose.onNodeWithText(string(R.string.split_bonuses_paid)).assertIsDisplayed()
        compose.onNode(hasText(string(R.string.split_bonuses)) and isToggleable()).assertIsOn().performClick().assertIsOff()

        compose.onNodeWithText(string(R.string.split_bonuses_not_paid)).assertIsDisplayed()
        assertEquals(TableRules(splitBonuses = false), rules)
    }

    @Test
    fun theDeckPenetrationSliderStartsAtThreeQuartersAndItsLabelFollowsItToEitherEnd() {
        showRules()

        compose.onNodeWithText("75% (4.50 decks)").assertIsDisplayed()

        for ((penetration, label) in listOf(PENETRATIONS.first to "10% (0.60 decks)", PENETRATIONS.last to "85% (5.10 decks)")) {
            compose.onNodeWithContentDescription(string(R.string.deck_penetration)).performSemanticsAction(SemanticsActions.SetProgress) { it(penetration.toFloat()) }

            assertEquals(TableRules(penetration = penetration), rules)
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun theSoft17PageMarksTheCurrentChoiceAndChoosingDealerHitsKeepsRedoublingAndGoesBack() {
        rules = TableRules(redoubling = true)
        var backs = 0
        compose.setContent { Sp21AceTheme { Soft17Screen(rules, onChange = { rules = it }, onBack = { backs++ }) } }

        compose.onNodeWithText(string(R.string.dealer_stands)).assertIsSelected()

        compose.onNodeWithText(string(R.string.dealer_hits)).assertIsNotSelected().performClick().assertIsSelected()

        compose.onNodeWithText(string(R.string.dealer_stands)).assertIsNotSelected()
        assertEquals(TableRules(dealerHitsSoft17 = true, redoubling = true), rules)
        assertEquals(1, backs)
    }
}
