package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.cards.Rank
import com.aquigs.sp21ace.domain.cards.Suit
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import java.time.Instant

/**
 * One practice history line: named fields, such as
 * `at=1789000000000 rules=S17 player=9c,7d upcard=As answer=HIT correctMove=HIT correct=true`.
 * Every card keeps its rank and suit, however many the hand holds, for card-count and bonus exceptions. Fields are found by
 * name, so a later one, such as which decision was asked once the trainer deals doubled hands, can join without breaking
 * the lines already saved, which predate it and so were all first decisions.
 */
internal object PracticeLine {
    private val SUIT_LETTERS = mapOf(Suit.SPADES to 's', Suit.HEARTS to 'h', Suit.DIAMONDS to 'd', Suit.CLUBS to 'c')

    fun print(answer: PracticeAnswer): String = listOf(
        "at" to answer.answeredAt.toEpochMilli(),
        "rules" to answer.ruleSet.name,
        "player" to answer.hand.player.joinToString(",") { it.code },
        "upcard" to answer.hand.upcard.code,
        "answer" to answer.answer.name,
        "correctMove" to answer.correctMove.name,
        "correct" to answer.isCorrect,
    ).joinToString(" ") { (key, value) -> "$key=$value" }

    /** Null for a line it can't read, such as one cut short when the app was killed mid-write. */
    fun parse(line: String): PracticeAnswer? = try {
        val fields = line.split(' ').associate { it.substringBefore('=') to it.substringAfter('=') }

        PracticeAnswer(
            answeredAt = Instant.ofEpochMilli(fields.getValue("at").toLong()),
            ruleSet = RuleSet.valueOf(fields.getValue("rules")),
            hand = TrainerHand(fields.getValue("player").split(',').map(::card), card(fields.getValue("upcard"))),
            answer = Move.valueOf(fields.getValue("answer")),
            correctMove = Move.valueOf(fields.getValue("correctMove")),
            isCorrect = fields.getValue("correct").toBooleanStrict(),
        )
    } catch (e: RuntimeException) {
        null
    }

    private val Card.code: String get() = "${rank.label}${SUIT_LETTERS.getValue(suit)}"

    private fun card(code: String) = Card(
        Rank.entries.first { it.label == code.dropLast(1) },
        SUIT_LETTERS.entries.first { it.value == code.last() }.key,
    )
}
