package com.aquigs.sp21ace.domain.indices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexPresetsTest {
    private fun preset(id: String, vararg indices: PlayIndex) = IndexPreset(id, "Preset $id", Game.BLACKJACK, basedOn = null, indices.map { IndexEntry(it) })

    private val three = IndexPresets(listOf(preset("a", FakeIndices.STAND), preset("b"), preset("c", FakeIndices.SPLIT)), activeId = "b")

    @Test
    fun aFirstLaunchSeedsAPresetOfEveryBuiltInSetWithShoeH17Active() {
        val seeded = IndexPresets.seeded()

        assertEquals(BuiltInIndexSet.entries, seeded.presets.map { it.basedOn })
        for (preset in seeded.presets) {
            val set = preset.basedOn!!
            assertEquals(set.title, preset.name)
            assertEquals(set.game, preset.game)
            assertEquals(set.indices.map { IndexEntry(it.index, enabled = true) }, preset.entries)
        }
        assertEquals(BuiltInIndexSet.SHOE_H17, seeded.active.basedOn)
    }

    @Test
    fun addsAPresetAtTheEndAndMakesItActive() {
        val empty = three.add("Mine", from = null, id = "new")
        assertEquals(listOf("a", "b", "c", "new"), empty.presets.map { it.id })
        assertEquals(IndexPreset("new", "Mine", Game.BLACKJACK, basedOn = null, emptyList()), empty.active)

        val built = three.add("Copy of S17", from = BuiltInIndexSet.SHOE_S17, id = "new")
        assertEquals(IndexPreset.from(BuiltInIndexSet.SHOE_S17, "new", "Copy of S17"), built.active)

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
    fun duplicatesAPresetJustAfterItAndMakesTheCopyActive() {
        val duplicated = three.duplicate("a", "Twin", newId = "twin")

        assertEquals(listOf("a", "twin", "b", "c"), duplicated.presets.map { it.id })
        assertEquals(three.presets[0].copy(id = "twin", name = "Twin"), duplicated.active)
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
    fun anUpdateKeepsThePresetsIdAndItsDeviationsUnique() {
        val switchedOff = three.update("a") { it.copy(entries = it.entries.map { entry -> entry.copy(enabled = false) }) }
        assertEquals(emptyList<PlayIndex>(), switchedOff.presets[0].enabledIndices)

        assertThrows(IllegalArgumentException::class.java) { three.update("a") { it.copy(id = "z") } }
        assertThrows(IllegalArgumentException::class.java) {
            three.update("a") { it.copy(entries = it.entries + IndexEntry(FakeIndices.STAND.copy(threshold = 1))) }
        }
    }

    @Test
    fun aPresetStartedEmptyHasOnlyCustomIndicesAndNoneDebated() {
        val mine = preset("m", *FakeIndices.ALL.toTypedArray())

        assertTrue(FakeIndices.ALL.all(mine::isCustom))
        assertFalse(FakeIndices.ALL.any(mine::isDebated))
    }

    @Test
    fun enabledIndicesKeepListOrderAndSkipTheSwitchedOff() {
        val mixed = IndexPreset("m", "Mixed", Game.BLACKJACK, null, FakeIndices.ALL.mapIndexed { i, index -> IndexEntry(index, enabled = i % 2 == 0) })

        assertEquals(listOf(FakeIndices.STAND, FakeIndices.DOUBLE, FakeIndices.INSURANCE), mixed.enabledIndices)
    }
}
