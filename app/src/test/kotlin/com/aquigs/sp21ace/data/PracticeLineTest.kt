package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class PracticeLineTest {
    private val wrongStand = PracticeAnswer(
        Instant.ofEpochMilli(1_789_000_000_000),
        RuleSet.H17_REDOUBLE,
        TrainerHand(cards("8h 8s"), card("6d")),
        answer = Move.STAND,
        correctMove = Move.SPLIT,
    )

    @Test
    fun printsEveryFieldByName() {
        // Saved lines must keep loading, so a change here has to read the old lines too
        assertEquals("at=1789000000000 rules=H17_REDOUBLE player=8h,8s upcard=6d answer=STAND correctMove=SPLIT", PracticeLine.print(wrongStand))
    }

    @Test
    fun readsBackEveryAnswerItPrints() {
        val answers = listOf(
            wrongStand,
            PracticeAnswer(Instant.ofEpochMilli(1_789_000_005_000), RuleSet.H17, TrainerHand(cards("Kc 6h"), card("As")), Move.SURRENDER, Move.SURRENDER),
            PracticeAnswer(Instant.ofEpochMilli(1_789_000_010_000), RuleSet.S17, TrainerHand(cards("As 6d"), card("Qh")), Move.DOUBLE, Move.HIT),
            // Suited and more than two cards, as the trainer will deal for card-count and bonus exceptions
            PracticeAnswer(Instant.ofEpochMilli(1_789_000_015_000), RuleSet.S17, TrainerHand(cards("2h 4h 7h"), card("4s")), Move.STAND, Move.HIT),
        )

        assertEquals(answers, answers.map { PracticeLine.parse(PracticeLine.print(it)) })
    }

    @Test
    fun aFieldAddedLaterDoesntStopALineLoading() {
        // Such as which decision was asked, once the trainer deals doubled hands
        assertEquals(wrongStand, PracticeLine.parse(PracticeLine.print(wrongStand).replace(" answer=", " decision=afterDoubling answer=")))
    }

    @Test
    fun aLineCutShortGarbledOrWithAFieldMissingOrTwiceReadsAsNothing() {
        val line = PracticeLine.print(wrongStand)

        for (cutShort in (0 until line.length).map(line::take)) {
            assertNull(cutShort, PracticeLine.parse(cutShort))
        }

        val garbled = listOf(
            "hello",
            // An unknown move, a hand past 21, and a 10-spot, which a Spanish deck doesn't have
            line.replace("STAND", "FOLD"),
            line.replace("8h,8s", "Kh,Qs,5d"),
            line.replace("6d", "10d"),
            // A field missing, a field twice, and a cut-short line run on into the next
            line.replace("upcard=6d ", ""),
            "$line answer=HIT",
            line.take(30) + line,
        )
        for (each in garbled) {
            assertNull(each, PracticeLine.parse(each))
        }
    }
}
