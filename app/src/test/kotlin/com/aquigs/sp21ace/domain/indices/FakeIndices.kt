package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard

/** Made-up numbers no source prints, far past any real index, for tests of what holds whatever the numbers. */
object FakeIndices {
    const val SET = """
        play       hand   upcard  direction  index
        STAND      20     2       higher     +99
        SPLIT      A-A    3       higher     -99†
        DOUBLE     A-9    10      higher     0
        SURRENDER  5      9       higher     +98
        HIT        19     4       lower      -97
        INSURANCE  -      A       higher     -98
    """

    val STAND = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 20), Upcard.TWO, DeviationPlay.STAND), 99)
    val SPLIT = PlayIndex(Deviation.OnHand(IndexHand(HandKind.PAIR, 1), Upcard.THREE, DeviationPlay.SPLIT), -99)
    val DOUBLE = PlayIndex(Deviation.OnHand(IndexHand(HandKind.SOFT, 20), Upcard.TEN, DeviationPlay.DOUBLE), 0)
    val SURRENDER = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 5), Upcard.NINE, DeviationPlay.SURRENDER), 98)
    val HIT = PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 19), Upcard.FOUR, DeviationPlay.HIT), -97, IndexDirection.AT_OR_BELOW)
    val INSURANCE = PlayIndex(Deviation.Insurance, -98)

    val ALL = listOf(STAND, SPLIT, DOUBLE, SURRENDER, HIT, INSURANCE)
}
