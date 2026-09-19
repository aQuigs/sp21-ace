package com.aquigs.sp21ace.ui.hands

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.dealing.HAND_TYPES
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.dealing.HandsDealt
import com.aquigs.sp21ace.domain.dealing.cardCountTally
import com.aquigs.sp21ace.domain.dealing.multiCardTally
import com.aquigs.sp21ace.domain.dealing.tallyByHandType
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.ui.chart.title
import com.aquigs.sp21ace.ui.components.ChoicePage
import com.aquigs.sp21ace.ui.components.ChoiceRow
import com.aquigs.sp21ace.ui.components.SettingsGroup
import com.aquigs.sp21ace.ui.components.SettingsIntro
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.components.SwitchRow
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.components.percentText

/**
 * How the trainer deals, and which types of hand it deals: switches for hands of 3 or more cards and for card-count hands, and one
 * for every move each kind of hand calls for, each with the accuracy of every answer ever given, as Blackjack Ace counts it
 * whatever the period on Accuracy.
 */
@Composable
fun CustomizeHandsScreen(
    customization: HandCustomization,
    history: List<PracticeAnswer>,
    onChange: (HandCustomization) -> Unit,
    onOpenHandsDealt: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tallies = remember(history) { history.tallyByHandType() }
    val multiCardTally = remember(history) { history.multiCardTally() }
    val cardCountTally = remember(history) { history.cardCountTally() }
    val cardCountTitle = stringResource(R.string.card_count_hands)

    SettingsPage(title = stringResource(R.string.customize_hands), onBack = onBack, modifier = modifier) {
        SettingsIntro(
            stringResource(R.string.hands_dealt_intro, stringResource(HandsDealt.RANDOM.title), stringResource(HandsDealt.PRIORITIZE_WORSE.title), cardCountTitle),
        )
        ChoiceRow(title = stringResource(R.string.hands_dealt), value = stringResource(customization.handsDealt.title), onClick = onOpenHandsDealt)
        HorizontalDivider()
        SettingsIntro(stringResource(R.string.hand_types_intro, cardCountTitle))
        SwitchRow(
            title = stringResource(R.string.multi_card_hands),
            summary = accuracySummary(multiCardTally),
            checked = customization.multiCardHands,
            onCheckedChange = { onChange(customization.copy(multiCardHands = it)) },
        )
        SwitchRow(
            title = cardCountTitle,
            summary = accuracySummary(cardCountTally),
            checked = customization.cardCountHands,
            onCheckedChange = { onChange(customization.copy(cardCountHands = it)) },
            // Every card-count hand has 3 or more cards
            enabled = customization.multiCardHands,
        )

        HAND_TYPES.groupBy { it.table }.forEach { (table, types) ->
            SettingsGroup(
                title = stringResource(table.title),
                summary = accuracySummary(types.map(tallies::getValue).reduce(Tally::plus)),
                icon = R.drawable.ic_customize_hands,
            ) {
                types.forEach { type ->
                    SwitchRow(
                        title = stringResource(type.move.displayName),
                        summary = accuracySummary(tallies.getValue(type)),
                        checked = type !in customization.switchedOff,
                        onCheckedChange = { on ->
                            val off = customization.switchedOff
                            onChange(customization.copy(switchedOff = if (on) off - type else off + type))
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun HandsDealtScreen(customization: HandCustomization, onChange: (HandCustomization) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.hands_dealt),
        choices = HandsDealt.entries.map { it to stringResource(it.title) },
        selected = customization.handsDealt,
        onSelect = { onChange(customization.copy(handsDealt = it)) },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun accuracySummary(tally: Tally): String = stringResource(R.string.accuracy_summary, tally.percentText() ?: stringResource(R.string.no_data))

private val HandsDealt.title: Int
    get() = when (this) {
        HandsDealt.RANDOM -> R.string.random
        HandsDealt.PRIORITIZE_WORSE -> R.string.prioritize_worse_hands
    }
