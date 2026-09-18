package com.aquigs.sp21ace.domain.cards

private val SUITS = mapOf('s' to Suit.SPADES, 'h' to Suit.HEARTS, 'd' to Suit.DIAMONDS, 'c' to Suit.CLUBS)

/** A card written as its rank label and a suit letter, such as "7h" or "Kc". */
fun card(text: String) = Card(Rank.entries.first { it.label == text.dropLast(1) }, SUITS.getValue(text.last()))

fun cards(text: String): List<Card> = text.split(" ").map(::card)
