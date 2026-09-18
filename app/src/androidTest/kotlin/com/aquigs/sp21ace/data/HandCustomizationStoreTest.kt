package com.aquigs.sp21ace.data

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aquigs.sp21ace.domain.dealing.HAND_TYPES
import com.aquigs.sp21ace.domain.dealing.HandCustomization
import com.aquigs.sp21ace.domain.dealing.HandsDealt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HandCustomizationStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "customize_hands_test"

    // Cleared through the cached preferences with commit, so a write the last test queued can't land after the clear
    private fun clearSaved() {
        context.getSharedPreferences(name, Context.MODE_PRIVATE).edit(commit = true) { clear() }
    }

    @Before
    fun setUp() = clearSaved()

    @After
    fun tearDown() = clearSaved()

    @Test
    fun loadsRandomDealingWithEverySwitchOnWhenNothingIsSaved() {
        assertEquals(HandCustomization(), HandCustomizationStore(context, name).load())
    }

    @Test
    fun loadsTheModeAndEverySwitchAsSaved() {
        // Everything away from its default first, then each switch off on its own, so no field can pass by matching its default
        val customizations = listOf(HandCustomization(HandsDealt.PRIORITIZE_WORSE, switchedOff = HAND_TYPES.toSet())) +
            HAND_TYPES.map { HandCustomization(switchedOff = setOf(it)) } +
            HandCustomization()

        for (customization in customizations) {
            HandCustomizationStore(context, name).save(customization)

            assertEquals(customization, HandCustomizationStore(context, name).load())
        }
    }
}
