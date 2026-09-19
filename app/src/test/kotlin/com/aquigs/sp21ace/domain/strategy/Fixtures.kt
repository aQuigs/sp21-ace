package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.HandTotal

object Fixtures {
    /** Every total a doubled hand can have: hard 6, from 2-2, and soft 13, from A-A, are the lowest a double reaches. */
    val doubledTotals: List<HandTotal> = (6..20).map { HandTotal(it, soft = false) } + (13..20).map { HandTotal(it, soft = true) }

    /** Every square of a rule set's fixture as its six fields: table, hand, upcard, play, debated, sources. */
    fun rows(ruleSet: RuleSet): List<List<String>> {
        val name = ruleSet.name.lowercase().replace('_', '-') + ".tsv"
        val text = requireNotNull(javaClass.getResource("/strategy/$name")) { "Missing fixture $name" }.readText()

        return text.lines().drop(1).filter(String::isNotBlank).map { line ->
            line.split('\t').also { require(it.size == 6) { "$name: expected 6 fields in \"$line\"" } }
        }
    }
}
