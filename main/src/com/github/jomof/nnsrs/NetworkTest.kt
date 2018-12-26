package com.github.jomof.nnsrs

import org.junit.Test
import java.lang.Math.abs

class NetworkTest {
    @Test
    fun trainLine() {
        val nodeCounts = arrayOf(1, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val m = 0.5
        val b = 0.1
        fun f(x : Double) = sigmoid(m * x + b)
        val inputs = listOf(-1.0, 1.0)
        val data = inputs.map { Pair(vectorOf(it), vectorOf(f(it))) }
        network.train(data, 1.0, 100, 1, reportBoolCost(network, data))
        val cost = cost(network, data)
        if (abs(cost) > 0.0001) throw RuntimeException()
    }

    @Test
    fun trainXor() {
        val nodeCounts = arrayOf(2, 7, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val inputs = listOf(
                vectorOf(0.0, 0.0),
                vectorOf(1.0, 0.0),
                vectorOf(0.0, 1.0),
                vectorOf(1.0, 1.0))
        val data = inputs.map { it ->
            val left = it[0] > 0
            val right = it[1] > 0
            if (left xor right) {
                Pair(it, vectorOf(0.66))
            } else {
                Pair(it, vectorOf(0.33))
            }
        }
        do {
            network.train(data, 1.0, 500, 2, reportBoolCost(network, data))
        } while (cost(network, data) > 0.00001)
        val cost = costBool(network, data)
        if (abs(cost) > 0.0001) throw RuntimeException()
    }

    @Test
    fun actorSimulated() {
        val nodeCounts = arrayOf(10, 20, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val bigSample = inputsWindow(sampleDumbActorInteraction()).take(10000).toMutableList()
        fun doubleOf(boolean : Boolean) : Double = if (boolean) 1.0 else 0.0
        val inputs = bigSample.map { window ->
            val now = window[0].day
            val t1 = now - window[1].day
            val a1 = doubleOf(window[1].correct)
            val t2 = now - window[2].day
            val a2 = doubleOf(window[2].correct)
            val t3 = now - window[3].day
            val a3 = doubleOf(window[3].correct)
            val t4 = now - window[4].day
            val a4 = doubleOf(window[4].correct)
            val t5 = now - window[5].day
            val a5 = doubleOf(window[5].correct)
            val result = vectorOf(t1, a1, t2, a2, t3, a3, t4, a4, t5, a5)
            result
        }
        val answers = bigSample.map { window ->
            vectorOf(doubleOf(window[0].correct))
        }
        val data = inputs.zip(answers).toList()
        network.train(data, 1.0, 500000, 500, reportBoolCost(network, data))
        val cost = costBool(network, data)
        println("Costbool = $cost")

//            1.0 1.0 1.0 1.0 1.0 1.0
//            0.0 0.0 0.0 0.0 0.0 0.0
//            0.85
//        """.trimIndent().toMatrix()
//        val network = Network.fromValues(values)
//        network.randomize()

//        bigSample.shuffle()

//        val cost = network.cost(inputs, answers)
//        println("cost = $cost")
    }
}