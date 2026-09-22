package com.aquigs.sp21ace.domain.strategy

import org.junit.Assert.assertEquals
import org.junit.Test

class TableRulesTest {
    @Test
    fun theDefaultIsTheDealerStandingWithoutRedoublingOrInsurance() {
        assertEquals(TableRules(dealerHitsSoft17 = false, redoubling = false, insurance = false), TableRules())
    }

    @Test
    fun insuranceChangesNoChart() {
        for (rules in listOf(TableRules(), TableRules(dealerHitsSoft17 = true), TableRules(dealerHitsSoft17 = true, redoubling = true))) {
            assertEquals(rules.ruleSet, rules.copy(insurance = true).ruleSet)
        }
    }

    @Test
    fun theDealerStandingPicksTheS17ChartWhateverRedoubling() {
        assertEquals(RuleSet.S17, TableRules(dealerHitsSoft17 = false, redoubling = false).ruleSet)
        assertEquals(RuleSet.S17, TableRules(dealerHitsSoft17 = false, redoubling = true).ruleSet)
    }

    @Test
    fun theDealerHittingPicksTheH17ChartOrItsRedoublingOne() {
        assertEquals(RuleSet.H17, TableRules(dealerHitsSoft17 = true, redoubling = false).ruleSet)
        assertEquals(RuleSet.H17_REDOUBLE, TableRules(dealerHitsSoft17 = true, redoubling = true).ruleSet)
    }
}
