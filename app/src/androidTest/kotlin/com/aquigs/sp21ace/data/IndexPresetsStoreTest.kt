package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
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

    @Test
    fun loadsEveryChangeAsSaved() {
        val added = IndexPresets.seeded().add("Mine", from = null)
        val changes = listOf(
            added,
            added.rename(added.activeId, "Renamed"),
            added.duplicate(BuiltInIndexSet.BJ_SHOE_S17.name, "Twin"),
            added.delete(BuiltInIndexSet.BJ_SHOE_H17.name),
            added.update(added.activeId) { it.copy(deckEstimate = DeckEstimate.ROUND, rounding = CountRounding.TRUNCATE) },
            IndexPresets.seeded().activate(BuiltInIndexSet.BJ_SHOE_S17.name),
        )

        for (presets in changes) {
            IndexPresetsStore(context, name).save(presets)

            assertEquals(presets, IndexPresetsStore(context, name).load())
        }
    }

    @Test
    fun loadsTheBuiltInPresetsWhenTheSaveCannotBeRead() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { putString("presets", "garbled") }

        assertEquals(IndexPresets.seeded(), IndexPresetsStore(context, name).load())
    }
}
