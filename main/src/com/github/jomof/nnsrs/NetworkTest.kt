package com.github.jomof.nnsrs

import org.junit.Test
import java.lang.Math.abs

class NetworkTest {
//
//    @Test
//    fun simpleInitialize() {
//        Network.fromNodeCounts(listOf(5, 4, 3, 2, 1))
//        Network.fromNodeCounts(listOf(1))
//    }
//
//    @Test
//    fun zero() {
//        Network.fromNodeCounts(listOf(0))
//    }
//
//    @Test
//    fun fromValues() {
//        val values = """
//            1.00 0.84 0.90
//            1.00 0.89 0.89
//            2.00 0.89 0.93
//            0.00 0.93 0.96
//            0.50 0.95 0.97
//            0.85
//        """.trimIndent().toMatrix()
//        Network.fromValues(values)
//    }
//
//    @Test
//    fun feedForward() {
//        val values = """
//               1.00 0.84
//            1.00 0.89 0.89
//                 0.89
//               0.00 0.93
//            0.50 0.95 0.97
//                 0.85
//        """.trimIndent().toMatrix()
//        val network = Network.fromValues(values)
//        val forward = network.feedForward(arrayOf(1.0, 2.0))
//    }
//
//
//    @Test
//    fun simplest() {
//        val values = """
//               1.00
//        """.trimIndent().toMatrix()
//        val network = Network.fromValues(values)
//        val forward = network.feedForward(arrayOf(2.0))
//        assert(forward.size == 1)
//        assert(forward[0] == 2.0) {
//            "Was $forward"
//        }
//    }
//
//    @Test
//    fun twoLayers() {
//        val values = """
//               1.0
//               0.0
//        """.trimIndent().toMatrix()
//        val biases = arrayOf(0.0)
//        val weights = arrayOf("""
//                1.0
//            """.trimIndent().toMatrix()
//        )
//        val network = Network(values, biases, weights)
//        val forward = network.feedForward(arrayOf(2.0))
//        assert(forward.size == 1)
//        assert(forward[0] > 0.8 && forward[0] < 0.9) {
//            "Was ${forward[0]}"
//        }
//    }

    @Test
    fun trainLine() {
        val nodeCounts = arrayOf(1, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val m = 0.5
        val b = 0.1
        fun f(x : Double) = sigmoid(m * x + b)
        val inputs = listOf(-1.0, 1.0)
        val data = inputs.map { Pair(vectorOf(it), vectorOf(f(it))) }
        network.train(data, 1.0, 100, 1)
        val cost = network.cost(data)
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
                Pair(it, vectorOf(0.33))
            } else {
                Pair(it, vectorOf(0.66))
            }
        }
        network.train(data, 1.0, 1000, 4)
        val cost = network.cost(data)
        if (abs(cost) > 0.0001) throw RuntimeException()
    }

    @Test
    fun actorSimulated() {
        val nodeCounts = arrayOf(10, 15, 1)
        val network = Network.fromNodeCounts(nodeCounts)
        val bigSample = inputsWindow(sampleDumbActorInteraction()).take(10000).toMutableList()
        fun doubleOf(boolean : Boolean) : Double = if (boolean) 1.0 else 0.0
        val inputs = bigSample.map { window ->
            val now = window[0].day
            val totalCorrect = window[1].totalCorrect.toDouble()
            val totalIncorrect = window[1].totalIncorrect.toDouble()
            val pctCorrect = (1.0 + totalCorrect) / (1.0 + totalCorrect / totalIncorrect)
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
            //vectorOf(t1, t2, t3, t4, t5)
            //vectorOf(a1, t1 /*, a2, t2, a3, t3, a4, t4, a5, t5 */)
            //vectorOf(a1, t1, a2, t2, a3, a4, a5)
            //vectorOf(totalCorrect)
            val result = vectorOf(t1, a1, t2, a2, t3, a3, t4, a4, t5, a5)
            result
        }
        val answers = bigSample.map { window ->
            vectorOf(doubleOf(window[0].correct))
        }
        val data = inputs.zip(answers).toList()
        network.train(data, 1.0, 500000, 50)
        val cost = network.costBool(data)
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