package com.aquigs.sp21ace.domain.trainer

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.strategy.Action
import com.aquigs.sp21ace.domain.strategy.BonusException
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.Play
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import org.junit.Assert.assertEquals
import org.junit.Test

class TrainerTest {
    private val s17 = StrategyCharts.forRules(RuleSet.S17)
    private val sixteenVsAce = TrainerHand(cards("9c 7d"), card("As"))
    private val softSeventeenVsTen = TrainerHand(cards("As 6d"), card("Kh"))

    @Test
    fun gradesARightAnswer() {
        val trainer = Trainer(s17) { sixteenVsAce }

        assertEquals(Grade(sixteenVsAce, Play(Action.HIT), Move.HIT, isCorrect = true), trainer.answer(Move.HIT))
    }

    @Test
    fun gradesAWrongAnswerWithTheMoveTheSquareCallsFor() {
        // Hard 14 vs 4 is S4*, so a 6-8 hits while the 6-7-8 bonus is possible
        val sixEightVsFour = TrainerHand(cards("6c 8d"), card("4s"))
        val trainer = Trainer(s17) { sixEightVsFour }

        assertEquals(
            Grade(sixEightVsFour, Play(Action.STAND, hitWithCards = 4, bonusException = BonusException.ANY_678), Move.HIT, isCorrect = false),
            trainer.answer(Move.STAND),
        )
    }

    @Test
    fun dealsTheNextHandOnceAnswered() {
        val trainer = Trainer(s17, listOf(sixteenVsAce, softSeventeenVsTen).iterator()::next)
        assertEquals(sixteenVsAce, trainer.hand)

        trainer.answer(Move.STAND)

        assertEquals(softSeventeenVsTen, trainer.hand)
    }
}
