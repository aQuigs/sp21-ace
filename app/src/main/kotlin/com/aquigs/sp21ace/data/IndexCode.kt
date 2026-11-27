package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.IndexFact
import com.aquigs.sp21ace.domain.indices.NoIndex
import com.aquigs.sp21ace.domain.indices.PlayIndex

/**
 * An index, or that there is none, as one token with no spaces, so it fits a [LineFields] field: play, hand, upcard, threshold and
 * direction, such as `STAND:16:10:0:AT_OR_ABOVE` or `HIT:13:2:-1:AT_OR_BELOW`, `INSURANCE:3:AT_OR_ABOVE` for insurance and
 * `STAND:16:8:NONE:AT_OR_ABOVE` for no index.
 */
internal object IndexCode {
    private const val NONE = "NONE"

    /** A threshold or an answer, null where there is no index. */
    fun printNumber(number: Int?): String = number?.toString() ?: NONE

    fun parseNumber(text: String): Int? = if (text == NONE) null else text.toInt()

    fun print(fact: IndexFact): String = when (val deviation = fact.deviation) {
        is Deviation.OnHand -> listOf(deviation.play.name, deviation.hand.label, deviation.upcard.label)
        Deviation.Insurance -> listOf(Deviation.INSURANCE)
    }.plus(listOf(printNumber(fact.threshold), fact.direction.name)).joinToString(":")

    /** The index [code] is for. Throws on a code it can't read. */
    fun parse(code: String): PlayIndex = requireNotNull(parseFact(code) as? PlayIndex) { "No index: $code" }

    /** Throws on a code it can't read. */
    fun parseFact(code: String): IndexFact {
        val parts = code.split(':')
        val deviation = when (parts.size) {
            3 -> Deviation.Insurance.takeIf { parts[0] == Deviation.INSURANCE }
            5 -> Deviation.parse(parts[0], parts[1], parts[2]).takeIf { it != Deviation.Insurance }
            else -> null
        }
        requireNotNull(deviation) { "Unknown index: $code" }

        val threshold = parseNumber(parts[parts.size - 2])
        val direction = IndexDirection.valueOf(parts.last())

        return if (threshold != null) PlayIndex(deviation, threshold, direction)
        else NoIndex(requireNotNull(deviation as? Deviation.OnHand) { "Unknown index: $code" }, direction)
    }
}
