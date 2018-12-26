package com.github.jomof.nnsrs

import org.junit.Test

import org.junit.Assert.*

class MatrixKtTest {

    @Test
    fun basicParse() {
        val matrix = """
            0.10 0.50
            0.20 0.40
            0.30 0.30
        """.trimIndent().toMatrix()
        assert(matrix[2][1] == 0.3)
    }

    @Test
    fun isRectangular() {
        val matrix = """
            0.10 0.50
            0.20 0.40
            0.30 0.30
        """.trimIndent().toMatrix()
        assert(matrix.isRectangular)
    }

    @Test
    fun isNotRectangular() {
        val matrix = """
            1.00
            0.10 0.50
            0.20 0.40
            0.30 0.30
        """.trimIndent().toMatrix()
        assert(!matrix.isRectangular)
    }
}