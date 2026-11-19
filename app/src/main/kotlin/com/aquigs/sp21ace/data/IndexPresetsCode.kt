package com.aquigs.sp21ace.data

import com.aquigs.sp21ace.domain.indices.BuiltInIndexSet
import com.aquigs.sp21ace.domain.indices.Game
import com.aquigs.sp21ace.domain.indices.IndexEntry
import com.aquigs.sp21ace.domain.indices.IndexPreset
import com.aquigs.sp21ace.domain.indices.IndexPresets
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Every preset as text, a [LineFields] line each: the active preset's id first, then each preset's line followed by a line for
 * each of its entries, such as
 * ```
 * active=SHOE_H17
 * preset=SHOE_H17 game=BLACKJACK basedOn=SHOE_H17 name=Shoe%2C+H17
 * index=STAND:16:10:0 on=true
 * ```
 * The name is URL-encoded, so it holds no space. A preset started empty has no `basedOn`.
 */
internal object IndexPresetsCode {
    private const val ACTIVE = "active"
    private const val PRESET = "preset"
    private const val GAME = "game"
    private const val BASED_ON = "basedOn"
    private const val NAME = "name"
    private const val INDEX = "index"
    private const val ON = "on"
    private const val UTF_8 = "UTF-8"

    fun print(presets: IndexPresets): String = buildList {
        add(LineFields.print(ACTIVE to presets.activeId))
        presets.presets.forEach { preset ->
            val basedOn = listOfNotNull(preset.basedOn?.let { BASED_ON to it.name })
            add(LineFields.print(PRESET to preset.id, GAME to preset.game.name, *basedOn.toTypedArray(), NAME to URLEncoder.encode(preset.name, UTF_8)))
            preset.entries.forEach { add(LineFields.print(INDEX to IndexCode.print(it.index), ON to it.enabled)) }
        }
    }.joinToString("\n")

    /**
     * Null when no preset reads. A preset or entry it can't read, such as one a later version saved with a game or play this one
     * doesn't know, is left out rather than losing the rest. With the active preset left out, the first is active.
     */
    fun parse(code: String): IndexPresets? {
        val lines = code.lines().filter(String::isNotBlank)
        val activeId = lines.firstOrNull()?.let { runCatching { LineFields(it)[ACTIVE] }.getOrNull() }

        val blocks = mutableListOf<MutableList<String>>()
        for (line in lines) {
            if (line.startsWith("$PRESET=")) blocks += mutableListOf(line) else blocks.lastOrNull()?.add(line)
        }
        val presets = blocks.mapNotNull(::readPreset).distinctBy { it.id }
        if (presets.isEmpty()) return null

        return IndexPresets(presets, activeId?.takeIf { id -> presets.any { it.id == id } } ?: presets.first().id)
    }

    private fun readPreset(block: List<String>): IndexPreset? = try {
        val fields = LineFields(block.first())

        IndexPreset(
            id = fields[PRESET],
            name = URLDecoder.decode(fields[NAME], UTF_8),
            game = Game.valueOf(fields[GAME]),
            basedOn = fields.ifSaved(BASED_ON)?.let(BuiltInIndexSet::valueOf),
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
