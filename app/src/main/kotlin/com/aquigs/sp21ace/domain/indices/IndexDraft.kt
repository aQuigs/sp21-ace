package com.aquigs.sp21ace.domain.indices

import com.aquigs.sp21ace.domain.strategy.Upcard
import java.io.Serializable

/** What an index is on: a hand of one of the kinds, or insurance, which asks for no hand. */
enum class IndexKind(val hand: HandKind?) {
    HARD(HandKind.HARD),
    SOFT(HandKind.SOFT),
    PAIR(HandKind.PAIR),
    INSURANCE(null),
}

/** The plays a kind offers, none for insurance, which is its own play. */
private val IndexKind.plays: List<DeviationPlay> get() = hand?.plays.orEmpty()

/**
 * An index being filled in, each field of its deviation null until chosen. Serializable, so a form half filled in survives the
 * activity's recreation.
 */
data class IndexDraft(
    val kind: IndexKind = IndexKind.HARD,
    val value: Int? = null,
    val upcard: Upcard? = null,
    val play: DeviationPlay? = null,
    val direction: IndexDirection = IndexDirection.AT_OR_ABOVE,
    val threshold: Int = 0,
) : Serializable {
    init {
        require(value == null || value in values) { "No $kind hand of $value" }
        require(play == null || play in plays) { "No $play index on a $kind hand" }
        require(threshold in INDEX_NUMBERS) { "An index of $threshold" }
    }

    /** The values [kind] offers, none for insurance. */
    val values: IntRange get() = kind.hand?.values ?: IntRange.EMPTY

    val plays: List<DeviationPlay> get() = kind.plays

    /** Another kind, which clears the value, since 8 means another hand to each kind, and a play the kind doesn't offer. */
    fun withKind(kind: IndexKind): IndexDraft = if (kind == this.kind) this else copy(kind = kind, value = null, play = play?.takeIf { it in kind.plays })

    /** The deviation once every field it needs is chosen. */
    val deviation: Deviation?
        get() {
            val hand = kind.hand ?: return Deviation.Insurance
            return if (value != null && upcard != null && play != null) Deviation.OnHand(IndexHand(hand, value), upcard, play) else null
        }

    val index: PlayIndex? get() = deviation?.let { PlayIndex(it, threshold, direction) }
}
