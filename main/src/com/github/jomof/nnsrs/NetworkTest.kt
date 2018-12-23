package com.github.jomof.nnsrs

import org.junit.Test

class NetworkTest {

    @Test
    fun simpleInitialize() {
        Network.fromNodeCounts(listOf(5,4,3,2,1))
        Network.fromNodeCounts(listOf(1))
    }

    @Test
    fun zero() {
        Network.fromNodeCounts(listOf(0))
    }

    @Test
    fun fromValues() {
        val values = """
            1.00 0.84 0.90
            1.00 0.89 0.89 
            2.00 0.89 0.93 
            0.00 0.93 0.96 
            0.50 0.95 0.97
            0.85
        """.trimIndent().toMatrix()
        Network.fromValues(values)
    }

    @Test
    fun feedForward() {
        val values = """
               1.00 0.84
            1.00 0.89 0.89
                 0.89
               0.00 0.93
            0.50 0.95 0.97
                 0.85
        """.trimIndent().toMatrix()
        val network = Network.fromValues(values)
        val forward = network.feedForward(arrayOf(1.0,2.0))
    }


    @Test
    fun simplest() {
        val values = """
               1.00
        """.trimIndent().toMatrix()
        val network = Network.fromValues(values)
        val forward = network.feedForward(arrayOf(2.0))
        assert(forward.size == 1)
        assert(forward[0] == 2.0) {
            "Was $forward"
        }
    }

    @Test
    fun twoLayers() {
        val values = """
               1.0
               0.0
        """.trimIndent().toMatrix()
        val biases = arrayOf(0.0)
        val weights = arrayOf(
                """
                    1.0
                """.trimIndent().toMatrix()
        )
        val network = Network(values, biases, weights)
        val forward = network.feedForward(arrayOf(2.0))
        assert(forward.size == 1)
        assert(forward[0] > 0.8 && forward[0] < 0.9) {
            "Was ${forward[0]}"
        }
    }
}