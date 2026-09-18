package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.ColorTheme
import com.aquigs.sp21ace.domain.settings.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "settings_test"

    // Cleared through the cached preferences with commit, so a write the last test queued can't land after the clear
    private fun clearSaved() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
    }

    @Before
    fun setUp() = clearSaved()

    @After
    fun tearDown() = clearSaved()

    @Test
    fun loadsBlackjackAcesDefaultsWhenNothingIsSaved() {
        assertEquals(Settings(), SettingsStore(context, name).load())
    }

    @Test
    fun loadsEverySettingAsSaved() {
        val defaults = Settings()
        // Each setting away from its default on its own, so no field can pass by matching its default, then all back again
        val eachChanged = listOf(
            defaults.copy(colorTheme = ColorTheme.LIGHT),
            defaults.copy(colorTheme = ColorTheme.DARK),
            defaults.copy(buttonLocation = ButtonLocation.LEFT),
            defaults.copy(handTotals = true),
            defaults.copy(chartButton = false),
            defaults.copy(streakMeter = false),
        )

        for (settings in eachChanged + defaults) {
            SettingsStore(context, name).save(settings)

            assertEquals(settings, SettingsStore(context, name).load())
        }
    }
}
