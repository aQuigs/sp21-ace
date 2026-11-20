package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.indices.BUILT_IN_REVISION
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
import com.aquigs.sp21ace.domain.indices.INDEX_CORRECTIONS
import com.aquigs.sp21ace.domain.indices.IndexPresets
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IndexPresetsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "index_presets_test"

    @get:Rule
    val cleared = ClearedPreferences(context, name)

    @Test
    fun loadsTheBuiltInPresetsWhenNothingIsSaved() {
        assertEquals(IndexPresets.seeded(), IndexPresetsStore(context, name).load())
    }

    // IndexPresetsCodeTest covers every change on the JVM, so one away from the seeded presets shows the store keeps the code
    @Test
    fun loadsThePresetsAsSaved() {
        val added = IndexPresets.seeded().add("Mine", from = null).delete(BuiltInIndexSet.BJ_SHOE_H17.name)
        val presets = added.update(added.activeId) { it.copy(deckEstimate = DeckEstimate.ROUND, rounding = CountRounding.TRUNCATE) }

        IndexPresetsStore(context, name).save(presets)

        assertEquals(presets, IndexPresetsStore(context, name).load())
    }

    // IndexPresetsCodeTest covers which corrections apply on the JVM; this shows the store saves the corrected presets back
    @Test
    fun presetsSavedBeforeACorrectionLoadCorrectedAndAreSavedAgainAsCorrected() {
        val firstNumbers = INDEX_CORRECTIONS.groupBy { it.set to it.from.deviation }.map { (_, corrections) -> corrections.minBy { it.revision } }
        val old = firstNumbers.fold(IndexPresets.seeded()) { presets, correction -> presets.update(correction.set.name) { it.edit(correction.from) } }
        val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
        prefs.edit(commit = true) { putString("presets", IndexPresetsCode.print(old).replace(" revision=$BUILT_IN_REVISION", "")) }

        assertEquals(IndexPresets.seeded(), IndexPresetsStore(context, name).load())
        assertEquals(IndexPresetsCode.print(IndexPresets.seeded()), prefs.getString("presets", null))
    }

    @Test
    fun loadsTheBuiltInPresetsWhenTheSaveCannotBeRead() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { putString("presets", "garbled") }

        assertEquals(IndexPresets.seeded(), IndexPresetsStore(context, name).load())
    }
}
