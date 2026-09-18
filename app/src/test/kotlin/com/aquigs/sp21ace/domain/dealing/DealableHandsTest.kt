package com.aquigs.sp21ace.domain.dealing

import com.aquigs.sp21ace.domain.cards.cards
import com.aquigs.sp21ace.domain.cards.spanishShoe
import com.aquigs.sp21ace.domain.strategy.ChartRow
import com.aquigs.sp21ace.domain.strategy.ChartTable
import com.aquigs.sp21ace.domain.strategy.RuleSet
import com.aquigs.sp21ace.domain.strategy.StrategyCharts
import com.aquigs.sp21ace.domain.strategy.Upcard
import com.aquigs.sp21ace.domain.strategy.chartRow
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.TrainerHand
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class DealableHandsTest {
    private val s17Hands = dealableHands(RuleSet.S17).values.flatten()

    private fun waysToDeal(player: String, upcard: Upcard) = s17Hands.single { it.player.toSet() == cards(player).toSet() && it.upcard == upcard }.ways

    @Test
    fun theDealtRowsAreHard5To19SoftA2ToA9AndEveryPairJustAsTheDealReachesThem() {
        val random = Random(21)
        val picker = HandPicker(RuleSet.S17, HandCustomization())
        val reached = List(5_000) { chartRow(picker.pick(emptyList(), random).player) }.toSet()
        val expected = (5..19).map { ChartRow(ChartTable.HARD, "$it") } +
            (2..9).map { ChartRow(ChartTable.SOFT, "A-$it") } +
            Upcard.entries.map { ChartRow(ChartTable.PAIRS, "${it.label}-${it.label}") }

        assertEquals(expected.toSet(), DEALT_ROWS)
        assertEquals(DEALT_ROWS, reached)
    }

    @Test
    fun everyTwoCardsButABlackjackAreListedAgainstEachUpcardValueAndGradedAsAgainstEveryCardOfIt() {
        val deck = spanishShoe(decks = 1)

        for (rules in RuleSet.entries) {
            val chart = StrategyCharts.forRules(rules)
            val listed = dealableHands(rules).values.flatten()
            val misgraded = listed.flatMap { hand -> deck.filter { it.upcard == hand.upcard }.map { TrainerHand(hand.player, it) }.filter { it.type(chart) != hand.type } }

            // A deck's 48 × 49 / 2 two cards, the same card twice included, less 4 aces × 12 tens
            assertEquals("$rules", 1_128 * 10, listed.size)
            assertEquals("$rules", listed.size, listed.map { it.player.toSet() to it.upcard }.toSet().size)
            assertEquals("$rules", emptyList<TrainerHand>(), misgraded)
        }
    }

    @Test
    fun theWaysToDealCountEveryDealOfASixDeckShoeButABlackjack() {
        // The same card twice, against any 7 left: 6 × 5 × 22
        assertEquals(660, waysToDeal("7h 7h", Upcard.SEVEN))
        // Either card first: 6 × 6 × 70 × 2
        assertEquals(5_040, waysToDeal("Ks Qs", Upcard.TEN))
        assertEquals(1_728, waysToDeal("9c 7d", Upcard.ACE))

        // The first card, the upcard and the second card from 288, less 24 aces and 72 tens either way against any upcard left
        assertEquals(288L * 287 * 286 - 24L * 72 * 2 * 286, s17Hands.sumOf { it.ways.toLong() })
    }
}
