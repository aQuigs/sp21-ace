package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import com.aquigs.sp21ace.domain.settings.Settings

/** Keeps the settings through restarts. A test passes its own [name], so it never overwrites the settings the app saved. */
class SettingsStore(context: Context, name: String = "settings") {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val defaults = Settings()

    fun load() = Settings(
        colorTheme = prefs.getEnum(COLOR_THEME, defaults.colorTheme),
        buttonLocation = prefs.getEnum(BUTTON_LOCATION, defaults.buttonLocation),
        handTotals = prefs.getBoolean(HAND_TOTALS, defaults.handTotals),
        chartButton = prefs.getBoolean(CHART_BUTTON, defaults.chartButton),
        streakMeter = prefs.getBoolean(STREAK_METER, defaults.streakMeter),
    )

    fun save(settings: Settings) {
        prefs.edit {
            putString(COLOR_THEME, settings.colorTheme.name)
            putString(BUTTON_LOCATION, settings.buttonLocation.name)
            putBoolean(HAND_TOTALS, settings.handTotals)
            putBoolean(CHART_BUTTON, settings.chartButton)
            putBoolean(STREAK_METER, settings.streakMeter)
        }
    }

    private companion object {
        const val COLOR_THEME = "color_theme"
        const val BUTTON_LOCATION = "button_location"
        const val HAND_TOTALS = "hand_totals"
        const val CHART_BUTTON = "chart_button"
        const val STREAK_METER = "streak_meter"
    }
}
