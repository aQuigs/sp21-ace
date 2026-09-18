package com.aquigs.sp21ace.ui.hands

import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.dealing.HandType
import com.aquigs.sp21ace.domain.dealing.HandsDealt
import com.aquigs.sp21ace.domain.history.HandFilter
import com.aquigs.sp21ace.domain.history.Period
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.history.Tally
import com.aquigs.sp21ace.domain.history.accuracy
import com.aquigs.sp21ace.ui.chart.title
import com.aquigs.sp21ace.ui.components.ChoicePage
import com.aquigs.sp21ace.ui.components.ChoiceRow
import com.aquigs.sp21ace.ui.components.SettingsIntro
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.components.SwitchRow
import com.aquigs.sp21ace.ui.components.displayName
import java.time.Instant

/**
 * How the trainer deals, and which types of hand it deals: a switch for every move each kind of hand calls for, each with the
 * accuracy of every answer ever given, as Blackjack Ace counts it whatever the period on Accuracy.
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
    val accuracy = remember(history) { HandFilter.entries.associateWith { history.accuracy(Period.ALL_TIME, it, Instant.now()) } }

    SettingsPage(title = stringResource(R.string.customize_hands), onBack = onBack, modifier = modifier) {
        SettingsIntro(handsDealtIntro())
        ChoiceRow(title = stringResource(R.string.hands_dealt), value = stringResource(customization.handsDealt.title), onClick = onOpenHandsDealt)
        HorizontalDivider()
        SettingsIntro(stringResource(R.string.hand_types_intro))

        HandFilter.entries.forEach { filter ->
            val table = filter.table ?: return@forEach
            val figures = accuracy.getValue(filter)

            HandTypeGroup(title = stringResource(table.title), summary = accuracySummary(figures.overall)) {
                filter.moves.forEach { move ->
                    val type = HandType(table, move)

                    SwitchRow(
                        title = stringResource(move.displayName),
                        summary = accuracySummary(figures.byMove.getValue(move)),
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

// As in Blackjack Ace, the intro puts the choices it explains in bold
@Composable
private fun handsDealtIntro(): AnnotatedString {
    val choices = HandsDealt.entries.map { stringResource(it.title) }
    val text = stringResource(R.string.hands_dealt_intro, choices[0], choices[1])

    return buildAnnotatedString {
        append(text)
        choices.forEach { choice ->
            val start = text.indexOf(choice)
            if (start >= 0) addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, start + choice.length)
        }
    }
}

/** A kind of hand whose header shows or hides its switches. Every group starts open, as Blackjack Ace's do. */
@Composable
private fun HandTypeGroup(title: String, summary: String, switches: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    ListItem(
        headlineContent = { Text(text = title, fontWeight = FontWeight.Bold) },
        modifier = Modifier.clickable(onClickLabel = stringResource(if (expanded) R.string.collapse else R.string.expand)) { expanded = !expanded },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(painterResource(R.drawable.ic_customize_hands), contentDescription = null) },
        trailingContent = {
            Icon(painterResource(R.drawable.ic_expand_less), contentDescription = null, modifier = Modifier.rotate(if (expanded) 0f else 180f))
        },
    )
    if (expanded) switches()
}

/** "Accuracy: 66.6%", rounded down as on Accuracy, or "Accuracy: --" with nothing answered. */
@Composable
private fun accuracySummary(tally: Tally): String =
    stringResource(R.string.accuracy_summary, tally.accuracyPermille?.let { stringResource(R.string.percentage, it / 10.0) } ?: stringResource(R.string.no_data))

private val HandsDealt.title: Int
    get() = when (this) {
        HandsDealt.RANDOM -> R.string.random
        HandsDealt.PRIORITIZE_WORSE -> R.string.prioritize_worse_hands
    }
