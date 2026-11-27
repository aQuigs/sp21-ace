package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.drill.CardsShown
import com.aquigs.sp21ace.domain.indices.FlashcardOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FlashcardOptionsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "flashcard_options_test"

    @get:Rule
    val cleared = ClearedPreferences(context, name)

    @Test
    fun loadsRandomTwoCardHandsWhenNothingIsSaved() {
        assertEquals(FlashcardOptions(CardsShown.RANDOM, noIndexCards = false, multiCardHands = false), FlashcardOptionsStore(context, name).load())
    }

    @Test
    fun loadsEachOptionAsSaved() {
        // All away from their defaults, then each on its own, so no field can pass by matching its default
        val options = listOf(
            FlashcardOptions(CardsShown.PRIORITIZE_WORSE, noIndexCards = true, multiCardHands = true),
            FlashcardOptions(CardsShown.PRIORITIZE_WORSE),
            FlashcardOptions(noIndexCards = true),
            FlashcardOptions(multiCardHands = true),
            FlashcardOptions(),
        )

        for (saved in options) {
            FlashcardOptionsStore(context, name).save(saved)

            assertEquals(saved, FlashcardOptionsStore(context, name).load())
        }
    }

    @Test
    fun aCardOrderThisVersionDoesNotKnowReadsAsRandom() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { putString("cards_shown", "SLOWEST_FIRST") }

        assertEquals(FlashcardOptions(), FlashcardOptionsStore(context, name).load())
    }
}
