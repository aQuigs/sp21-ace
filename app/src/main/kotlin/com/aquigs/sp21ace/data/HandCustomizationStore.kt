package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.dealing.HAND_TYPES
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.dealing.HandType
import com.aquigs.sp21ace.domain.dealing.HandsDealt

/** Keeps how the trainer deals through restarts. A test passes its own [name], so it never overwrites what the app saved. */
class HandCustomizationStore(context: Context, name: String = "customize_hands") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val defaults = HandCustomization()

    fun load() = HandCustomization(
        handsDealt = prefs.getEnum(HANDS_DEALT, defaults.handsDealt),
        switchedOff = HAND_TYPES.filter { !prefs.getBoolean(it.key, true) }.toSet(),
    )

    fun save(customization: HandCustomization) {
        prefs.edit {
            putString(HANDS_DEALT, customization.handsDealt.name)
            HAND_TYPES.forEach { putBoolean(it.key, it !in customization.switchedOff) }
        }
    }

    private companion object {
        const val HANDS_DEALT = "hands_dealt"

        // A switch without a saved value reads as on, so a type added later starts on
        val HandType.key: String get() = "deal_${table.name.lowercase()}_${move.name.lowercase()}"
    }
}
