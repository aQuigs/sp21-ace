package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.strategy.TableRules

/** Keeps the table rules through restarts. A test passes its own [name], so it never overwrites the rules the app saved. */
class TableRulesStore(context: Context, name: String = "table_rules") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val defaults = TableRules()

    fun load() = TableRules(
        dealerHitsSoft17 = prefs.getBoolean(DEALER_HITS_SOFT_17, defaults.dealerHitsSoft17),
        redoubling = prefs.getBoolean(REDOUBLING, defaults.redoubling),
    )

    fun save(rules: TableRules) {
        prefs.edit {
            putBoolean(DEALER_HITS_SOFT_17, rules.dealerHitsSoft17)
            putBoolean(REDOUBLING, rules.redoubling)
        }
    }

    private companion object {
        const val DEALER_HITS_SOFT_17 = "dealer_hits_soft_17"
        const val REDOUBLING = "redoubling"
    }
}
