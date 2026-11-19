package com.aquigs.sp21ace.domain.indices

/** "Hard 16", "Soft 18", "Pair of 8s", "Pair of aces", worded as the trainer's [com.aquigs.sp21ace.domain.strategy.handClass]. */
val IndexHand.title: String
    get() = when (kind) {
        HandKind.HARD -> "Hard $value"
        HandKind.SOFT -> "Soft $value"
        HandKind.PAIR -> if (value == 1) "Pair of aces" else "Pair of ${value}s"
    }

/** "Hard 16 vs 10", or "Insurance". */
val Deviation.title: String
    get() = when (this) {
        is Deviation.OnHand -> "${hand.title} vs ${upcard.label}"
        Deviation.Insurance -> "Insurance"
    }

/** The play an index makes from its threshold up: "Stand", "Double", "Split", "Surrender" or "Insurance". */
val Deviation.playName: String
    get() = when (this) {
        is Deviation.OnHand -> play.name.lowercase().replaceFirstChar(Char::uppercase)
        Deviation.Insurance -> "Insurance"
    }

/** A true count with its sign: "+3", "0", or "−2" with a true minus, which screen readers say as minus. */
fun signedCount(count: Int): String = when {
    count > 0 -> "+$count"
    count < 0 -> "−${-count}"
    else -> "0"
}

/** "Stand at 0 or higher", "Insurance at +3 or higher". */
val PlayIndex.inPlainWords: String get() = "${deviation.playName} at ${signedCount(threshold)} or higher"
