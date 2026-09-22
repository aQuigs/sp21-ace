package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.strategy.TableRules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableRulesStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "table_rules_test"

    // Cleared through the cached preferences with commit, so a write the last test queued can't land after the clear
    private fun clearSaved() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
    }

    @Before
    fun setUp() = clearSaved()

    @After
    fun tearDown() = clearSaved()

    @Test
    fun loadsTheDefaultRulesWhenNoneAreSaved() {
        assertEquals(TableRules(), TableRulesStore(context, name).load())
    }

    @Test
    fun loadsEveryCombinationAsSaved() {
        // All on first, so no field can pass by matching its default
        val both = listOf(true, false)
        val combinations = both.flatMap { hits ->
            both.flatMap { redoubling ->
                both.map { insurance -> TableRules(hits, redoubling, insurance, penetration = if (insurance) 60 else 75, splitBonuses = !insurance) }
            }
        }
        for (rules in combinations) {
            TableRulesStore(context, name).save(rules)

            assertEquals(rules, TableRulesStore(context, name).load())
        }
    }
}
