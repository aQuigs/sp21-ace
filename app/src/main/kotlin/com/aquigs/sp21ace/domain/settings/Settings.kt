package com.aquigs.sp21ace.domain.settings

enum class ColorTheme { SYSTEM, LIGHT, DARK }

enum class ButtonLocation { LEFT, RIGHT }

/** How the app looks, what the trainer shows and whether answers make a sound, each starting as Blackjack Ace's does. */
data class Settings(
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val buttonLocation: ButtonLocation = ButtonLocation.RIGHT,
    val handTotals: Boolean = false,
    val chartButton: Boolean = true,
    val soundEffects: Boolean = false,
    val streakMeter: Boolean = true,
)
