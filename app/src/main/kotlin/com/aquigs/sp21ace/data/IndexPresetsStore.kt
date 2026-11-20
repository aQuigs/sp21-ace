package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.indices.IndexPresets

/**
 * Keeps the index presets through restarts. Until the first save it loads the built-in ones, and so does a save it can't read.
 * Saved presets load once with any built-in number corrected since they were saved, and are stored again as corrected.
 * A test passes its own [name], so it never overwrites the presets the app saved.
 */
class IndexPresetsStore(context: Context, name: String = "index_presets") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun load(): IndexPresets {
        val loaded = IndexPresetsCode.load(prefs.getString(PRESETS, null))
        if (loaded.corrected) save(loaded.presets)
        return loaded.presets
    }

    fun save(presets: IndexPresets) {
        prefs.edit { putString(PRESETS, IndexPresetsCode.print(presets)) }
    }

    private companion object {
        const val PRESETS = "presets"
    }
}
