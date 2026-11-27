package com.aquigs.sp21ace.domain.settings

enum class ColorTheme { SYSTEM, LIGHT, DARK }

enum class ButtonLocation { LEFT, RIGHT }

/**
 * How the app looks, what the trainer, the flashcards and the table show, and whether answers make a sound, each starting as
 * Blackjack Ace's does. Blackjack Ace has no flashcards.
 */
data class Settings(
    val colorTheme: ColorTheme = ColorTheme.SYSTEM,
    val buttonLocation: ButtonLocation = ButtonLocation.RIGHT,
    val handTotals: Boolean = false,
    val chartButton: Boolean = true,
    val streakMeter: Boolean = true,
    val soundEffects: Boolean = false,
    val flashcardTimer: Boolean = true,
    val warnOnIncorrectMove: Boolean = true,
    val hintButton: Boolean = true,
) {
    val buttonsOnLeft: Boolean get() = buttonLocation == ButtonLocation.LEFT
}
