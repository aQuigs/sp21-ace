package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// The fixtures list every built-in number with the sources that print it, so a number, play, direction or debated mark can't
// change without its fixture row, and every number cites a source
class BuiltInIndicesTest {
    @Test
    fun everySetMatchesItsFixture() {
        val mismatches = BuiltInIndexSet.entries.flatMap { set ->
            val expected = fixture(set).associateBy { it.index.deviation }
            val actual = set.indices.associateBy { it.index.deviation }

            (expected.keys + actual.keys).filter { expected[it] != actual[it] }
                .map { "$set ${it.title}: fixture ${expected[it]}, set ${actual[it]}" }
        }

        assertEquals("Numbers that differ from their fixtures", emptyList<String>(), mismatches)
    }

    @Test
    fun everySetHasNumbers() {
        for (set in BuiltInIndexSet.entries) {
            assertTrue("$set has no indices", set.indices.isNotEmpty())
        }
    }

    // Every row of a set's fixture. A row the sources dispute, status conflict, ships debated.
    private fun fixture(set: BuiltInIndexSet): List<BuiltInIndex> {
        val name = set.name.lowercase().replace('_', '-') + ".tsv"
        val text = requireNotNull(javaClass.getResource("/indices/$name")) { "Missing fixture $name" }.readText()
        val lines = text.lines().filter(String::isNotBlank)
        require(lines.first() == COLUMNS.joinToString("\t")) { "$name: expected the columns $COLUMNS" }

        val rows = lines.drop(1).map { line ->
            // A row with no note ends at its sources, since trailing whitespace is trimmed on commit
            val fields = line.split('\t').also { require(it.size in COLUMNS.size - 1..COLUMNS.size) { "$name: expected ${COLUMNS.size} fields in \"$line\"" } }
            val row = COLUMNS.zip(fields).toMap()
            require(row["rule_set"] == set.name && row["rounding"] == set.rounding.name) { "$name: another set's row \"$line\"" }
            require(row.getValue("sources").isNotBlank()) { "$name: no source cited in \"$line\"" }

            val play = row.getValue("play")
            val deviation = if (play == "INSURANCE") {
                require(row["hand"] == "-" && row["upcard"] == "A") { "$name: insurance has no hand and a dealer ace in \"$line\"" }
                Deviation.Insurance
            } else {
                Deviation.OnHand(IndexHand.parse(row.getValue("hand")), Upcard.fromLabel(row.getValue("upcard")), DeviationPlay.valueOf(play))
            }
            val debated = when (row["status"]) {
                "conflict" -> true
                "agree", "single-source" -> false
                else -> error("$name: status must be agree, conflict or single-source in \"$line\"")
            }

            BuiltInIndex(PlayIndex(deviation, row.getValue("index").toInt(), IndexDirection.valueOf(row.getValue("direction"))), debated)
        }

        return rows.also { require(it.distinctBy { row -> row.index.deviation }.size == it.size) { "$name lists a deviation twice" } }
    }

    private companion object {
        val COLUMNS = listOf("rule_set", "rounding", "hand", "upcard", "play", "direction", "index", "status", "sources", "note")
    }
}
