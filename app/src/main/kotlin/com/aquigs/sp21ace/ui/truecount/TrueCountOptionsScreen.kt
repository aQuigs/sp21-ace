package com.aquigs.sp21ace.ui.truecount

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.signedCount
import com.aquigs.sp21ace.domain.truecount.DeckPrecision
import com.aquigs.sp21ace.domain.truecount.DecksShown
import com.aquigs.sp21ace.domain.truecount.SHOE_SIZES
import com.aquigs.sp21ace.domain.truecount.TrueCountOptions
import com.aquigs.sp21ace.ui.components.ChipField
import com.aquigs.sp21ace.ui.components.SettingsIntro
import com.aquigs.sp21ace.ui.components.SettingsNote
import com.aquigs.sp21ace.ui.components.SettingsPage
import com.aquigs.sp21ace.ui.indices.title

/** What the true count flashcards ask, each option's values side by side. */
@Composable
fun TrueCountOptionsScreen(options: TrueCountOptions, onChange: (TrueCountOptions) -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    SettingsPage(title = stringResource(R.string.true_count_options), onBack = onBack, modifier = modifier) {
        SettingsIntro(stringResource(R.string.true_count_options_intro))

        ChipField(
            name = stringResource(R.string.shoe_size),
            choices = SHOE_SIZES.map { it to deckCount(it) },
            selected = options.shoe,
            onSelect = { onChange(options.copy(shoe = it)) },
        )
        ChipField(
            name = stringResource(R.string.deck_precision),
            choices = DeckPrecision.entries.map { it to stringResource(it.title) },
            selected = options.precision,
            onSelect = { onChange(options.copy(precision = it)) },
        )
        ChipField(
            name = stringResource(R.string.decks_shown),
            choices = DecksShown.entries.map { it to stringResource(it.title) },
            selected = options.decksShown,
            onSelect = { onChange(options.copy(decksShown = it)) },
        )
        ChipField(
            name = stringResource(R.string.true_count_rounding),
            choices = CountRounding.entries.map { it to stringResource(it.title) },
            selected = options.rounding,
            onSelect = { onChange(options.copy(rounding = it)) },
        )
        SettingsNote(
            stringResource(R.string.rounding_example, signedCount(options.rounding.divide(3, 2)), signedCount(options.rounding.divide(-3, 2))),
        )
        ChipField(
            name = stringResource(R.string.cards_shown),
            choices = CardsShown.entries.map { it to stringResource(it.title) },
            selected = options.cardsShown,
            onSelect = { onChange(options.copy(cardsShown = it)) },
        )
        if (options.cardsShown == CardsShown.PRIORITIZE_WORSE) SettingsNote(stringResource(R.string.prioritize_worse_decks_note))
    }
}

/** "6 decks". */
@Composable
internal fun deckCount(decks: Int): String = pluralStringResource(R.plurals.deck_count, decks, decks)

private val DeckPrecision.title: Int
    get() = when (this) {
        DeckPrecision.WHOLE -> R.string.whole_decks
        DeckPrecision.HALF -> R.string.half_decks
        DeckPrecision.QUARTER -> R.string.quarter_decks
    }

private val DecksShown.title: Int
    get() = when (this) {
        DecksShown.LEFT -> R.string.decks_left_caption
        DecksShown.DISCARDED -> R.string.decks_discarded_caption
    }

private val CardsShown.title: Int
    get() = when (this) {
        CardsShown.RANDOM -> R.string.random
        CardsShown.PRIORITIZE_WORSE -> R.string.prioritize_worse_decks
    }
