package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.DEBATED_MARK

/** A published index, [debated] where the sources disagree and the app ships the recommended number. */
data class BuiltInIndex(val index: PlayIndex, val debated: Boolean = false)

/**
 * The sourced index sets, one for each rule set with published numbers: Hi-Lo's Illustrious 18 and Fab 4 for a six-deck shoe.
 * [rounding] is the true count rounding the numbers were published for; no source states a deck estimate.
 */
enum class BuiltInIndexSet(val game: Game, val title: String, val rounding: CountRounding) {
    BJ_SHOE_H17(Game.BLACKJACK, "Shoe, H17", CountRounding.FLOOR),
    BJ_SHOE_S17(Game.BLACKJACK, "Shoe, S17", CountRounding.FLOOR),
    ;

    /** In the order Wizard of Odds lists them: the Illustrious 18, then the Fab 4. */
    val indices: List<BuiltInIndex> by lazy { parseIndexSet(INDEX_SETS.getValue(this)) }

    /** The set's number for [deviation], or null when it has none. */
    fun find(deviation: Deviation): BuiltInIndex? = indices.firstOrNull { it.index.deviation == deviation }
}

/**
 * A built-in number changed in [revision] of the built-in sets: a preset based on [set] that still holds [from], saved before that
 * revision, takes [to].
 */
internal data class IndexCorrection(val revision: Int, val set: BuiltInIndexSet, val from: PlayIndex, val to: PlayIndex) {
    init {
        require(revision > FIRST_REVISION && from.deviation == to.deviation && from != to) { "Not a correction: $this" }
    }

    companion object {
        /** The revision presets saved before any correction carry. */
        const val FIRST_REVISION = 1

        internal fun of(revision: Int, set: BuiltInIndexSet, play: String, hand: String, upcard: String, from: Int, to: Int): IndexCorrection {
            val deviation = Deviation.parse(play, hand, upcard)
            return IndexCorrection(revision, set, PlayIndex(deviation, from), PlayIndex(deviation, to))
        }
    }
}

/** The built-in sets' revision now, which presets saved by this version carry. */
internal val BUILT_IN_REVISION: Int = INDEX_CORRECTIONS.maxOfOrNull { it.revision } ?: IndexCorrection.FIRST_REVISION

private val WHITESPACE = Regex("\\s+")
private const val HEADER = "play hand upcard direction index"

/**
 * Reads a set as IndexSetData.kt prints it: a header, then a row each of play, hand ([IndexHand.label], or - for insurance),
 * upcard, [IndexDirection] and index, a † after the index marking it debated.
 */
internal fun parseIndexSet(rows: String): List<BuiltInIndex> {
    val lines = rows.lines().map(String::trim).filter(String::isNotEmpty)
    require(lines.firstOrNull()?.split(WHITESPACE)?.joinToString(" ") == HEADER) { "An index set starts with the header \"$HEADER\"" }

    return lines.drop(1).map { line ->
        val fields = line.split(WHITESPACE)
        require(fields.size == 5) { "Expected 5 fields in \"$line\"" }
        val (play, hand, upcard, direction, index) = fields
        val deviation = Deviation.parse(play, hand, upcard)
        val threshold = requireNotNull(index.removeSuffix(DEBATED_MARK).toIntOrNull()) { "Unknown index in \"$line\"" }

        BuiltInIndex(
            PlayIndex(deviation, threshold, IndexDirection.valueOf(direction)),
            debated = index.endsWith(DEBATED_MARK),
        )
    }.also { indices -> require(indices.distinctBy { it.index.deviation }.size == indices.size) { "A set lists a deviation twice" } }
}
