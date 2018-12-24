package com.github.jomof.nnsrs

import java.lang.Double.max
import java.lang.Double.min
import java.util.*


data class Interaction(
        val item: Int,
        val day: Double,
        val correct: Boolean
)


class DumbActor {
    private val startOfTime = 100.0
    private val shortestForgettingTime = 1.0 / 24.0 // One hour
    private val longestForgettingTime = 365.24 * 10.0  // Ten years

    private val forgetTimes = mutableMapOf<Int, Pair<Double, Double>>()

    fun plantItem(item: Int) {
        if (!forgetTimes.contains(item)) {
            forgetTimes[item] = Pair(startOfTime, shortestForgettingTime) // Will initially forget in one hour
        }
    }

    fun waterItem(
            item: Int,
            daysPastTooLate: Double): Double {

        val (lastNow, forgetDuration) = forgetTimes[item]!!
        val momentOfForgetting = lastNow + forgetDuration
        val now = max(lastNow, momentOfForgetting + daysPastTooLate)
        if (daysPastTooLate < 0) {
            // Remembered. Add 20% to the duration and update now.
            forgetTimes[item] = Pair(now, min(forgetDuration * 1.2, longestForgettingTime))
        } else {
            // Didn't remember. Cut the forget time back by 20% but not less than 1 hour
            forgetTimes[item] = Pair(now, max(forgetDuration * 0.8, shortestForgettingTime))
        }
        return now
    }
}

fun sampleDumbActorInteraction(): Sequence<Interaction> {
    val itemCount = 5
    val chanceOfCorrectGuess = 0.9
    val maxInteractions = 200000000
    val itemBatches = 10 * 3
    val actor = DumbActor()
    val random = Random()
    val result = (0 until maxInteractions).asSequence().map {
        (0 until itemCount).asSequence().map { item ->
            actor.plantItem(item)
            (0 until itemBatches).asSequence().map {
                val daysPastTooLate = random.nextDouble() - chanceOfCorrectGuess
                val now = actor.waterItem(item, daysPastTooLate)
                if (daysPastTooLate < 0.0) {
                    Interaction(item, now, true)
                } else {
                    Interaction(item, now, false)
                }
            }
        }
    }.flatten().flatten()
    return result
}

fun inputsWindow(interactions: Sequence<Interaction>): Sequence<List<Double>> {
    return interactions.windowed(4)
            .filter { window -> window.map { it.item }.toSet().size == 1 }
            .map { window ->
                val baseline : Double = window.map { it.day }.max() ?: throw RuntimeException()
                val result = mutableListOf<Double>()
                result.add(window[0].item.toDouble())
                result.addAll(window.map { listOf(baseline - it.day, if (it.correct) 1.0 else 0.0) }.flatten())

                result
            }
}