package com.aquigs.sp21ace.ui.components

import java.text.NumberFormat
import kotlin.math.abs

/** An amount in cents as the table writes it: 1,000, or 37.50 where there are cents. */
fun chipsText(cents: Long): String {
    val digits = if (cents % 100 == 0L) 0 else 2
    val format = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
    }
    return format.format(cents / 100.0)
}

/** A profit or loss as Statistics writes it: 25, or −37.50. */
fun profitText(cents: Long): String = (if (cents < 0) "−" else "") + chipsText(abs(cents))

/** What a hand won or lost: +37.50 or −25. */
fun netText(cents: Long): String = (if (cents >= 0) "+" else "") + profitText(cents)
