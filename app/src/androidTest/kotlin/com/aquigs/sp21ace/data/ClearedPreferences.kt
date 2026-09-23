package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import org.junit.rules.ExternalResource

/** Empties the named preferences before each test. */
class ClearedPreferences(private val context: Context, private val name: String) : ExternalResource() {
    // Cleared through the cached preferences with commit, so a write the last test queued can't land after the clear
    override fun before() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
    }
}
