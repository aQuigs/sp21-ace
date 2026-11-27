package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.truecount.DeckPrecision
import com.aquigs.sp21ace.domain.truecount.DecksShown
import com.aquigs.sp21ace.domain.truecount.TrueCountOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrueCountOptionsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "true_count_options_test"

    @get:Rule
    val cleared = ClearedPreferences(context, name)

    @Test
    fun loadsEachOptionAsSavedAndTheDefaultsWhenNothingIs() {
        assertEquals(TrueCountOptions(), TrueCountOptionsStore(context, name).load())

        // All away from their defaults, then each on its own, so no field can pass by matching its default
        val options = listOf(
            TrueCountOptions(8, DeckPrecision.QUARTER, CountRounding.ROUND, DecksShown.DISCARDED, CardsShown.PRIORITIZE_WORSE),
            TrueCountOptions(shoe = 1),
            TrueCountOptions(precision = DeckPrecision.WHOLE),
            TrueCountOptions(rounding = CountRounding.TRUNCATE),
            TrueCountOptions(decksShown = DecksShown.DISCARDED),
            TrueCountOptions(cardsShown = CardsShown.PRIORITIZE_WORSE),
        )
        for (saved in options) {
            TrueCountOptionsStore(context, name).save(saved)

            assertEquals(saved, TrueCountOptionsStore(context, name).load())
        }
    }

    @Test
    fun aShoeOrValueThisVersionDoesNotKnowReadsAsItsDefault() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) {
            putInt("shoe", 5)
            putString("precision", "EIGHTH")
        }

        assertEquals(TrueCountOptions(), TrueCountOptionsStore(context, name).load())
    }
}
