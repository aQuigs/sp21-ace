package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.strategy.Upcard

/**
 * An index as one token with no spaces, so it fits a [LineFields] field: play, hand, upcard, threshold and direction, such as
 * `STAND:16:10:0:AT_OR_ABOVE` or `HIT:13:2:-1:AT_OR_BELOW`, and `INSURANCE:3:AT_OR_ABOVE` for insurance. A code without its
 * direction, which no saved code has yet, reads as at or above, the direction every built-in index takes.
 */
internal object IndexCode {
    private const val INSURANCE = "INSURANCE"

    fun print(index: PlayIndex): String = when (val deviation = index.deviation) {
        is Deviation.OnHand -> listOf(deviation.play.name, deviation.hand.label, deviation.upcard.label)
        Deviation.Insurance -> listOf(INSURANCE)
    }.plus(listOf(index.threshold.toString(), index.direction.name)).joinToString(":")

    /** Throws on a code it can't read. */
    fun parse(code: String): PlayIndex {
        val parts = code.split(':')
        val handParts = if (parts.first() == INSURANCE) 1 else 3
        require(parts.size == handParts + 1 || parts.size == handParts + 2) { "Unknown index: $code" }

        val deviation = if (handParts == 1) {
            Deviation.Insurance
        } else {
            Deviation.OnHand(IndexHand.parse(parts[1]), Upcard.fromLabel(parts[2]), DeviationPlay.valueOf(parts[0]))
        }
        val threshold = requireNotNull(parts[handParts].toIntOrNull()) { "Unknown index: $code" }
        val direction = parts.getOrNull(handParts + 1)?.let(IndexDirection::valueOf) ?: IndexDirection.AT_OR_ABOVE

        return PlayIndex(deviation, threshold, direction)
    }
}
