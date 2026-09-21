package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.game.STARTING_BANKROLL
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "table_test"

    // Cleared through the cached preferences with commit, so a write the last test queued can't land after the clear
    private fun clearSaved() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
    }

    @Before
    fun setUp() = clearSaved()

    @After
    fun tearDown() = clearSaved()

    @Test
    fun startsWithBlackjackAcesBankrollWhenNothingIsSaved() {
        assertEquals(STARTING_BANKROLL, TableStore(context, name).loadChips())
    }

    @Test
    fun loadsTheChipsAsSaved() {
        TableStore(context, name).saveChips(103_750)

        assertEquals(103_750L, TableStore(context, name).loadChips())
    }
}
