package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.FakeIndices
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IndexPresetsCodeTest {
    private val mine = IndexPreset(
        "mine",
        "Mine, = 100% & more\nlines",
        Game.BLACKJACK,
        basedOn = null,
        FakeIndices.ALL.mapIndexed { i, index -> IndexEntry(index, enabled = i != 1) },
    )
    private val presets = IndexPresets(IndexPresets.seeded().presets + mine, activeId = "mine")

    @Test
    fun printsEveryIndexAsOneTokenThatReadsBack() {
        // Saved presets and history lines must keep loading, so a change here has to read the old codes too
        assertEquals(
            listOf("STAND:20:2:99", "SPLIT:A-A:3:-99", "DOUBLE:A-9:10:0", "SURRENDER:5:9:98", "INSURANCE:-98"),
            FakeIndices.ALL.map(IndexCode::print),
        )
        assertEquals(FakeIndices.ALL, FakeIndices.ALL.map { IndexCode.parse(IndexCode.print(it)) })

        for (code in listOf("", "STAND", "STAND:20:2", "STAND:20:2:x", "HIT:20:2:1", "STAND:20:J:1", "INSURANCE:16:A:1", "INSURANCE")) {
            assertThrows(code, IllegalArgumentException::class.java) { IndexCode.parse(code) }
        }
    }

    @Test
    fun printsEveryPresetLineByLine() {
        val one = IndexPresets(listOf(mine.copy(name = "Mine", entries = mine.entries.take(2))), "mine")

        assertEquals(
            "active=mine\npreset=mine game=BLACKJACK name=Mine\nindex=STAND:20:2:99 on=true\nindex=SPLIT:A-A:3:-99 on=false",
            IndexPresetsCode.print(one),
        )
    }

    @Test
    fun readsBackEveryPresetItPrintsWithItsNameEntriesSwitchesAndActivePreset() {
        assertEquals(presets, IndexPresetsCode.parse(IndexPresetsCode.print(presets)))
        assertEquals(presets.activate(BuiltInIndexSet.DOUBLE_DECK.name), IndexPresetsCode.parse(IndexPresetsCode.print(presets.activate(BuiltInIndexSet.DOUBLE_DECK.name))))
    }

    @Test
    fun leavesOutAPresetOrEntryItCannotReadAndKeepsTheRest() {
        val code = IndexPresetsCode.print(presets)
        val withoutMine = presets.delete("mine").activate(presets.presets.first().id)

        // A game, a built-in set and a play a later version might save, and a cut-short entry
        assertEquals(withoutMine, IndexPresetsCode.parse(code.replace("preset=mine game=BLACKJACK", "preset=mine game=SPANISH_21")))
        assertEquals(withoutMine, IndexPresetsCode.parse(code.replace("preset=mine game=BLACKJACK", "preset=mine game=BLACKJACK basedOn=SIX_DECK_DAS")))
        assertEquals(
            presets.update("mine") { it.copy(entries = it.entries.drop(1)) },
            IndexPresetsCode.parse(code.replace("index=STAND:20:2:99 on=true", "index=HIT:20:2:99 on=true")),
        )
        assertEquals(
            presets.update("mine") { it.copy(entries = it.entries.drop(1)) },
            IndexPresetsCode.parse(code.replace("index=STAND:20:2:99 on=true", "index=STAND:20:2:99 on=")),
        )
    }

    @Test
    fun withTheActivePresetLeftOutTheFirstIsActive() {
        val code = IndexPresetsCode.print(presets)

        assertEquals(presets.activate(presets.presets.first().id), IndexPresetsCode.parse(code.replace("active=mine", "active=gone")))
        assertEquals(presets.activate(presets.presets.first().id), IndexPresetsCode.parse(code.lines().drop(1).joinToString("\n")))
    }

    @Test
    fun readsNothingFromTextWithNoPresetInIt() {
        for (code in listOf("", "active=mine", "hello", "active=mine\nindex=STAND:20:2:99 on=true", "active=x\npreset=x game=CHESS name=X")) {
            assertNull(code, IndexPresetsCode.parse(code))
        }
    }
}
