package com.aquigs.sp21ace.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aquigs.sp21ace.R
import com.aquigs.sp21ace.domain.cards.Card
import com.aquigs.sp21ace.domain.drill.Streak
import com.aquigs.sp21ace.domain.settings.ButtonLocation
import com.aquigs.sp21ace.domain.settings.Settings
import com.aquigs.sp21ace.domain.strategy.upcard
import com.aquigs.sp21ace.domain.trainer.totalLabel

/**
 * A drill's [content] beside its streak meter. The meter and the drill's [controls], if any, take opposite edges of the screen,
 * which [settings] choose, as they do whether the meter shows. [overMeter], when given, sits above the meter, shown or not.
 */
@Composable
fun DrillTable(
    streak: Streak,
    settings: Settings,
    modifier: Modifier = Modifier,
    overMeter: (@Composable () -> Unit)? = null,
    controls: @Composable RowScope.() -> Unit = {},
    content: @Composable RowScope.() -> Unit,
) {
    val buttonsOnLeft = settings.buttonLocation == ButtonLocation.LEFT

    val edge: @Composable RowScope.() -> Unit = {
        if (settings.streakMeter || overMeter != null) {
            // The gap to the content, on the side the content is on
            val gap = if (buttonsOnLeft) Modifier.absolutePadding(left = 8.dp) else Modifier.absolutePadding(right = 8.dp)

            Column(
                modifier = gap.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                overMeter?.invoke()
                // As in Blackjack Ace, the numbers face the screen's edge
                if (settings.streakMeter) StreakMeter(streak, modifier = Modifier.weight(1f), numbersOnRight = buttonsOnLeft)
            }
        }
    }

    // The meter trades edges with the buttons
    EdgeRow(buttonsOnLeft, listOf(edge, content, controls), modifier)
}

/**
 * The dealer's hand over the [player]'s, with their [totals] when on, or with no player's cards, as for insurance, the dealer's alone,
 * the player's space left empty so the cards don't move.
 */
@Composable
fun RowScope.HandsColumn(upcard: Card, player: List<Card>, totals: Boolean, doubled: Boolean = false) {
    Column(
        modifier = Modifier.weight(1f).fillMaxHeight().wrapContentWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        HandArea(label = stringResource(R.string.dealer), total = upcard.upcard.label.takeIf { totals }, modifier = Modifier.weight(1f)) { fan ->
            DealerHand(upcard, fan)
        }
        if (player.isEmpty()) {
            Spacer(Modifier.weight(1f))
        } else {
            HandArea(label = stringResource(R.string.you), total = totalLabel(player).takeIf { totals }, modifier = Modifier.weight(1f)) { fan ->
                DissolvingHand(player, fan, sideways = doubled)
            }
        }
    }
}
