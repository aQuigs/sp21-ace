package com.aquigs.sp21ace.domain.strategy

enum class Action { HIT, STAND, DOUBLE, SPLIT, SURRENDER, SURRENDER_OR_HIT }

/** A play gives way to a hit while one of these bonus hands is still possible. */
enum class BonusException {
    ANY_678,

    /** Suited 6-7-8, spades included. */
    SUITED_678,
    SPADED_678,

    /** Suited 7-7-7 against a dealer 7, the Super Bonus. */
    SUITED_777,
}

/**
 * One chart square in Wizard of Odds notation: the play, the card count from which to hit instead, and a bonus mark.
 * S5' means stand, but hit with 5 or more cards or while a suited 6-7-8 is possible.
 */
data class Play(
    val action: Action,
    val hitWithCards: Int? = null,
    val bonusException: BonusException? = null,
    val debated: Boolean = false,
) {
    companion object {
        private val CODE = Regex("""(RH|[HSDPR])([3-6])?([*'"$])?(†)?""")
        private val ACTIONS = mapOf(
            "H" to Action.HIT,
            "S" to Action.STAND,
            "D" to Action.DOUBLE,
            "P" to Action.SPLIT,
            "R" to Action.SURRENDER,
            "RH" to Action.SURRENDER_OR_HIT,
        )
        private val MARKS = mapOf(
            "*" to BonusException.ANY_678,
            "'" to BonusException.SUITED_678,
            "\"" to BonusException.SPADED_678,
            "$" to BonusException.SUITED_777,
        )

        fun parse(code: String): Play {
            val match = requireNotNull(CODE.matchEntire(code)) { "Unknown chart code: $code" }
            val (base, cards, mark, dagger) = match.destructured
            val play = Play(ACTIONS.getValue(base), cards.toIntOrNull(), MARKS[mark], dagger.isNotEmpty())
            require(play.isPrinted()) { "Unknown chart code: $code" }
            return play
        }

        // The notation only puts card counts on doubles and stands, 6-7-8 marks on counted stands, and $ on a plain split
        private fun Play.isPrinted() = when (bonusException) {
            null -> hitWithCards == null || action == Action.DOUBLE || action == Action.STAND
            BonusException.SUITED_777 -> action == Action.SPLIT && hitWithCards == null
            else -> action == Action.STAND && hitWithCards != null
        }
    }
}
