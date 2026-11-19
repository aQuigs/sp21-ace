package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexPresetsTest {
    private fun preset(id: String, vararg indices: PlayIndex) =
        IndexPreset(id, "Preset $id", Game.BLACKJACK, basedOn = null, entries = indices.map { IndexEntry(it) })

    private val three = IndexPresets(listOf(preset("a", FakeIndices.STAND), preset("b"), preset("c", FakeIndices.SPLIT)), activeId = "b")

    @Test
    fun aFirstLaunchSeedsAPresetOfEveryBuiltInSetWithDefaultConventionsAndShoeH17Active() {
        val seeded = IndexPresets.seeded()

        assertEquals(BuiltInIndexSet.entries, seeded.presets.map { it.basedOn })
        for (preset in seeded.presets) {
            val set = preset.basedOn!!
            assertEquals(set.title, preset.name)
            assertEquals(set.game, preset.game)
            assertEquals(set.indices.map { IndexEntry(it.index, enabled = true) }, preset.entries)
            assertEquals(DeckEstimate.DEFAULT, preset.deckEstimate)
            assertEquals(set.rounding, preset.rounding)
        }
        assertEquals(BuiltInIndexSet.BJ_SHOE_H17, seeded.active.basedOn)
    }

    @Test
    fun addsAPresetAtTheEndWithDefaultConventionsAndMakesItActive() {
        val empty = three.add("Mine", from = null, id = "new")
        assertEquals(listOf("a", "b", "c", "new"), empty.presets.map { it.id })
        assertEquals(IndexPreset("new", "Mine", Game.BLACKJACK, DeckEstimate.TRUNCATE, CountRounding.FLOOR, null, emptyList()), empty.active)

        val built = three.add("Copy of S17", from = BuiltInIndexSet.BJ_SHOE_S17, id = "new")
        assertEquals(IndexPreset.from(BuiltInIndexSet.BJ_SHOE_S17, "new", "Copy of S17"), built.active)

        assertNotEquals(three.add("x", null).activeId, three.add("x", null).activeId)
    }

    @Test
    fun renamesOnlyThatPresetAndKeepsTheActiveOne() {
        val renamed = three.rename("c", "Renamed")

        assertEquals(listOf("Preset a", "Preset b", "Renamed"), renamed.presets.map { it.name })
        assertEquals("b", renamed.activeId)
        assertThrows(IllegalArgumentException::class.java) { three.rename("c", " ") }
    }

    @Test
    fun duplicatesAPresetWithItsConventionsJustAfterItAndMakesTheCopyActive() {
        val original = three.update("a") { it.copy(deckEstimate = DeckEstimate.PERCENTILE_95, rounding = CountRounding.TRUNCATE) }
        val duplicated = original.duplicate("a", "Twin", newId = "twin")

        assertEquals(listOf("a", "twin", "b", "c"), duplicated.presets.map { it.id })
        assertEquals(original.presets[0].copy(id = "twin", name = "Twin"), duplicated.active)
    }

    @Test
    fun deletingTheActivePresetActivatesTheOneTakingItsPlace() {
        assertEquals(IndexPresets(listOf(three.presets[0], three.presets[2]), "c"), three.delete("b"))
        assertEquals("b", three.activate("c").delete("c").activeId)
        assertEquals("b", three.delete("a").activeId)
    }

    @Test
    fun theLastPresetCannotBeDeleted() {
        val one = three.delete("a").delete("c")

        assertFalse(one.canDelete)
        assertThrows(IllegalArgumentException::class.java) { one.delete("b") }
    }

    @Test
    fun activatesOnlyAPresetThatExists() {
        assertEquals("c", three.activate("c").active.id)
        assertThrows(IllegalArgumentException::class.java) { three.activate("z") }
        assertThrows(IllegalArgumentException::class.java) { three.rename("z", "Name") }
    }

    @Test
    fun anUpdateChangesOnlyThatPresetKeepingItsIdAndItsDeviationsUnique() {
        val changed = three.update("a") { it.copy(rounding = CountRounding.ROUND, entries = it.entries.map { entry -> entry.copy(enabled = false) }) }
        assertEquals(CountRounding.ROUND, changed.presets[0].rounding)
        assertEquals(emptyList<PlayIndex>(), changed.presets[0].enabledIndices)
        assertEquals(three.presets.drop(1), changed.presets.drop(1))

        assertThrows(IllegalArgumentException::class.java) { three.update("a") { it.copy(id = "z") } }
        assertThrows(IllegalArgumentException::class.java) {
            three.update("a") { it.copy(entries = it.entries + IndexEntry(FakeIndices.STAND.copy(threshold = 1))) }
        }
    }

    @Test
    fun aBuiltInNumberIsNeitherCustomNorDebatedUnlessItsSourcesDebateIt() {
        val s17 = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_S17, "s17")
        val debated = s17.entries.map { it.index }.filter(s17::isDebated)

        assertFalse(s17.entries.any { s17.isCustom(it.index) })
        assertEquals(BuiltInIndexSet.BJ_SHOE_S17.indices.filter { it.debated }.map { it.index }, debated)
        assertTrue(debated.isNotEmpty())
    }

    @Test
    fun anEditedNumberDirectionOrAnAddedIndexIsCustomAndNeverDebated() {
        val s17 = IndexPreset.from(BuiltInIndexSet.BJ_SHOE_S17, "s17")
        val debated = s17.entries.map { it.index }.first(s17::isDebated)
        val edits = listOf(
            debated.copy(threshold = debated.threshold + 1),
            debated.copy(direction = IndexDirection.AT_OR_BELOW),
            PlayIndex(Deviation.OnHand(IndexHand(HandKind.HARD, 13), Upcard.TWO, DeviationPlay.HIT), -2, IndexDirection.AT_OR_BELOW),
        )

        for (edit in edits) {
            assertTrue("$edit", s17.isCustom(edit))
            assertFalse("$edit", s17.isDebated(edit))
        }
    }

    @Test
    fun aPresetStartedEmptyHasOnlyCustomIndicesAndNoneDebated() {
        val mine = preset("m", *FakeIndices.ALL.toTypedArray())

        assertTrue(FakeIndices.ALL.all(mine::isCustom))
        assertFalse(FakeIndices.ALL.any(mine::isDebated))
    }

    @Test
    fun anIndexIsAddedAtTheEndSwitchedOnButNeverTwiceForOneDeviation() {
        val added = preset("p", FakeIndices.STAND).add(FakeIndices.HIT)

        assertEquals(listOf(IndexEntry(FakeIndices.STAND), IndexEntry(FakeIndices.HIT)), added.entries)
        assertTrue(added.has(FakeIndices.HIT.deviation))
        assertFalse(added.has(FakeIndices.SPLIT.deviation))
        assertThrows(IllegalArgumentException::class.java) { added.add(FakeIndices.HIT.copy(threshold = 0, direction = IndexDirection.AT_OR_ABOVE)) }
    }

    @Test
    fun anEditChangesTheNumberAndDirectionInPlaceKeepingItsSwitch() {
        val mine = preset("p", FakeIndices.STAND, FakeIndices.SPLIT, FakeIndices.HIT).switch(FakeIndices.SPLIT.deviation, enabled = false)
        val edited = FakeIndices.SPLIT.copy(threshold = 4, direction = IndexDirection.AT_OR_BELOW)

        assertEquals(
            listOf(IndexEntry(FakeIndices.STAND), IndexEntry(edited, enabled = false), IndexEntry(FakeIndices.HIT)),
            mine.edit(edited).entries,
        )
        assertThrows(IllegalArgumentException::class.java) { mine.edit(FakeIndices.DOUBLE) }
    }

    @Test
    fun aSwitchTurnsOnlyThatIndexOffAndOnAgain() {
        val mine = preset("p", FakeIndices.STAND, FakeIndices.SPLIT)

        assertEquals(listOf(FakeIndices.SPLIT), mine.switch(FakeIndices.STAND.deviation, enabled = false).enabledIndices)
        assertEquals(mine, mine.switch(FakeIndices.STAND.deviation, enabled = false).switch(FakeIndices.STAND.deviation, enabled = true))
        assertThrows(IllegalArgumentException::class.java) { mine.switch(FakeIndices.HIT.deviation, enabled = false) }
    }

    @Test
    fun aDeletedIndexLeavesTheRestInOrder() {
        val mine = preset("p", FakeIndices.STAND, FakeIndices.SPLIT, FakeIndices.HIT)

        assertEquals(listOf(FakeIndices.STAND, FakeIndices.HIT), mine.remove(FakeIndices.SPLIT.deviation).enabledIndices)
        assertEquals(emptyList<IndexEntry>(), preset("p", FakeIndices.STAND).remove(FakeIndices.STAND.deviation).entries)
        assertThrows(IllegalArgumentException::class.java) { mine.remove(FakeIndices.DOUBLE.deviation) }
    }

    @Test
    fun enabledIndicesKeepListOrderAndSkipTheSwitchedOff() {
        val mixed = IndexPreset("m", "Mixed", Game.BLACKJACK, basedOn = null, entries = FakeIndices.ALL.mapIndexed { i, index -> IndexEntry(index, enabled = i % 2 == 0) })

        assertEquals(listOf(FakeIndices.STAND, FakeIndices.DOUBLE, FakeIndices.HIT), mixed.enabledIndices)
    }
}
