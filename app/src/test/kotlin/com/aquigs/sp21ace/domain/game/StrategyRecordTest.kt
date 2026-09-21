package com.aquigs.sp21ace.domain.game

import org.junit.Assert.assertEquals
import org.junit.Test

class StrategyRecordTest {
    @Test
    fun aHandIsGradedAsBlackjackAceGradesIt() {
        val none = StrategyRecord()

        assertEquals(StrategyGrade.NO_ACTION_REQUIRED, none.grade)
        assertEquals(StrategyGrade.CORRECT, none.withDecision(correct = true).withDecision(correct = true).grade)
        assertEquals(StrategyGrade.CORRECT_WITH_HINTS, none.copy(helped = true).withDecision(correct = true).withDecision(correct = true).grade)
        // A wrong decision outweighs help, before or after it
        assertEquals(StrategyGrade.INCORRECT, none.copy(helped = true).withDecision(correct = true).withDecision(correct = false).grade)
        assertEquals(StrategyGrade.INCORRECT, none.withDecision(correct = false).copy(helped = true).withDecision(correct = true).grade)
    }
}
