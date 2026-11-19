package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.pairClass
import com.aquigs.sp21ace.domain.strategy.totalClass

/** "Hard 16", "Soft 18", "Pair of 8s", "Pair of aces", worded as the trainer words its hands. */
val IndexHand.title: String get() = if (kind == HandKind.PAIR) pairClass(pairCard) else totalClass(total)

/** "Hard 16 vs 10", or "Insurance". */
val Deviation.title: String
    get() = when (this) {
        is Deviation.OnHand -> "${hand.title} vs ${upcard.label}"
        Deviation.Insurance -> "Insurance"
    }

/** "Hit", "Stand", "Double", "Split" or "Surrender". */
val DeviationPlay.title: String get() = name.lowercase().replaceFirstChar(Char::uppercase)

/** The play an index names: "Hit", "Stand", "Double", "Split", "Surrender" or "Insurance". */
val Deviation.playName: String
    get() = when (this) {
        is Deviation.OnHand -> play.title
        Deviation.Insurance -> "Insurance"
    }

/** A true count with its sign: "+3", "0", or "−2" with a true minus, which screen readers say as minus. */
fun signedCount(count: Int): String = when {
    count > 0 -> "+$count"
    count < 0 -> "−${-count}"
    else -> "0"
}

/** "or higher", or "or lower". */
val IndexDirection.inPlainWords: String
    get() = when (this) {
        IndexDirection.AT_OR_ABOVE -> "or higher"
        IndexDirection.AT_OR_BELOW -> "or lower"
    }

/** "Stand at 0 or higher", "Hit at −1 or lower", "Insurance at +3 or higher". */
val PlayIndex.inPlainWords: String get() = "${deviation.playName} at ${signedCount(threshold)} ${direction.inPlainWords}"
