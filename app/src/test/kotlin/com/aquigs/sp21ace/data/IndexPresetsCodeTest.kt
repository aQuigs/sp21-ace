package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.BUILT_IN_REVISION
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
import com.aquigs.sp21ace.domain.indices.Deviation
import com.aquigs.sp21ace.domain.indices.FakeIndices
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.INDEX_CORRECTIONS
import com.aquigs.sp21ace.domain.indices.IndexCorrection
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import com.aquigs.sp21ace.domain.indices.PlayIndex
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
                "STAND:18:2:99:AT_OR_ABOVE",
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
    fun readsNoIndexFromAGarbledToken() {
        val garbled = listOf(
            "", "STAND", "STAND:18:2", "STAND:18:2:99", "STAND:18:2:x:AT_OR_ABOVE", "FOLD:18:2:1", "STAND:20:J:1", "STAND:18:2:1:SIDEWAYS", "STAND:18:2:1:AT_OR_ABOVE:x",
            "INSURANCE", "INSURANCE:-98", "INSURANCE:16:A:1:AT_OR_ABOVE", "INSURANCE:-:A:1:AT_OR_ABOVE", "INSURANCE:1:AT_OR_ABOVE:x",
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
                "active=mine revision=$BUILT_IN_REVISION",
                "preset=mine game=BLACKJACK deckEstimate=PERCENTILE_95 rounding=TRUNCATE name=Mine",
                "index=STAND:18:2:99:AT_OR_ABOVE on=true",
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

        // A game and a play a later version might save, and a cut-short entry
        assertEquals(withoutMine, IndexPresetsCode.parse(code.replace("preset=mine game=BLACKJACK", "preset=mine game=SPANISH_21")))
        assertEquals(withoutStand, IndexPresetsCode.parse(code.replace("index=STAND:18:2:99:AT_OR_ABOVE on=true", "index=FOLD:18:2:99:AT_OR_ABOVE on=true")))
        assertEquals(withoutStand, IndexPresetsCode.parse(code.replace("index=STAND:18:2:99:AT_OR_ABOVE on=true", "index=STAND:18:2:99:AT_OR_ABOVE on=")))
    }

    @Test
    fun aBuiltInSetItDoesNotKnowReadsAsNoneAndKeepsThePresetsIndices() {
        val code = IndexPresetsCode.print(presets)
        val unknownSet = presets.update(BuiltInIndexSet.BJ_SHOE_S17.name) { it.copy(basedOn = null) }

        assertEquals(unknownSet, IndexPresetsCode.parse(code.replace("basedOn=BJ_SHOE_S17", "basedOn=BJ_DOUBLE_DECK")))
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
        for (code in listOf("", "active=mine", "hello", "active=mine\nindex=STAND:18:2:99 on=true", "active=x\npreset=x game=CHESS name=X")) {
            assertNull(code, IndexPresetsCode.parse(code))
        }
    }

    // The seeded presets as a build at [revision] saved them: every row corrected since holds the number it had then
    private fun seededAt(revision: Int): IndexPresets = INDEX_CORRECTIONS
        .filter { it.revision > revision }
        .groupBy { it.set to it.from.deviation }
        .map { (_, corrections) -> corrections.minBy { it.revision } }
        .fold(IndexPresets.seeded()) { presets, correction -> presets.update(correction.set.name) { it.edit(correction.from) } }

    private fun savedAt(presets: IndexPresets, revision: Int?) =
        IndexPresetsCode.print(presets).replace(" revision=$BUILT_IN_REVISION", revision?.let { " revision=$it" }.orEmpty())

    private val firstRelease = seededAt(IndexCorrection.FIRST_REVISION)

    @Test
    fun presetsSavedWithNoRevisionOrAnOlderOneTakeTheCorrectionsSinceAndMustBeSavedAgain() {
        val stored = listOf(savedAt(firstRelease, revision = null)) +
            (IndexCorrection.FIRST_REVISION until BUILT_IN_REVISION).map { savedAt(seededAt(it), it) }

        for (code in stored) {
            assertEquals(code, IndexPresetsCode.Loaded(IndexPresets.seeded(), corrected = true), IndexPresetsCode.load(code))
        }
    }

    // The seeded presets with H17's 12 vs 3, 12 vs 6, 13 vs 3 and 11 vs A at these numbers
    private fun h17At(twelveVsThree: Int, twelveVsSix: Int, thirteenVsThree: Int, elevenVsAce: Int): IndexPresets {
        val numbers = listOf(
            Deviation.parse("STAND", "12", "3") to twelveVsThree,
            Deviation.parse("STAND", "12", "6") to twelveVsSix,
            Deviation.parse("STAND", "13", "3") to thirteenVsThree,
            Deviation.parse("DOUBLE", "11", "A") to elevenVsAce,
        )
        return IndexPresets.seeded().update(BuiltInIndexSet.BJ_SHOE_H17.name) { preset ->
            numbers.fold(preset) { edited, (deviation, number) -> edited.edit(PlayIndex(deviation, number)) }
        }
    }

    private val shipped = h17At(twelveVsThree = 1, twelveVsSix = -4, thirteenVsThree = -3, elevenVsAce = 0)

    @Test
    fun theShippedH17NumbersAreTheOnesTheUpgradesLoad() {
        assertEquals(IndexPresets.seeded(), shipped)
    }

    @Test
    fun presetsMainSavedAtRevision2TakeTheThreeRevision3Corrections() {
        val main = savedAt(h17At(twelveVsThree = 1, twelveVsSix = -3, thirteenVsThree = -2, elevenVsAce = -1), revision = 2)

        assertEquals(IndexPresetsCode.Loaded(shipped, corrected = true), IndexPresetsCode.load(main))
    }

    @Test
    fun presetsSavedBeforeAnyRevisionTakeAllFourCorrectionsInOneLoad() {
        val beforeRevisions = savedAt(h17At(twelveVsThree = 2, twelveVsSix = -3, thirteenVsThree = -2, elevenVsAce = -1), revision = null)

        assertEquals(IndexPresetsCode.Loaded(shipped, corrected = true), IndexPresetsCode.load(beforeRevisions))
    }

    @Test
    fun aUsersOwnOldNumberSavedAtRevision3Stays() {
        val own = h17At(twelveVsThree = 1, twelveVsSix = -3, thirteenVsThree = -3, elevenVsAce = 0)

        assertEquals(IndexPresetsCode.Loaded(own, corrected = false), IndexPresetsCode.load(savedAt(own, revision = 3)))
    }

    @Test
    fun presetsSavedAtTheCurrentRevisionLoadAsSavedSoAUsersOwnOldNumberStays() {
        assertEquals(IndexPresetsCode.Loaded(firstRelease, corrected = false), IndexPresetsCode.load(IndexPresetsCode.print(firstRelease)))
    }

    @Test
    fun nothingStoredOrNothingReadableLoadsTheSeededPresets() {
        for (stored in listOf(null, "garbled")) {
            assertEquals(IndexPresetsCode.Loaded(IndexPresets.seeded(), corrected = false), IndexPresetsCode.load(stored))
        }
    }
}
