package com.aquigs.sp21ace.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.game.STARTING_BANKROLL
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "table_test"

    @get:Rule
    val cleared = ClearedPreferences(context, name)

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
