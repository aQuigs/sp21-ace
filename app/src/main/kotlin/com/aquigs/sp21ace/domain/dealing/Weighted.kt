package com.aquigs.sp21ace.domain.dealing

import kotlin.random.Random

/** Things to pick from, each as often as its weight, with the running sum of the weights worked out once. */
internal class Weighted<T>(private val items: List<T>, weight: (T) -> Double) {
    private val running = items.map(weight).runningReduce(Double::plus)

    val total: Double get() = running.last()

    fun pick(random: Random): T {
        val target = random.nextDouble(total)
        return items[running.indexOfFirst { it > target }]
    }
}
