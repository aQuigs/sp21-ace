package com.aquigs.sp21ace.domain.indices

import java.util.UUID

/**
 * A named list of indices for one [game], to drill and swap between. [deckEstimate] and [rounding] are how the player computes
 * the true count the numbers are for; they change no number. [basedOn] is the built-in set it started from, or null for one
 * started empty, and tells the indices the user changed or added from the published ones. No deviation is listed twice.
 */
data class IndexPreset(
    val id: String,
    val name: String,
    val game: Game,
    val deckEstimate: DeckEstimate = DeckEstimate.DEFAULT,
    val rounding: CountRounding = CountRounding.DEFAULT,
    val basedOn: BuiltInIndexSet?,
    val entries: List<IndexEntry>,
) {
    init {
        require(name.isNotBlank()) { "A preset needs a name" }
        require(basedOn == null || basedOn.game == game) { "$name is for $game but based on $basedOn" }
        require(entries.distinctBy { it.index.deviation }.size == entries.size) { "$name lists a deviation twice" }
    }

    /** The indices a run drills, in list order. */
    val enabledIndices: List<PlayIndex> get() = entries.filter { it.enabled }.map { it.index }

    /** Whether [index] is the user's own: a number or direction [basedOn] doesn't print for its deviation, or a deviation it doesn't have. */
    fun isCustom(index: PlayIndex): Boolean = basedOn?.find(index.deviation)?.index != index

    /** Whether [index] is [basedOn]'s own number and one the sources debate. */
    fun isDebated(index: PlayIndex): Boolean = basedOn?.find(index.deviation)?.let { it.debated && it.index == index } == true

    /** Whether an index on [deviation] is listed, which makes adding another for it impossible. */
    fun has(deviation: Deviation): Boolean = entries.any { it.index.deviation == deviation }

    /** Adds [index], switched on, at the end. Its deviation mustn't be listed yet. */
    fun add(index: PlayIndex): IndexPreset = copy(entries = entries + IndexEntry(index))

    /** Puts [index] in place of the listed index on its deviation, where that is listed and switched on or off as that is. */
    fun edit(index: PlayIndex): IndexPreset = change(index.deviation) { it.copy(index = index) }

    fun switch(deviation: Deviation, enabled: Boolean): IndexPreset = change(deviation) { it.copy(enabled = enabled) }

    fun remove(deviation: Deviation): IndexPreset = copy(entries = entries.filterNot { it.index.deviation == deviation }).also {
        require(it.entries.size < entries.size) { "$name has no index on $deviation" }
    }

    private fun change(deviation: Deviation, change: (IndexEntry) -> IndexEntry): IndexPreset {
        require(has(deviation)) { "$name has no index on $deviation" }
        return copy(entries = entries.map { if (it.index.deviation == deviation) change(it) else it })
    }

    companion object {
        /** A preset of every index in [set], all enabled, with the rounding its numbers assume and the default deck estimate. */
        fun from(set: BuiltInIndexSet, id: String, name: String = set.title): IndexPreset =
            IndexPreset(id, name, set.game, rounding = set.rounding, basedOn = set, entries = set.indices.map { IndexEntry(it.index) })
    }
}

private fun newPresetId(): String = UUID.randomUUID().toString()

/**
 * Every preset, in the order listed, and the one the flashcards drill. Never empty, and every change keeps it so. Each change
 * returns the presets it leaves, so the caller saves the result.
 */
data class IndexPresets(val presets: List<IndexPreset>, val activeId: String) {
    init {
        require(presets.isNotEmpty()) { "There is always a preset" }
        require(presets.distinctBy { it.id }.size == presets.size) { "Two presets share an id" }
        require(presets.any { it.id == activeId }) { "No preset $activeId" }
    }

    val active: IndexPreset get() = presets.first { it.id == activeId }

    /** The last preset can't be deleted. */
    val canDelete: Boolean get() = presets.size > 1

    fun activate(id: String): IndexPresets = copy(activeId = id)

    /**
     * Adds a preset of every index in [from], as [IndexPreset.from] makes it, or an empty one with the default conventions, at the
     * end, and makes it active to edit.
     */
    fun add(name: String, from: BuiltInIndexSet?, id: String = newPresetId()): IndexPresets {
        val preset = from?.let { IndexPreset.from(it, id, name) } ?: IndexPreset(id, name, Game.BLACKJACK, basedOn = null, entries = emptyList())
        return IndexPresets(presets + preset, preset.id)
    }

    fun rename(id: String, name: String): IndexPresets = update(id) { it.copy(name = name) }

    /** Copies preset [id], conventions and all, as [name] just after it, and makes the copy active to edit. */
    fun duplicate(id: String, name: String, newId: String = newPresetId()): IndexPresets {
        val at = indexOf(id)
        val copy = presets[at].copy(id = newId, name = name)
        return IndexPresets(presets.toMutableList().apply { add(at + 1, copy) }, newId)
    }

    /** Deletes preset [id], unless it's the last. Deleting the active one activates the preset that takes its place in the list. */
    fun delete(id: String): IndexPresets {
        require(canDelete) { "The last preset can't be deleted" }

        val at = indexOf(id)
        val left = presets.filterNot { it.id == id }
        return IndexPresets(left, if (id == activeId) left[at.coerceAtMost(left.lastIndex)].id else activeId)
    }

    /** Replaces preset [id] with what [change] makes of it, which keeps its id, such as its entries or conventions changed. */
    fun update(id: String, change: (IndexPreset) -> IndexPreset): IndexPresets {
        val at = indexOf(id)
        val changed = change(presets[at])
        require(changed.id == id) { "A preset keeps its id" }
        return copy(presets = presets.toMutableList().apply { set(at, changed) })
    }

    private fun indexOf(id: String): Int = presets.indexOfFirst { it.id == id }.also { require(it >= 0) { "No preset $id" } }

    companion object {
        /** The presets of a first launch: one per built-in set as [IndexPreset.from] makes it, with its name as id, and shoe H17 active. */
        fun seeded(): IndexPresets =
            IndexPresets(BuiltInIndexSet.entries.map { IndexPreset.from(it, id = it.name) }, activeId = BuiltInIndexSet.BJ_SHOE_H17.name)
    }
}
