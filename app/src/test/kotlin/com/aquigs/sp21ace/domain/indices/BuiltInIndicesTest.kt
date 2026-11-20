package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Fixtures
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
            assertTrue("$set's fixture has no indices", expected.isNotEmpty())
            val actual = set.indices.associateBy { it.index.deviation }

            (expected.keys + actual.keys).filter { expected[it] != actual[it] }
                .map { "$set ${it.title}: fixture ${expected[it]}, set ${actual[it]}" }
        }

        assertEquals("Numbers that differ from their fixtures", emptyList<String>(), mismatches)
    }

    @Test
    fun eachRowsLastCorrectionEndsAtItsSetsNumberAndTheRevisionIsTheNewest() {
        for ((row, corrections) in INDEX_CORRECTIONS.groupBy { it.set to it.from.deviation }) {
            val (set, deviation) = row
            assertEquals("$set ${deviation.title}", set.find(deviation)?.index, corrections.maxBy { it.revision }.to)
        }
        assertEquals(INDEX_CORRECTIONS.maxOf { it.revision }, BUILT_IN_REVISION)
    }

    // Every row of a set's fixture. A row the sources dispute, status conflict, ships debated.
    private fun fixture(set: BuiltInIndexSet): List<BuiltInIndex> {
        val lines = Fixtures.tsv("indices", set)
        require(lines.first() == COLUMNS) { "$set: expected the columns $COLUMNS" }

        val rows = lines.drop(1).map { fields ->
            // A row with no note ends at its sources, since trailing whitespace is trimmed on commit
            require(fields.size in COLUMNS.size - 1..COLUMNS.size) { "$set: expected ${COLUMNS.size} fields in $fields" }
            val row = COLUMNS.zip(fields).toMap()
            require(row["rule_set"] == set.name && row["rounding"] == set.rounding.name) { "$set: another set's row $fields" }
            require(row.getValue("sources").isNotBlank()) { "$set: no source cited in $fields" }

            val deviation = Deviation.parse(row.getValue("play"), row.getValue("hand"), row.getValue("upcard"))
            val debated = when (row["status"]) {
                "conflict" -> true
                "agree", "single-source" -> false
                else -> error("$set: status must be agree, conflict or single-source in $fields")
            }

            BuiltInIndex(PlayIndex(deviation, row.getValue("index").toInt(), IndexDirection.valueOf(row.getValue("direction"))), debated)
        }

        return rows.also { require(it.distinctBy { row -> row.index.deviation }.size == it.size) { "$set lists a deviation twice" } }
    }

    private companion object {
        val COLUMNS = listOf("rule_set", "rounding", "hand", "upcard", "play", "direction", "index", "status", "sources", "note")
    }
}
