package com.github.jomof.nnsrs

import org.joda.time.DateTime
import java.util.*
import kotlin.RuntimeException

fun Boolean?.toDoubleString() = if (this == null) "" else if (this) "1" else ""
fun Double?.toCompactString() = if (this == null) "" else if (this == 0.0) "" else "%.3f".format(this)
fun Int.toOneOff(hash : Int) : String {
    val list = mutableListOf<String>()
    val atZero = this % hash
    list.addAll((0 until atZero).map { "" })
    list.add("1")
    list.addAll((atZero until hash - 1).map { "" })
    return list.joinToString(",")
}

data class ReviewHistoryWindow(
        val size : Int,
        val subjectId : Int,
        val currentPass : Boolean,
        val passes : List<Boolean?>,
        val days : List<Double>
) {
    fun normalize(min : Double, max : Double) : ReviewHistoryWindow {
        return copy(
                days = days.map { day ->
                    val result = (day - min) / (max - min)
                    if (result < 0.0 || result > 1.0) {
                        throw RuntimeException()
                    }
                    result
                }
        )
    }

    fun toString(hash : Int, random : Random): String {
        val result = mutableListOf(currentPass.toDoubleString())
        val correct = (0 until days.size).filter { passes[it]!! }.size.toDouble()
        val incorrect = (0 until days.size).filter { !passes[it]!! }.size.toDouble()
        val percent = (correct) / (correct + incorrect)
        result.add(percent.toCompactString())
        result.addAll(days.padEnd(size, 0.0)
                .zip(passes.padEnd(size, null))
                .map { (days, pass) -> if (pass == null) null else if (pass) days else -days}
                .map { it.toCompactString() })
        return result.joinToString(",")
    }
}

fun toDays(datetime : DateTime) : Double {
    return datetime.millis.toDouble() / 1000.0 / 60.0 / 60.0 / 24.0
}

fun toDays(review : Review) : Double {
    return toDays(review.dataUpdatedAt)
}

fun passed(review : Review) : Boolean {
    return review.data.startingSrsStage < review.data.endingSrsStage
}

fun <E> List<E>.padEnd(size : Int, value : E) : List<E> {
    val result = this + (this.size until size).map { value }
    if (result.size != size) {
        throw RuntimeException()
    }
    return result
}

fun slideReviews(size : Int, list : List<Review>) : List<ReviewHistoryWindow> {
    return list.asSequence().windowed(size + 1, 1, true).mapNotNull { reviews ->
        if (reviews[0].data.subjectId < 0) {
            throw RuntimeException()
        }
        val sorted = reviews.sortedByDescending { review -> toDays(review) }
        val reviews = 1
        val windowSize = sorted.size
        if (windowSize == 1)
            null
        else if (windowSize == size + 1) {
            null
        } else {
            (1 until windowSize).map { index ->
                if (sorted[index].data.subjectId != sorted[index - 1].data.subjectId) {
                    throw RuntimeException()
                }
            }
            val passes = (1 until windowSize).map { index ->
                passed(sorted[index])
            }
            val timeDeltas = (1 until windowSize).map { index ->
                val lastTime = toDays(sorted[index - 1])
                val thisTime = toDays(sorted[index])
                if (thisTime >= lastTime) throw RuntimeException()
                lastTime - thisTime
            }
            if (timeDeltas.sum() == 0.0) {
                throw RuntimeException()
            }
            ReviewHistoryWindow(
                    size = size,
                    subjectId = sorted[0].data.subjectId,
                    currentPass = passed(sorted[0]),
                    passes = passes,
                    days = timeDeltas)
        }
    }.toList()
}

fun minmaxDays(list : List<ReviewHistoryWindow>) : Pair<Double, Double> {
    var min = Double.MIN_VALUE
    var max = Double.MIN_VALUE
    list.onEach { window ->
        window.days .onEach { day ->
            min = Math.min(min, day)
            max = Math.max(max, day)
        }
    }
    return Pair(min, max)
}

fun minmaxSubjectId(list : List<ReviewHistoryWindow>) : Pair<Int, Int> {
    var min = Int.MAX_VALUE
    var max = Int.MIN_VALUE
    list.onEach { window ->
        min = Math.min(min, window.subjectId)
        max = Math.max(max, window.subjectId)
    }
    return Pair(min, max)
}