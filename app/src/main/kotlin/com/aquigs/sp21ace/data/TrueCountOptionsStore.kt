package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.truecount.SHOE_SIZES
import com.aquigs.sp21ace.domain.truecount.TrueCountOptions

/** Keeps the true count flashcard options through restarts. A test passes its own [name], so it never overwrites what the app saved. */
class TrueCountOptionsStore(context: Context, name: String = "true_count_options") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val defaults = TrueCountOptions()

    fun load() = TrueCountOptions(
        shoe = prefs.getInt(SHOE, defaults.shoe).takeIf { it in SHOE_SIZES } ?: defaults.shoe,
        precision = prefs.getEnum(PRECISION, defaults.precision),
        rounding = prefs.getEnum(ROUNDING, defaults.rounding),
        decksShown = prefs.getEnum(DECKS_SHOWN, defaults.decksShown),
        cardsShown = prefs.getEnum(CARDS_SHOWN, defaults.cardsShown),
    )

    fun save(options: TrueCountOptions) {
        prefs.edit {
            putInt(SHOE, options.shoe)
            putString(PRECISION, options.precision.name)
            putString(ROUNDING, options.rounding.name)
            putString(DECKS_SHOWN, options.decksShown.name)
            putString(CARDS_SHOWN, options.cardsShown.name)
        }
    }

    private companion object {
        const val SHOE = "shoe"
        const val PRECISION = "precision"
        const val ROUNDING = "rounding"
        const val DECKS_SHOWN = "decks_shown"
        const val CARDS_SHOWN = "cards_shown"
    }
}
