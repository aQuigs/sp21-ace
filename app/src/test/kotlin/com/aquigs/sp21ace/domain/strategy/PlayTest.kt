package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayTest {
    @Test
    fun parsesPlainPlays() {
        assertEquals(Play(Action.HIT), Play.parse("H"))
        assertEquals(Play(Action.SPLIT), Play.parse("P"))
        assertEquals(Play(Action.SURRENDER), Play.parse("R"))
        assertEquals(Play(Action.SURRENDER_OR_HIT), Play.parse("RH"))
    }

    @Test
    fun parsesCardCountsAndBonusMarks() {
        assertEquals(Play(Action.DOUBLE, hitWithCards = 4), Play.parse("D4"))
        assertEquals(Play(Action.STAND, hitWithCards = 4, bonusException = BonusException.ANY_678), Play.parse("S4*"))
        assertEquals(Play(Action.STAND, hitWithCards = 5, bonusException = BonusException.SUITED_678), Play.parse("S5'"))
        assertEquals(Play(Action.STAND, hitWithCards = 6, bonusException = BonusException.SPADED_678), Play.parse("S6\""))
        assertEquals(Play(Action.SPLIT, bonusException = BonusException.SUITED_777), Play.parse("P$"))
    }

    @Test
    fun marksDebatedSquares() {
        assertEquals(Play(Action.DOUBLE, hitWithCards = 3, debated = true), Play.parse("D3†"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownCodes() {
        Play.parse("X")
    }
}
