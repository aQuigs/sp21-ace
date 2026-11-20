package com.aquigs.sp21ace.domain.indices

/**
 * How many decks count as dealt when a true count divides by the decks left, as Casino Vérité's Deck Estimate setting offers.
 * A preset carries it for the counting it drills; it changes no index number.
 */
enum class DeckEstimate {
    /** Only whole decks dealt count: 0 to 51 cards gone is a full shoe, 52 to 103 one deck dealt. */
    TRUNCATE,

    /** The nearest whole deck: 0 to 25 cards gone is a full shoe, 26 to 77 one deck dealt. */
    ROUND,

    /** Casino Vérité's 95th percentile: 0 to 49 cards gone is a full shoe, 50 to 99 one deck dealt. */
    PERCENTILE_95,
    ;

    companion object {
        val DEFAULT = TRUNCATE
    }
}

/**
 * How a true count with a fraction becomes the whole number an index is compared with, as Casino Vérité's True Count Division
 * setting offers. Every published table assumes one, so a preset carries the one its numbers were made for.
 */
enum class CountRounding {
    /** Always down: 1.5 counts as 1, and −1.5 as −2. */
    FLOOR,

    /** Toward zero: 1.5 counts as 1, and −1.5 as −1. */
    TRUNCATE,

    /** To the nearest whole number, a half up: 1.5 counts as 2, and −1.5 as −1. */
    ROUND,
    ;

    companion object {
        val DEFAULT = FLOOR
    }
}
