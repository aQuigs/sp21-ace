package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.BUILT_IN_REVISION
import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.CountRounding
import com.aquigs.sp21ace.domain.indices.DeckEstimate
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.IndexCorrection
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Every preset as text, a [LineFields] line each: the active preset's id and the [BUILT_IN_REVISION] it was saved at first, then
 * each preset's line followed by a line for each of its entries, such as
 * ```
 * active=BJ_SHOE_H17 revision=2
 * preset=BJ_SHOE_H17 game=BLACKJACK deckEstimate=TRUNCATE rounding=FLOOR basedOn=BJ_SHOE_H17 name=Shoe%2C+H17
 * index=STAND:16:10:0:AT_OR_ABOVE on=true
 * ```
 * The name is URL-encoded, so it holds no space. A preset started empty has no `basedOn`. A convention missing, or one this
 * version doesn't know, reads as its default, and a built-in set it doesn't know as none. Text saved before the revision joined
 * reads as [IndexCorrection.FIRST_REVISION].
 */
internal object IndexPresetsCode {
    private const val ACTIVE = "active"
    private const val REVISION = "revision"
    private const val PRESET = "preset"
    private const val GAME = "game"
    private const val DECK_ESTIMATE = "deckEstimate"
    private const val ROUNDING = "rounding"
    private const val BASED_ON = "basedOn"
    private const val NAME = "name"
    private const val INDEX = "index"
    private const val ON = "on"
    private const val UTF_8 = "UTF-8"

    fun print(presets: IndexPresets): String = buildList {
        add(LineFields.print(ACTIVE to presets.activeId, REVISION to BUILT_IN_REVISION))
        presets.presets.forEach { preset ->
            val head = listOfNotNull(
                PRESET to preset.id,
                GAME to preset.game.name,
                DECK_ESTIMATE to preset.deckEstimate.name,
                ROUNDING to preset.rounding.name,
                preset.basedOn?.let { BASED_ON to it.name },
                NAME to URLEncoder.encode(preset.name, UTF_8),
            )
            add(LineFields.print(*head.toTypedArray()))
            preset.entries.forEach { add(LineFields.print(INDEX to IndexCode.print(it.index), ON to it.enabled)) }
        }
    }.joinToString("\n")

    /** What [stored] loads as, and whether it was [corrected], so it must be saved again to apply each correction only once. */
    data class Loaded(val presets: IndexPresets, val corrected: Boolean)

    /**
     * The presets [stored] holds, with every correction to a built-in set made since it was saved. Nothing stored, or nothing
     * readable, loads the seeded presets.
     */
    fun load(stored: String?): Loaded {
        val (presets, revision) = stored?.let(::read) ?: return Loaded(IndexPresets.seeded(), corrected = false)
        if (revision >= BUILT_IN_REVISION) return Loaded(presets, corrected = false)

        return Loaded(presets.corrected(since = revision), corrected = true)
    }

    fun parse(code: String): IndexPresets? = read(code)?.first

    /**
     * Null when no preset reads. A preset or entry it can't read, such as one a later version saved with a game or play this one
     * doesn't know, is left out rather than losing the rest. With the active preset left out, the first is active.
     */
    private fun read(code: String): Pair<IndexPresets, Int>? {
        val lines = code.lines().filter(String::isNotBlank)
        val head = lines.firstOrNull()?.let(::LineFields)
        val activeId = runCatching { head?.get(ACTIVE) }.getOrNull()
        val revision = runCatching { head?.ifSaved(REVISION)?.toInt() }.getOrNull() ?: IndexCorrection.FIRST_REVISION

        val blocks = mutableListOf<MutableList<String>>()
        for (line in lines) {
            if (line.startsWith("$PRESET=")) blocks += mutableListOf(line) else blocks.lastOrNull()?.add(line)
        }
        val presets = blocks.mapNotNull(::readPreset).distinctBy { it.id }
        if (presets.isEmpty()) return null

        return IndexPresets(presets, activeId?.takeIf { id -> presets.any { it.id == id } } ?: presets.first().id) to revision
    }

    private fun readPreset(block: List<String>): IndexPreset? = try {
        val fields = LineFields(block.first())

        IndexPreset(
            id = fields[PRESET],
            name = URLDecoder.decode(fields[NAME], UTF_8),
            game = Game.valueOf(fields[GAME]),
            deckEstimate = enumNamed(fields.ifSaved(DECK_ESTIMATE)) ?: DeckEstimate.DEFAULT,
            rounding = enumNamed(fields.ifSaved(ROUNDING)) ?: CountRounding.DEFAULT,
            // A set this version doesn't know leaves the user's own indices, so the preset reads as started empty rather than lost
            basedOn = enumNamed<BuiltInIndexSet>(fields.ifSaved(BASED_ON)),
            entries = block.drop(1).mapNotNull(::readEntry).distinctBy { it.index.deviation },
        )
    } catch (e: RuntimeException) {
        null
    }

    private fun readEntry(line: String): IndexEntry? = try {
        val fields = LineFields(line)
        IndexEntry(IndexCode.parse(fields[INDEX]), fields[ON].toBooleanStrict())
    } catch (e: RuntimeException) {
        null
    }
}
