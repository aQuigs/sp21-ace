package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.DEBATED_MARK
import com.aquigs.sp21ace.domain.strategy.Upcard

/** A published index, [debated] where the sources disagree and the app ships the recommended number. */
data class BuiltInIndex(val index: PlayIndex, val debated: Boolean = false)

/** The sourced index sets, one for each rule set with published numbers: Hi-Lo's Illustrious 18 and Fab 4. */
enum class BuiltInIndexSet(val game: Game, val title: String) {
    SHOE_H17(Game.BLACKJACK, "Shoe, H17"),
    SHOE_S17(Game.BLACKJACK, "Shoe, S17"),
    DOUBLE_DECK(Game.BLACKJACK, "Double deck"),
    ;

    /** In the order the sources list them. */
    val indices: List<BuiltInIndex> by lazy { parseIndexSet(INDEX_SETS.getValue(this)) }

    /** The set's number for [deviation], or null when it has none. */
    fun find(deviation: Deviation): BuiltInIndex? = indices.firstOrNull { it.index.deviation == deviation }
}

private val WHITESPACE = Regex("\\s+")
private const val HEADER = "play hand upcard index"
private const val NO_HAND = "-"

/**
 * Reads a set as IndexSetData.kt prints it: a header, then a row each of play, hand ([IndexHand.label], or - for insurance),
 * upcard and index, a † after the index marking it debated.
 */
internal fun parseIndexSet(rows: String): List<BuiltInIndex> {
    val lines = rows.lines().map(String::trim).filter(String::isNotEmpty)
    require(lines.firstOrNull()?.split(WHITESPACE)?.joinToString(" ") == HEADER) { "An index set starts with the header \"$HEADER\"" }

    return lines.drop(1).map { line ->
        val fields = line.split(WHITESPACE)
        require(fields.size == 4) { "Expected 4 fields in \"$line\"" }
        val (play, hand, upcard, index) = fields
        val deviation = if (play == "INSURANCE") {
            require(hand == NO_HAND && upcard == Upcard.ACE.label) { "Insurance has no hand and a dealer ace: \"$line\"" }
            Deviation.Insurance
        } else {
            Deviation.OnHand(IndexHand.parse(hand), Upcard.fromLabel(upcard), DeviationPlay.valueOf(play))
        }
        val threshold = requireNotNull(index.removeSuffix(DEBATED_MARK).toIntOrNull()) { "Unknown index in \"$line\"" }

        BuiltInIndex(PlayIndex(deviation, threshold), debated = index.endsWith(DEBATED_MARK))
    }.also { indices -> require(indices.distinctBy { it.index.deviation }.size == indices.size) { "A set lists a deviation twice" } }
}
