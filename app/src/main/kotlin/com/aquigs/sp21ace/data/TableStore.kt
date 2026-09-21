package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.game.STARTING_BANKROLL

/**
 * Keeps the player's chips, in cents, through restarts. The cards on the table live only as long as the activity's saved state,
 * so a restart mid-round gives back the bets. A test passes its own [name], so it never overwrites the chips the app saved.
 */
class TableStore(context: Context, name: String = "table") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun loadChips(): Long = prefs.getLong(CHIPS, STARTING_BANKROLL)

    fun saveChips(chips: Long) {
        prefs.edit { putLong(CHIPS, chips) }
    }

    private companion object {
        const val CHIPS = "chips"
    }
}
