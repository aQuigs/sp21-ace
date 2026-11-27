package com.aquigs.sp21ace.domain.drill

import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.numberInPlainWords
import com.aquigs.sp21ace.domain.indices.signedCount

/** The [numbers] a card can be answered with. When [openEnded], the first and last stand for every number past them too. */
data class AnswerPad(val numbers: IntRange, val openEnded: Boolean = false) {
    /** [count] as the pad answers it: itself, or the end it lies past. */
    fun answer(count: Int): Int = count.coerceIn(numbers)

    /** "+3", or for an open end "≥+6" or "≤−6", short enough for a button or a heatmap's column. */
    fun label(number: Int): String = when (end(number)) {
        IndexDirection.AT_OR_BELOW -> "≤"
        IndexDirection.AT_OR_ABOVE -> "≥"
        null -> ""
    } + signedCount(number)

    /** "+3", or for an open end "+6 or higher". */
    fun inPlainWords(number: Int): String = end(number)?.let { numberInPlainWords(number, it) } ?: signedCount(number)

    /** The way the pad runs on past [number], or null when it stands only for itself. */
    fun end(number: Int): IndexDirection? = when {
        !openEnded -> null
        number == numbers.first -> IndexDirection.AT_OR_BELOW
        number == numbers.last -> IndexDirection.AT_OR_ABOVE
        else -> null
    }
}
