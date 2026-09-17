package com.aquigs.sp21ace.ui.rules

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.rules.TableRules
import com.aquigs.sp21ace.ui.components.ChoicePage
import com.aquigs.sp21ace.ui.components.ChoiceRow
import com.aquigs.sp21ace.ui.components.SubPage
import com.aquigs.sp21ace.ui.components.SwitchRow

/** Offers only the rules that choose between the published charts, because a rule without a chart would leave nothing to grade against. */
@Composable
fun TableRulesScreen(
    rules: TableRules,
    onChange: (TableRules) -> Unit,
    onOpenSoft17: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SubPage(title = stringResource(R.string.table_rules), onBack = onBack, modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = {
                    Text(
                        text = stringResource(R.string.table_rules_intro),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                leadingContent = { Icon(painterResource(R.drawable.ic_info), contentDescription = null) },
            )
            ChoiceRow(
                title = stringResource(R.string.soft_17),
                value = stringResource(soft17Choice(rules.dealerHitsSoft17)),
                onClick = onOpenSoft17,
            )
            // Casinos only offer redoubling where the dealer hits soft 17
            if (rules.dealerHitsSoft17) {
                SwitchRow(
                    title = stringResource(R.string.redoubling),
                    summary = stringResource(R.string.redoubling_summary),
                    checked = rules.redoubling,
                    onCheckedChange = { onChange(rules.copy(redoubling = it)) },
                )
            }
        }
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
