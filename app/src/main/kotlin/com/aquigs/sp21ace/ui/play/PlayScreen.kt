package com.aquigs.sp21ace.ui.play

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.game.Bonus
import com.aquigs.sp21ace.domain.game.CHIPS
import com.aquigs.sp21ace.domain.game.Finish
import com.aquigs.sp21ace.domain.game.HandResult
import com.aquigs.sp21ace.domain.game.Outcome
import com.aquigs.sp21ace.domain.game.PlayerHand
import com.aquigs.sp21ace.domain.game.Round
import com.aquigs.sp21ace.domain.game.TOP_UPS
import com.aquigs.sp21ace.domain.game.Table
import com.aquigs.sp21ace.domain.game.correctMove
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.Move
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.totalLabel
import com.aquigs.sp21ace.ui.components.CardBack
import com.aquigs.sp21ace.ui.components.CircleButton
import com.aquigs.sp21ace.ui.components.CircleButtonSize
import com.aquigs.sp21ace.ui.components.ConfirmDialog
import com.aquigs.sp21ace.ui.components.DissolvingHand
import com.aquigs.sp21ace.ui.components.EdgeControls
import com.aquigs.sp21ace.ui.components.EdgeRow
import com.aquigs.sp21ace.ui.components.HandArea
import com.aquigs.sp21ace.ui.components.MoveButton
import com.aquigs.sp21ace.ui.components.OverlappingCards
import com.aquigs.sp21ace.ui.components.PlayingCard
import com.aquigs.sp21ace.ui.components.appBarColors
import com.aquigs.sp21ace.ui.components.displayName
import com.aquigs.sp21ace.ui.components.rememberArmed
import com.aquigs.sp21ace.ui.theme.DISABLED_ALPHA
import kotlinx.coroutines.delay

// About as long as Blackjack Ace's dealer takes over each card it turns or draws
internal const val DEALER_CARD_MILLIS = 500L

/**
 * Blackjack Ace's Play table: the bankroll in the app bar, the dealer's cards over yours, a band across the middle that asks for
 * a bet and gives each result, and a tray of chips to bet with. Only the moves the hand can make have buttons. As [settings]
 * choose, a bulb under the chart tile rings the correct move, and a move that isn't asks to be confirmed. [onUpdate] is handed a
 * change to apply to whichever table is current, so a second tap before the screen redraws can't undo the first, and a move is
 * checked against the hand it lands on.
 */
@Composable
fun PlayScreen(
    table: Table,
    settings: Settings,
    onUpdate: ((Table) -> Table?) -> Unit,
    onDeal: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenChart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val round = table.round
    val buttonsOnLeft = settings.buttonLocation == ButtonLocation.LEFT
    var addingChips by rememberSaveable { mutableStateOf(false) }
    var questioned by rememberSaveable { mutableStateOf<Move?>(null) }
    val play: (Move) -> Unit = { move ->
        onUpdate { current ->
            val next = current.play(move)
            if (next != null && settings.warnOnIncorrectMove && move != current.round?.correctMove()) {
                questioned = move
                null
            } else {
                next
            }
        }
    }

    // The hole card turns over as the round settles, then each card the dealer drew in turn
    LaunchedEffect(round?.settled, table.dealerCardsShown) {
        if (round?.settled == true && !table.revealed) {
            delay(DEALER_CARD_MILLIS)
            onUpdate { it.revealDealerCard() }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { PlayBar(table.available, onAddChips = { addingChips = true }, onOpenDrawer) },
        bottomBar = { Tray(table, onBet = { chip -> onUpdate { it.addChip(chip) } }) },
    ) { padding ->
        val cards: @Composable RowScope.() -> Unit = {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight().wrapContentWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    if (round != null) DealerArea(round, table.dealerCardsShown, settings.handTotals)
                }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    when {
                        round != null -> table.shownHand?.let { PlayerArea(it, settings.handTotals) }
                        table.bet > 0 -> BetStack(table.bet, onTakeBack = { onUpdate { it.clearBet() } })
                    }
                }
            }
        }
        val controls: @Composable RowScope.() -> Unit = {
            EdgeControls(
                buttonsOnLeft,
                settings.chartButton,
                onOpenChart,
                underTile = {
                    // As in Blackjack Ace, the bulb goes once it has shown the move, until the next decision
                    if (settings.hintButton && table.canHint) HintBulb(onClick = { onUpdate { it.showHint() } })
                },
            ) {
                TableButtons(table, onDeal = onDeal, onMove = play, onNext = { onUpdate { it.next() } })
            }
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            EdgeRow(buttonsOnLeft, listOf(cards, controls), Modifier.fillMaxSize().padding(16.dp))
            Band(table, Modifier.align(Alignment.Center))
        }
    }

    if (addingChips) AddChipsDialog(onTopUp = { amount -> onUpdate { it.topUp(amount) } }, onDismiss = { addingChips = false })
    // Blackjack Ace's check before a move the strategy doesn't make. It doesn't say which move is right; the hint does
    questioned?.let { move ->
        ConfirmDialog(
            title = stringResource(R.string.incorrect_move_title),
            message = stringResource(R.string.incorrect_move_message, stringResource(move.displayName)),
            confirmLabel = stringResource(R.string.play_move),
            onConfirm = {
                questioned = null
                onUpdate { it.play(move) }
            },
            onDismiss = {
                questioned = null
                onUpdate { it.heedWarning() }
            },
        )
    }
}

