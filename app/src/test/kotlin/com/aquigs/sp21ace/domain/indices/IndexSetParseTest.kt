package com.aquigs.sp21ace.domain.indices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IndexSetParseTest {
    @Test
    fun readsEveryRowInOrderWithItsDebatedMark() {
        assertEquals(
            FakeIndices.ALL.map { BuiltInIndex(it, debated = it == FakeIndices.SPLIT) },
            parseIndexSet(FakeIndices.SET),
        )
    }

    @Test
    fun rejectsAMissingHeaderAMalformedRowOrADeviationTwice() {
        val header = "play hand upcard index\n"
        val bad = listOf(
            "STAND 20 2 +99",
            header + "STAND 20 2",
            header + "STAND 20 2 high",
            header + "HIT 20 2 +1",
            header + "INSURANCE 16 A +1",
            header + "INSURANCE - 10 +1",
            header + "STAND 20 2 +1\nSTAND 20 2 +2",
        )

        for (rows in bad) {
            assertThrows(rows, IllegalArgumentException::class.java) { parseIndexSet(rows) }
        }
    }
}
