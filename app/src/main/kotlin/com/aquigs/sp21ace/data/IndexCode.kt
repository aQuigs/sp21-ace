package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.DeviationPlay
import com.aquigs.sp21ace.domain.indices.IndexHand
import com.aquigs.sp21ace.domain.indices.PlayIndex
import com.aquigs.sp21ace.domain.strategy.Upcard

/**
 * An index as one token with no spaces, so it fits a [LineFields] field: play, hand, upcard and threshold, such as
 * `STAND:16:10:0`, `SPLIT:10-10:5:5` or `DOUBLE:A-8:6:-1`, and `INSURANCE:3` for insurance.
 */
internal object IndexCode {
    private const val INSURANCE = "INSURANCE"

    fun print(index: PlayIndex): String = when (val deviation = index.deviation) {
        is Deviation.OnHand -> listOf(deviation.play.name, deviation.hand.label, deviation.upcard.label, index.threshold)
        Deviation.Insurance -> listOf(INSURANCE, index.threshold)
    }.joinToString(":")

    /** Throws on a code it can't read. */
    fun parse(code: String): PlayIndex {
        val parts = code.split(':')
        val threshold = requireNotNull(parts.last().toIntOrNull()) { "Unknown index: $code" }
        val deviation = when {
            parts.size == 2 && parts[0] == INSURANCE -> Deviation.Insurance
            parts.size == 4 -> Deviation.OnHand(IndexHand.parse(parts[1]), Upcard.fromLabel(parts[2]), DeviationPlay.valueOf(parts[0]))
            else -> throw IllegalArgumentException("Unknown index: $code")
        }

        return PlayIndex(deviation, threshold)
    }
}
