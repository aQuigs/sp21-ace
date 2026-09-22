package com.aquigs.sp21ace.ui.rules

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.strategy.DECKS
import com.aquigs.sp21ace.domain.strategy.PENETRATIONS
import com.aquigs.sp21ace.domain.strategy.TableRules
import com.aquigs.sp21ace.ui.components.ChoicePage
import com.aquigs.sp21ace.ui.components.ChoiceRow
import com.aquigs.sp21ace.ui.components.SettingsIntro
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.components.SliderRow
import com.aquigs.sp21ace.ui.components.SwitchRow

/**
 * Offers the rules that choose between the published charts, because a rule without a chart would leave nothing to grade against,
 * and Blackjack Ace's deck penetration and insurance, which change no chart, and whether split hands earn the Bonus 21 payouts.
 */
@Composable
fun TableRulesScreen(
    rules: TableRules,
    onChange: (TableRules) -> Unit,
    onOpenSoft17: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsPage(title = stringResource(R.string.table_rules), onBack = onBack, modifier = modifier) {
        SettingsIntro(stringResource(R.string.table_rules_intro))
        SliderRow(
            title = stringResource(R.string.deck_penetration),
            label = stringResource(R.string.deck_penetration_value, rules.penetration, DECKS * rules.penetration / 100f),
            value = rules.penetration,
            range = PENETRATIONS,
            onValueChange = { onChange(rules.copy(penetration = it)) },
        )
        ChoiceRow(
            title = stringResource(R.string.soft_17),
            value = stringResource(soft17Choice(rules.dealerHitsSoft17)),
            onClick = onOpenSoft17,
        )
        if (rules.offersRedoubling) {
            SwitchRow(
                title = stringResource(R.string.redoubling),
                summary = stringResource(R.string.redoubling_summary),
                checked = rules.redoubling,
                onCheckedChange = { onChange(rules.copy(redoubling = it)) },
            )
        }
        SwitchRow(
            title = stringResource(R.string.split_bonuses),
            summary = stringResource(if (rules.splitBonuses) R.string.split_bonuses_paid else R.string.split_bonuses_not_paid),
            checked = rules.splitBonuses,
            onCheckedChange = { onChange(rules.copy(splitBonuses = it)) },
        )
        SwitchRow(
            title = stringResource(R.string.insurance),
            summary = stringResource(if (rules.insurance) R.string.insurance_offered else R.string.insurance_not_offered),
            checked = rules.insurance,
            onCheckedChange = { onChange(rules.copy(insurance = it)) },
        )
    }
}

@Composable
fun Soft17Screen(rules: TableRules, onChange: (TableRules) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.soft_17),
        // Blackjack Ace's order
        choices = listOf(true, false).map { it to stringResource(soft17Choice(it)) },
        selected = rules.dealerHitsSoft17,
        onSelect = { onChange(rules.copy(dealerHitsSoft17 = it)) },
        onBack = onBack,
        modifier = modifier,
    )
}

private fun soft17Choice(dealerHits: Boolean) = if (dealerHits) R.string.dealer_hits else R.string.dealer_stands