/** Blackjack Ace's hint bulb, as wide as the chart tile over it. */
@Composable
private fun HintBulb(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(CircleButtonSize)) {
        Icon(
            painterResource(R.drawable.ic_lightbulb),
            contentDescription = stringResource(R.string.show_hint),
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayBar(available: Long, onAddChips: () -> Unit, onOpenDrawer: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { BankrollPill(available, onAddChips) },
        actions = {
            IconButton(onClick = onOpenDrawer) {
                Icon(painterResource(R.drawable.ic_menu), contentDescription = stringResource(R.string.open_menu))
            }
        },
        colors = appBarColors(),
    )
}

/** Blackjack Ace's bankroll pill, which opens the free top-ups. */
@Composable
private fun BankrollPill(available: Long, onAddChips: () -> Unit) {
    val amount = chipsText(available)
    val description = stringResource(R.string.bankroll_description, amount)

    Surface(
        onClick = onAddChips,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Chip(CHIPS[2], Modifier.size(28.dp), labelled = false)
            Text(amount, style = MaterialTheme.typography.titleLarge)
            Icon(painterResource(R.drawable.ic_add_circle), contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DealerArea(round: Round, shown: Int, handTotals: Boolean) {
    val faceUp = round.dealer.take(shown)

    HandArea(
        label = stringResource(R.string.dealer),
        total = (if (round.settled) totalLabel(faceUp) else round.upcard.upcard.label).takeIf { handTotals },
    ) { fan ->
        if (round.settled) {
            DissolvingHand(faceUp, fan)
        } else {
            // As Blackjack Ace deals it, the hole card lies over the upcard's right side
            OverlappingCards(fan) {
                PlayingCard(round.upcard)
                CardBack()
            }
        }
    }
}

@Composable
private fun PlayerArea(hand: PlayerHand, handTotals: Boolean) {
    HandArea(label = stringResource(R.string.you), total = totalLabel(hand.cards).takeIf { handTotals }) { fan ->
        DissolvingHand(hand.cards, fan, sideways = hand.doubled)
    }
}

@Composable
private fun BetStack(bet: Long, onTakeBack: () -> Unit) {
    val amount = chipsText(bet)
    val description = stringResource(R.string.take_back_bet, amount)

    Column(
        modifier = Modifier.clickable(onClick = onTakeBack).semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Chip(topChip(bet), Modifier.size(64.dp))
        Text(amount, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

/**
 * DEAL once a bet is down, the moves the hand can make while it's played, and OK or NEXT on each result. With only those moves
 * shown, the buttons shift whenever they change, so a tap waits until the new ones have been there a double tap's length: the
 * second tap of a double tap on DEAL would otherwise land on the SURRENDER that takes its place.
 */
@Composable
private fun ColumnScope.TableButtons(table: Table, onDeal: () -> Unit, onMove: (Move) -> Unit, onNext: () -> Unit) {
    val round = table.round
    val moves = round?.takeUnless { it.settled }?.moves()?.let { moves -> Move.entries.filter { it in moves } }.orEmpty()
    val action = when {
        round == null -> R.string.deal.takeIf { table.bet > 0 }
        table.hasNextResult -> R.string.next
        table.revealed -> R.string.ok
        else -> null
    }
    // The bulb going can move the buttons up into its place on a short screen
    val armed by rememberArmed(moves, action, table.hinted)

    val hint = table.hint
    moves.forEach { move ->
        MoveButton(move, onClick = { if (armed) onMove(move) }, modifier = Modifier.weight(1f, fill = false), hinted = move == hint)
    }
    action?.let { name -> CircleButton(name = stringResource(name), onClick = { if (armed) if (round == null) onDeal() else onNext() }) }
}

/**
 * Blackjack Ace's band across the middle of the table, over the cards, with the message in large light type: Place Your Bet
 * between rounds, and each hand's result once the dealer's cards are all face up. Taps go through it to the bet under it.
 */
@Composable
private fun Band(table: Table, modifier: Modifier = Modifier) {
    val hand = table.shownHand
    val result = table.shownResult
    val message = when {
        table.round == null -> stringResource(R.string.place_your_bet)
        hand != null && result != null -> stringResource(result.headline(hand))
        else -> null
    }
    val details = result?.let { resultDetails(it) }
    val spoken = listOfNotNull(message, details).joinToString(". ")

    // A live region speaks when its words change, so it's this box, there between messages too, that speaks each one
    Box(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
            if (message != null) contentDescription = spoken
        },
    ) {
        if (message != null) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimaryContainer) {
                Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f))) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(message, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Light, textAlign = TextAlign.Center)
                        details?.let { Text(it, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun HandResult.headline(hand: PlayerHand): Int = when {
    blackjack -> R.string.result_blackjack
    hand.finish == Finish.BUSTED -> R.string.result_bust
    hand.finish == Finish.SURRENDERED -> R.string.result_surrendered
    hand.finish == Finish.RESCUED -> R.string.result_rescued
    outcome == Outcome.WIN -> R.string.result_win
    outcome == Outcome.PUSH -> R.string.result_push
    else -> R.string.result_lose
}

/** The bonus and what it paid, the Super Bonus, and the chips won or lost, as far as there are any. */
@Composable
private fun resultDetails(result: HandResult): String? = listOfNotNull(
    result.bonus?.let { stringResource(R.string.bonus_pays, stringResource(it.displayName), stringResource(R.string.odds, it.odds.win, it.odds.stake)) },
    result.superBonus.takeIf { it > 0 }?.let { stringResource(R.string.super_bonus, chipsText(it)) },
    result.net.takeIf { it != 0L }?.let(::netText),
).joinToString(" · ").ifEmpty { null }

/** Chips to bet with between rounds, those the bankroll can't cover greyed out, and during a round the bet on each hand. */
@Composable
private fun Tray(table: Table, onBet: (Long) -> Unit) {
    val round = table.round

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().heightIn(min = 88.dp).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (round == null) {
                    CHIPS.forEach { chip ->
                        val enabled = table.addChip(chip) != null
                        val description = stringResource(R.string.bet_chip, chipsText(chip))
                        Chip(
                            chip,
                            Modifier
                                .size(56.dp)
                                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                                .clickable(enabled = enabled) { onBet(chip) }
                                .semantics { contentDescription = description },
                        )
                    }
                } else {
                    round.hands.forEachIndexed { index, hand ->
                        HandBet(hand.wager, shown = index == table.shownIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandBet(wager: Long, shown: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
        Chip(topChip(wager), Modifier.size(48.dp))
        Text(
            chipsText(wager),
            color = if (shown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (shown) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/** Blackjack Ace's free top-ups: each amount tapped goes straight onto the bankroll. */
@Composable
private fun AddChipsDialog(onTopUp: (Long) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
        title = { Text(stringResource(R.string.add_chips)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.add_chips_message))
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                TOP_UPS.forEach { amount ->
                    val description = stringResource(R.string.add_chips_amount, chipsText(amount))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTopUp(amount) }
                            .semantics(mergeDescendants = true) { contentDescription = description }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Chip(amount, Modifier.size(40.dp))
                        Text(chipsText(amount), style = MaterialTheme.typography.titleMedium)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    )
}

private val Bonus.displayName: Int
    get() = when (this) {
        Bonus.FIVE_CARD_21 -> R.string.bonus_five_card_21
        Bonus.SIX_CARD_21 -> R.string.bonus_six_card_21
        Bonus.SEVEN_CARD_21 -> R.string.bonus_seven_card_21
        Bonus.MIXED_678 -> R.string.bonus_mixed_678
        Bonus.SUITED_678 -> R.string.bonus_suited_678
        Bonus.SPADED_678 -> R.string.bonus_spaded_678
        Bonus.MIXED_777 -> R.string.bonus_mixed_777
        Bonus.SUITED_777 -> R.string.bonus_suited_777
        Bonus.SPADED_777 -> R.string.bonus_spaded_777
    }
