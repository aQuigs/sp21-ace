package com.aquigs.sp21ace.domain.cards

import java.io.Serializable

enum class Suit(val letter: Char) { SPADES('s'), HEARTS('h'), DIAMONDS('d'), CLUBS('c') }

/** A Spanish deck has no 10-spots, so J, Q and K are its only ten-value cards. */
enum class Rank(val label: String, val value: Int) {
    ACE("A", 1),
    TWO("2", 2),
    THREE("3", 3),
    FOUR("4", 4),
    FIVE("5", 5),
    SIX("6", 6),
    SEVEN("7", 7),
    EIGHT("8", 8),
    NINE("9", 9),
    JACK("J", 10),
    QUEEN("Q", 10),
    KING("K", 10),
}

data class Card(val rank: Rank, val suit: Suit) : Serializable

/** [decks] Spanish decks of 48 cards each, unshuffled. */
fun spanishShoe(decks: Int): List<Card> = List(decks) { Rank.entries.flatMap { rank -> Suit.entries.map { Card(rank, it) } } }.flatten()

/** A card written as its rank label and suit letter, such as "7h" or "Kc". */
val Card.code: String get() = "${rank.label}${suit.letter}"

// Every code reads as one of these, so answers loaded by the thousand share their cards
private val CARDS_BY_CODE: Map<String, Card> = spanishShoe(decks = 1).associateBy { it.code }

/** The card [code] names, such as "7h". */
fun card(code: String): Card = requireNotNull(CARDS_BY_CODE[code]) { "Unknown card: $code" }

/** The cards [codes] names, space-separated, such as "As 6d". */
fun cards(codes: String): List<Card> = codes.split(" ").map(::card)

/** A soft total counts one ace as 11, which it does only while that doesn't bust the hand. */
data class HandTotal(val value: Int, val soft: Boolean)

fun List<Card>.total(): HandTotal = fold(HandTotal(0, soft = false)) { total, card -> total.plusCard(card.rank.value) }

/** The total once a card worth [points], an ace 1, joins the hand. */
fun HandTotal.plusCard(points: Int): HandTotal {
    // A hard 11 or less holds no ace, or it would count one as 11 and be soft, and a hard 12 or more can only count an ace as 1
    val hard = (if (soft) value - 10 else value) + points
    val nowSoft = (soft || points == 1) && hard + 10 <= 21
    return HandTotal(if (nowSoft) hard + 10 else hard, nowSoft)
}

fun List<Card>.isBlackjack(): Boolean = size == 2 && total().value == 21
