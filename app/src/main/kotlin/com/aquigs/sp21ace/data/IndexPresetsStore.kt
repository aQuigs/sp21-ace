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
        loaded.rewrite?.let(::write)
        return loaded.presets
    }

    fun save(presets: IndexPresets) = write(IndexPresetsCode.print(presets))

    private fun write(code: String) = prefs.edit { putString(PRESETS, code) }

    private companion object {
        const val PRESETS = "presets"
    }
}
