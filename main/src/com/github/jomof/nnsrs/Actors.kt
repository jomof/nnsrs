package com.github.jomof.nnsrs

import java.lang.Double.max
import java.lang.Double.min
import java.util.*


data class Interaction(
        val item: Int,
        val day: Double,
        val correct: Boolean,
        val totalCorrect: Int,
        val totalIncorrect: Int)

data class Data(
        val lastNow : Double,
        val forgetDuration : Double,
        val correct : Int,
        val incorrect : Int)

class DumbActor {
    private val startOfTime = 100.0
    private val shortestForgettingTime = 1.0 / 24.0 // One hour
    private val longestForgettingTime = 365.24 * 10.0  // Ten years

    private val forgetTimes = mutableMapOf<Int, Data>()

    fun plantItem(item: Int) {
        if (!forgetTimes.contains(item)) {
            forgetTimes[item] = Data(startOfTime, shortestForgettingTime, 0, 0) // Will initially forget in one hour
        }
    }

    fun waterItem(
            item: Int,
            daysPastTooLate: Double): Data {

        val data = forgetTimes[item]!!
        val momentOfForgetting = data.lastNow + data.forgetDuration
        val now = max(data.lastNow, momentOfForgetting + daysPastTooLate)
        if (daysPastTooLate < 0) {
            // Remembered. Add 20% to the duration and update now.
            forgetTimes[item] = data.copy(
                    lastNow = now,
                    forgetDuration = min(data.forgetDuration * 1.2, longestForgettingTime),
                    correct = data.correct + 1
            )
        } else {
            // Didn't remember. Cut the forget time back by 20% but not less than 1 hour
            forgetTimes[item] = data.copy(
                    lastNow = now,
                    forgetDuration = max(data.forgetDuration * 0.8, shortestForgettingTime),
                    incorrect = data.incorrect + 1
            )
        }
        return forgetTimes[item]!!
    }
}

fun sampleDumbActorInteraction(): Sequence<Interaction> {
    val itemCount = 100
    val chanceOfCorrectGuess = 0.5
    val maxInteractions = 200000000
    val itemBatches = 10 * 3
    val actor = DumbActor()
    val random = Random()
    val result = (0 until maxInteractions).asSequence().map {
        (0 until itemCount).asSequence().map { item ->
            actor.plantItem(item)
            (0 until itemBatches).asSequence().map {
                val daysPastTooLate = random.nextDouble() - chanceOfCorrectGuess
                val data = actor.waterItem(item, daysPastTooLate)
                if (daysPastTooLate < 0.0) {
                    Interaction(item, data.lastNow, true, data.correct, data.incorrect)
                } else {
                    Interaction(item, data.lastNow, false, data.correct, data.incorrect)
                }
            }
        }
    }.flatten().flatten()
    return result
}

fun inputsWindow(interactions: Sequence<Interaction>): Sequence<List<Interaction>> {
    val samplesPerWindow = 6
    return interactions.windowed(samplesPerWindow)
            .filter { window -> window.map { it.item }.toSet().size == 1 }
            .map { it.reversed() }

}