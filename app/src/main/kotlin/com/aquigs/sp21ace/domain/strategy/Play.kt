package com.aquigs.sp21ace.domain.strategy

import java.io.Serializable

enum class Action(val code: String) { HIT("H"), STAND("S"), DOUBLE("D"), SPLIT("P"), SURRENDER("R"), SURRENDER_OR_HIT("RH") }

/** A play gives way to a hit while one of these bonus hands is still possible. */
enum class BonusException(val mark: String) {
    ANY_678("*"),

    /** Suited 6-7-8, spades included. */
    SUITED_678("'"),
    SPADED_678("\""),

    /** Suited 7-7-7 against a dealer 7, the Super Bonus. */
    SUITED_777("$"),
}

/** Printed after a square the sources still debate. */
internal const val DEBATED_MARK = "†"

/**
 * One chart square in Wizard of Odds notation: the play, the card count from which to hit instead, and a bonus mark.
 * S5' means stand, but hit with 5 or more cards or while a suited 6-7-8 is possible.
 */
data class Play(
    val action: Action,
    val hitWithCards: Int? = null,
    val bonusException: BonusException? = null,
    val debated: Boolean = false,
) : Serializable {
    companion object {
        // The notation only puts card counts on doubles and stands, 6-7-8 marks on counted stands, and $ on a bare split
        private val CODE = Regex("""(RH|[HSDPR])((?<=[DS])[3-6])?((?<=S[3-6])[*'"]|(?<=P)\$)?($DEBATED_MARK)?""")

        fun parse(code: String): Play {
            val match = requireNotNull(CODE.matchEntire(code)) { "Unknown chart code: $code" }
            val (base, cards, mark, dagger) = match.destructured
            return Play(
                Action.entries.first { it.code == base },
                cards.toIntOrNull(),
                BonusException.entries.firstOrNull { it.mark == mark },
                dagger.isNotEmpty(),
            )
        }
    }
}

/** The square as the charts print it, which [Play.parse] reads back as the same play. */
val Play.code: String
    get() = listOfNotNull(action.code, hitWithCards?.toString(), bonusException?.mark, DEBATED_MARK.takeIf { debated }).joinToString("")
