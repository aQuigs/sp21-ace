package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.IndexDirection
import com.aquigs.sp21ace.domain.indices.PlayIndex

/**
 * An index as one token with no spaces, so it fits a [LineFields] field: play, hand, upcard, threshold and direction, such as
 * `STAND:16:10:0:AT_OR_ABOVE` or `HIT:13:2:-1:AT_OR_BELOW`, and `INSURANCE:3:AT_OR_ABOVE` for insurance.
 */
internal object IndexCode {
    fun print(index: PlayIndex): String = when (val deviation = index.deviation) {
        is Deviation.OnHand -> listOf(deviation.play.name, deviation.hand.label, deviation.upcard.label)
        Deviation.Insurance -> listOf(Deviation.INSURANCE)
    }.plus(listOf(index.threshold.toString(), index.direction.name)).joinToString(":")

    /** Throws on a code it can't read. */
    fun parse(code: String): PlayIndex {
        val parts = code.split(':')
        val deviation = when (parts.size) {
            3 -> Deviation.Insurance.takeIf { parts[0] == Deviation.INSURANCE }
            5 -> Deviation.parse(parts[0], parts[1], parts[2]).takeIf { it != Deviation.Insurance }
            else -> null
        }

        return PlayIndex(
            requireNotNull(deviation) { "Unknown index: $code" },
            requireNotNull(parts[parts.size - 2].toIntOrNull()) { "Unknown index: $code" },
            IndexDirection.valueOf(parts.last()),
        )
    }
}
