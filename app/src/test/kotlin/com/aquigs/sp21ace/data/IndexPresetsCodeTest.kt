package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
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
        DeckEstimate.PERCENTILE_95,
        CountRounding.TRUNCATE,
        basedOn = null,
        FakeIndices.ALL.mapIndexed { i, index -> IndexEntry(index, enabled = i != 1) },
    )
    private val presets = IndexPresets(IndexPresets.seeded().presets + mine, activeId = "mine")

    @Test
    fun printsEveryIndexAsOneTokenThatReadsBack() {
        // Saved presets and history lines must keep loading, so a change here has to read the old codes too
        assertEquals(
            listOf(
                "STAND:20:2:99:AT_OR_ABOVE",
                "SPLIT:A-A:3:-99:AT_OR_ABOVE",
                "DOUBLE:A-9:10:0:AT_OR_ABOVE",
                "SURRENDER:5:9:98:AT_OR_ABOVE",
                "HIT:19:4:-97:AT_OR_BELOW",
                "INSURANCE:-98:AT_OR_ABOVE",
            ),
            FakeIndices.ALL.map(IndexCode::print),
        )
        assertEquals(FakeIndices.ALL, FakeIndices.ALL.map { IndexCode.parse(IndexCode.print(it)) })
    }

    @Test
    fun aTokenWithoutItsDirectionReadsAsAtOrAbove() {
        assertEquals(FakeIndices.STAND, IndexCode.parse("STAND:20:2:99"))
        assertEquals(FakeIndices.INSURANCE, IndexCode.parse("INSURANCE:-98"))
    }

    @Test
    fun readsNoIndexFromAGarbledToken() {
        val garbled = listOf(
            "", "STAND", "STAND:20:2", "STAND:20:2:x", "FOLD:20:2:1", "STAND:20:J:1", "STAND:20:2:1:SIDEWAYS", "STAND:20:2:1:AT_OR_ABOVE:x",
            "INSURANCE", "INSURANCE:16:A:1", "INSURANCE:1:AT_OR_ABOVE:x",
        )

        for (code in garbled) {
            assertThrows(code, IllegalArgumentException::class.java) { IndexCode.parse(code) }
        }
    }

    @Test
    fun printsEveryPresetLineByLine() {
        val one = IndexPresets(listOf(mine.copy(name = "Mine", entries = mine.entries.take(2))), "mine")

        assertEquals(
            listOf(
                "active=mine",
                "preset=mine game=BLACKJACK deckEstimate=PERCENTILE_95 rounding=TRUNCATE name=Mine",
                "index=STAND:20:2:99:AT_OR_ABOVE on=true",
                "index=SPLIT:A-A:3:-99:AT_OR_ABOVE on=false",
            ).joinToString("\n"),
            IndexPresetsCode.print(one),
        )
    }

    @Test
    fun readsBackEveryPresetItPrintsWithItsNameConventionsEntriesSwitchesAndActivePreset() {
        assertEquals(presets, IndexPresetsCode.parse(IndexPresetsCode.print(presets)))

        val s17Active = presets.activate(BuiltInIndexSet.BJ_SHOE_S17.name)
        assertEquals(s17Active, IndexPresetsCode.parse(IndexPresetsCode.print(s17Active)))
    }

    @Test
    fun aConventionMissingOrUnknownReadsAsItsDefault() {
        val code = IndexPresetsCode.print(presets)
        val defaults = presets.update("mine") { it.copy(deckEstimate = DeckEstimate.DEFAULT, rounding = CountRounding.DEFAULT) }

        assertEquals(defaults, IndexPresetsCode.parse(code.replace(" deckEstimate=PERCENTILE_95 rounding=TRUNCATE", "")))
        assertEquals(defaults, IndexPresetsCode.parse(code.replace("deckEstimate=PERCENTILE_95 rounding=TRUNCATE", "deckEstimate=HALF rounding=STATISTICAL")))
    }

    @Test
    fun leavesOutAPresetOrEntryItCannotReadAndKeepsTheRest() {
        val code = IndexPresetsCode.print(presets)
        val withoutMine = presets.delete("mine").activate(presets.presets.first().id)
        val withoutStand = presets.update("mine") { it.copy(entries = it.entries.drop(1)) }

        // A game, a built-in set and a play a later version might save, and a cut-short entry
        assertEquals(withoutMine, IndexPresetsCode.parse(code.replace("preset=mine game=BLACKJACK", "preset=mine game=SPANISH_21")))
        assertEquals(withoutMine, IndexPresetsCode.parse(code.replace("rounding=TRUNCATE name=", "rounding=TRUNCATE basedOn=BJ_DOUBLE_DECK name=")))
        assertEquals(withoutStand, IndexPresetsCode.parse(code.replace("index=STAND:20:2:99:AT_OR_ABOVE on=true", "index=FOLD:20:2:99:AT_OR_ABOVE on=true")))
        assertEquals(withoutStand, IndexPresetsCode.parse(code.replace("index=STAND:20:2:99:AT_OR_ABOVE on=true", "index=STAND:20:2:99:AT_OR_ABOVE on=")))
    }

    @Test
    fun withTheActivePresetLeftOutTheFirstIsActive() {
        val code = IndexPresetsCode.print(presets)
        val firstActive = presets.activate(presets.presets.first().id)

        assertEquals(firstActive, IndexPresetsCode.parse(code.replace("active=mine", "active=gone")))
        assertEquals(firstActive, IndexPresetsCode.parse(code.lines().drop(1).joinToString("\n")))
    }

    @Test
    fun readsNothingFromTextWithNoPresetInIt() {
        for (code in listOf("", "active=mine", "hello", "active=mine\nindex=STAND:20:2:99 on=true", "active=x\npreset=x game=CHESS name=X")) {
            assertNull(code, IndexPresetsCode.parse(code))
        }
    }
}
