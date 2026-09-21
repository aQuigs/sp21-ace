package com.aquigs.sp21ace.domain.game

import org.junit.Assert.assertEquals
import org.junit.Test

class StrategyRecordTest {
    @Test
    fun aHandIsGradedAsBlackjackAceGradesIt() {
        val none = StrategyRecord()

        assertEquals(StrategyGrade.NO_ACTION_REQUIRED, none.grade)
        assertEquals(StrategyGrade.CORRECT, none.withDecision(correct = true, helped = false).withDecision(correct = true, helped = false).grade)
        assertEquals(StrategyGrade.CORRECT_WITH_HINTS, none.withDecision(correct = true, helped = true).withDecision(correct = true, helped = false).grade)
        // A wrong decision outweighs help, before or after it
        assertEquals(StrategyGrade.INCORRECT, none.withDecision(correct = true, helped = true).withDecision(correct = false, helped = false).grade)
        assertEquals(StrategyGrade.INCORRECT, none.withDecision(correct = false, helped = false).withDecision(correct = true, helped = true).grade)
    }
}
