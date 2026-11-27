package com.aquigs.sp21ace.ui.indices

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.FlashcardOptions
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.inPlainWords
import com.aquigs.sp21ace.domain.indices.title
import com.aquigs.sp21ace.domain.strategy.DEBATED_MARK
import com.aquigs.sp21ace.ui.components.ActionRow
import com.aquigs.sp21ace.ui.components.ChoicePage
import com.aquigs.sp21ace.ui.components.ChoiceRow
import com.aquigs.sp21ace.ui.components.EditableSwitchRow
import com.aquigs.sp21ace.ui.components.SettingsHeader
import com.aquigs.sp21ace.ui.components.SettingsIntro
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.components.SwitchRow

// The groups the active preset's indices are listed in, insurance last
private val PLAY_GROUPS = listOf(DeviationPlay.STAND, DeviationPlay.HIT, DeviationPlay.DOUBLE, DeviationPlay.SPLIT, DeviationPlay.SURRENDER)

/**
 * What the flashcards drill: the active preset and its counting conventions, the flashcard options, which hold for every preset,
 * and the preset's indices by play, each switched on or off here and edited on a page of its own.
 */
@Composable
fun IndicesScreen(
    presets: IndexPresets,
    options: FlashcardOptions,
    onPresetsChange: (IndexPresets) -> Unit,
    onOptionsChange: (FlashcardOptions) -> Unit,
    onOpenPresets: () -> Unit,
    onOpenDeckEstimate: () -> Unit,
    onOpenRounding: () -> Unit,
    onOpenCardsShown: () -> Unit,
    onAddIndex: () -> Unit,
    onEditIndex: (Deviation) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preset = presets.active

    SettingsPage(title = stringResource(R.string.indices), onBack = onBack, modifier = modifier) {
        SettingsIntro(stringResource(R.string.indices_intro))
        ChoiceRow(title = stringResource(R.string.preset), value = preset.name, onClick = onOpenPresets)
        ChoiceRow(title = stringResource(R.string.deck_estimate), value = stringResource(preset.deckEstimate.title), onClick = onOpenDeckEstimate)
        ChoiceRow(title = stringResource(R.string.true_count_rounding), value = stringResource(preset.rounding.title), onClick = onOpenRounding)
        HorizontalDivider()

        SettingsHeader(stringResource(R.string.flashcards))
        ChoiceRow(title = stringResource(R.string.cards_shown), value = stringResource(options.cardsShown.title), onClick = onOpenCardsShown)
        SwitchRow(
            title = stringResource(R.string.hands_with_no_index),
            summary = stringResource(if (options.noIndexCards) R.string.no_index_cards_on else R.string.no_index_cards_off),
            checked = options.noIndexCards,
            onCheckedChange = { onOptionsChange(options.copy(noIndexCards = it)) },
        )
        SwitchRow(
            title = stringResource(R.string.multi_card_hands),
            summary = stringResource(if (options.multiCardHands) R.string.multi_card_flashcards_on else R.string.multi_card_flashcards_off),
            checked = options.multiCardHands,
            onCheckedChange = { onOptionsChange(options.copy(multiCardHands = it)) },
        )
        HorizontalDivider()

        // First rather than after every index, so it stays in reach of a long list
        ActionRow(title = stringResource(R.string.add_index), onClick = onAddIndex)
        if (preset.entries.isEmpty()) {
            Text(
                text = stringResource(R.string.no_indices_in_preset),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val byPlay = preset.entries.groupBy { (it.index.deviation as? Deviation.OnHand)?.play }
        PLAY_GROUPS.forEach { play -> byPlay[play]?.let { IndexGroup(play.title, it, presets, onPresetsChange, onEditIndex) } }
        byPlay[null]?.let { IndexGroup(stringResource(R.string.insurance), it, presets, onPresetsChange, onEditIndex) }
    }
}

@Composable
private fun IndexGroup(
    title: String,
    entries: List<IndexEntry>,
    presets: IndexPresets,
    onPresetsChange: (IndexPresets) -> Unit,
    onEditIndex: (Deviation) -> Unit,
) {
    val preset = presets.active

    SettingsHeader(title)
    entries.forEach { entry ->
        val index = entry.index
        // Non-breaking, so a large font never leaves the mark or the tag on a line of its own
        val words = index.inPlainWords + if (preset.isDebated(index)) "\u00A0$DEBATED_MARK" else ""

        EditableSwitchRow(
            title = index.deviation.title,
            summary = if (preset.isCustom(index)) stringResource(R.string.custom_index, words) else words,
            checked = entry.enabled,
            onCheckedChange = { on -> onPresetsChange(presets.updateActive { it.switch(index.deviation, on) }) },
            clickLabel = stringResource(R.string.edit_index_label),
            onClick = { onEditIndex(index.deviation) },
        )
    }
}

@Composable
fun DeckEstimateScreen(presets: IndexPresets, onChange: (IndexPresets) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.deck_estimate),
        choices = DeckEstimate.entries.map { it to stringResource(it.title) },
        selected = presets.active.deckEstimate,
        onSelect = { estimate -> onChange(presets.updateActive { it.copy(deckEstimate = estimate) }) },
        onBack = onBack,
        modifier = modifier,
        intro = stringResource(R.string.deck_estimate_intro),
    )
}

@Composable
fun CountRoundingScreen(presets: IndexPresets, onChange: (IndexPresets) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.true_count_rounding),
        choices = CountRounding.entries.map { it to stringResource(it.title) },
        selected = presets.active.rounding,
        onSelect = { rounding -> onChange(presets.updateActive { it.copy(rounding = rounding) }) },
        onBack = onBack,
        modifier = modifier,
        intro = stringResource(R.string.true_count_rounding_intro),
    )
}

@Composable
fun CardsShownScreen(options: FlashcardOptions, onChange: (FlashcardOptions) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    ChoicePage(
        title = stringResource(R.string.cards_shown),
        choices = CardsShown.entries.map { it to stringResource(it.title) },
        selected = options.cardsShown,
        onSelect = { onChange(options.copy(cardsShown = it)) },
        onBack = onBack,
        modifier = modifier,
        intro = stringResource(R.string.cards_shown_intro, stringResource(CardsShown.RANDOM.title), stringResource(CardsShown.PRIORITIZE_WORSE.title)),
    )
}

internal val DeckEstimate.title: Int
    get() = when (this) {
        DeckEstimate.TRUNCATE -> R.string.truncate
        DeckEstimate.ROUND -> R.string.round
        DeckEstimate.PERCENTILE_95 -> R.string.percentile_95
    }

internal val CountRounding.title: Int
    get() = when (this) {
        CountRounding.FLOOR -> R.string.floor
        CountRounding.TRUNCATE -> R.string.truncate
        CountRounding.ROUND -> R.string.round
    }

private val CardsShown.title: Int
    get() = when (this) {
        CardsShown.RANDOM -> R.string.random
        CardsShown.PRIORITIZE_WORSE -> R.string.prioritize_worse_indices
    }
