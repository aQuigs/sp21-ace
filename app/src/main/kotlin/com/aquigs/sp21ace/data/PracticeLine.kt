package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.cards.card
import com.aquigs.sp21ace.domain.cards.code
import com.aquigs.sp21ace.domain.history.PracticeAnswer
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import java.time.Instant

/**
 * One practice history line: named fields, such as
 * `at=1789000000000 rules=S17 player=9c,7d upcard=As doubled=false answer=HIT correctMove=HIT`.
 * Every card keeps its rank and suit, however many the hand holds, for card-count and bonus exceptions. Fields are found by
 * name, so a later one can join without breaking the lines already saved. `doubled` joined that way, and a line without it
 * predates doubled hands, so it was no doubled hand.
 */
internal object PracticeLine {
    fun print(answer: PracticeAnswer): String = listOf(
        "at" to answer.answeredAt.toEpochMilli(),
        "rules" to answer.ruleSet.name,
        "player" to answer.hand.player.joinToString(",") { it.code },
        "upcard" to answer.hand.upcard.code,
        "doubled" to answer.hand.doubled,
        "answer" to answer.answer.name,
        "correctMove" to answer.correctMove.name,
    ).joinToString(" ") { (key, value) -> "$key=$value" }

    /** Null for a line it can't read, such as one cut short when the app was killed mid-write. */
    fun parse(line: String): PracticeAnswer? = try {
        val fields = line.split(' ').groupBy({ it.substringBefore('=') }, { it.substringAfter('=') })
        // A field named twice, as in a cut-short line run on into the next, leaves no telling which answer is meant
        fun field(key: String) = fields.getValue(key).single()
        fun fieldIfSaved(key: String) = fields[key]?.single()

        PracticeAnswer(
            answeredAt = Instant.ofEpochMilli(field("at").toLong()),
            ruleSet = RuleSet.valueOf(field("rules")),
            hand = TrainerHand(
                field("player").split(',').map(::card),
                card(field("upcard")),
                doubled = fieldIfSaved("doubled")?.toBooleanStrict() ?: false,
            ),
            answer = Move.valueOf(field("answer")),
            correctMove = Move.valueOf(field("correctMove")),
        )
    } catch (e: RuntimeException) {
        null
    }
}
