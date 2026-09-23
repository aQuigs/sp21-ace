package com.aquigs.sp21ace.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.strategy.TableRules
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableRulesStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "table_rules_test"

    @get:Rule
    val cleared = ClearedPreferences(context, name)

    @Test
    fun loadsTheDefaultRulesWhenNoneAreSaved() {
        assertEquals(TableRules(), TableRulesStore(context, name).load())
    }

    @Test
    fun loadsEveryCombinationAsSaved() {
        // Every field off its default first, so none can pass by matching it
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
