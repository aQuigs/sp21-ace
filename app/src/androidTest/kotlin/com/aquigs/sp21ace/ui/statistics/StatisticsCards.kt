package com.aquigs.sp21ace.ui.statistics

import android.content.Context
import com.aquigs.sp21ace.R

/** The texts the Bankroll card reads with a [profit], [won] and [lost] as the screen writes them. The chart reads as a description instead. */
fun Context.bankrollCardTexts(profit: String, won: String, lost: String): List<String> =
    listOf(getString(R.string.bankroll), profit, getString(R.string.profit_loss), won, getString(R.string.amount_won), lost, getString(R.string.amount_lost))

/** The texts the Strategy card reads with [hands] played and each grade's count and share, in Blackjack Ace's order. */
fun Context.strategyCardTexts(hands: Int, vararg grades: Pair<Int, String>): List<String> {
    val labels = listOf(R.string.correct, R.string.correct_with_hints, R.string.incorrect, R.string.no_action_required)
    return listOf(getString(R.string.strategy), "$hands", getString(R.string.hands_played)) +
        labels.zip(grades) { label, (count, share) -> listOf("$count", share, getString(label)) }.flatten()
}
