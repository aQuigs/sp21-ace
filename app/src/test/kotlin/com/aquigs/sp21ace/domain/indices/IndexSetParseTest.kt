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
        val header = "play hand upcard direction index\n"
        val bad = listOf(
            "STAND 18 2 higher +99",
            header + "STAND 18 2 +99",
            header + "STAND 18 2 higher high",
            header + "STAND 18 2 above +1",
            header + "FOLD 18 2 higher +1",
            header + "INSURANCE 16 A higher +1",
            header + "INSURANCE - 10 higher +1",
            header + "STAND 18 2 higher +1\nSTAND 18 2 lower +2",
        )

        for (rows in bad) {
            assertThrows(rows, IllegalArgumentException::class.java) { parseIndexSet(rows) }
        }
    }
}
