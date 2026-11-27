package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.indices.FlashcardOptions

/** Keeps the flashcard options through restarts. A test passes its own [name], so it never overwrites what the app saved. */
class FlashcardOptionsStore(context: Context, name: String = "flashcard_options") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val defaults = FlashcardOptions()

    fun load() = FlashcardOptions(
        cardsShown = prefs.getEnum(CARDS_SHOWN, defaults.cardsShown),
        noIndexCards = prefs.getBoolean(NO_INDEX_CARDS, defaults.noIndexCards),
        multiCardHands = prefs.getBoolean(MULTI_CARD_HANDS, defaults.multiCardHands),
    )

    fun save(options: FlashcardOptions) {
        prefs.edit {
            putString(CARDS_SHOWN, options.cardsShown.name)
            putBoolean(NO_INDEX_CARDS, options.noIndexCards)
            putBoolean(MULTI_CARD_HANDS, options.multiCardHands)
        }
    }

    private companion object {
        const val CARDS_SHOWN = "cards_shown"
        const val NO_INDEX_CARDS = "no_index_cards"
        const val MULTI_CARD_HANDS = "multi_card_hands"
    }
}
