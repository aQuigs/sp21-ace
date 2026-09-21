package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.StrategyGrade
import com.aquigs.sp21ace.domain.history.PlayedHand
import com.aquigs.sp21ace.domain.strategy.RuleSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PlayLineTest {
    private val hand = PlayedHand(Instant.ofEpochMilli(1_789_000_000_000), RuleSet.H17_REDOUBLE, Outcome.LOSE, -3_750, StrategyGrade.CORRECT_WITH_HINTS)

    @Test
    fun aHandPrintsAsNamedFieldsAndReadsBackTheSame() {
        val line = PlayLine.print(hand)

        assertEquals("at=1789000000000 rules=H17_REDOUBLE outcome=LOSE net=-3750 strategy=CORRECT_WITH_HINTS", line)
        assertEquals(hand, PlayLine.parse(line))
    }

    @Test
    fun aFieldAddedLaterDoesntStopTheLineReading() {
        assertEquals(hand, PlayLine.parse(PlayLine.print(hand) + " cards=9c,7d"))
    }

    @Test
    fun aLineCutShortOrRunOnReadsAsNothing() {
        assertNull(PlayLine.parse("at=1789000000000 rules=H17_REDOUBLE outc"))
        assertNull(PlayLine.parse(PlayLine.print(hand) + PlayLine.print(hand)))
        assertNull(PlayLine.parse(""))
    }
}
