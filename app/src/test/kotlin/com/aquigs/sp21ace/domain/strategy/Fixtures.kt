package com.aquigs.sp21ace.domain.strategy

object Fixtures {
    /** Every square of a rule set's fixture as its six fields: table, hand, upcard, play, debated, sources. */
    fun rows(ruleSet: RuleSet): List<List<String>> {
        val name = ruleSet.name.lowercase().replace('_', '-') + ".tsv"
        val text = requireNotNull(javaClass.getResource("/strategy/$name")) { "Missing fixture $name" }.readText()

        return text.lines().drop(1).filter(String::isNotBlank).map { line ->
            line.split('\t').also { require(it.size == 6) { "$name: expected 6 fields in \"$line\"" } }
        }
    }
}
