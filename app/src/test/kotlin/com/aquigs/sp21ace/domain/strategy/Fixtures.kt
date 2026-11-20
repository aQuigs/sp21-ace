package com.aquigs.sp21ace.domain.strategy

import com.aquigs.sp21ace.domain.cards.HandTotal

object Fixtures {
    /** Every total a doubled hand can have: hard 6, from 2-2, and soft 13, from A-A, are the lowest a double reaches. */
    val doubledTotals: List<HandTotal> = (6..20).map { HandTotal(it, soft = false) } + (13..20).map { HandTotal(it, soft = true) }

    /** Every square of a rule set's fixture as its six fields: table, hand, upcard, play, debated, sources. */
    fun rows(ruleSet: RuleSet): List<List<String>> = tsv("strategy", ruleSet).drop(1).onEach { require(it.size == 6) { "$ruleSet: expected 6 fields in $it" } }

    /** Every line of the fixture in [dir] named after [set], such as h17-redouble.tsv for H17_REDOUBLE, header first, split at its tabs. */
    fun tsv(dir: String, set: Enum<*>): List<List<String>> {
        val name = set.name.lowercase().replace('_', '-') + ".tsv"
        val text = requireNotNull(javaClass.getResource("/$dir/$name")) { "Missing fixture $name" }.readText()

        return text.lines().filter(String::isNotBlank).map { it.split('\t') }
    }
}
