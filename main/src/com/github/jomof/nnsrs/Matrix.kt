package com.github.jomof.nnsrs

import kotlin.RuntimeException


typealias Matrix = Array<Vector>


fun vectorOf(vararg args : Double) : Vector = Vector(args.map { it }.toTypedArray())

fun vecToMatrix(input: Vector): Matrix {
    // input[size] into output[size][1]
    return matrix(input.size, 1) { i, _ ->
        input[i]
    }
}

fun matrixToVec(input: Matrix): Vector {
    // input[size][1] into output[size]
    if (input.width() != 1) {
        throw RuntimeException()
    }
    return Vector(input.size) { i ->
        input[i][0]
    }
}


fun matrix(
        height : Int,
        width : Int,
        init : (Int, Int) -> Double = { _, _ -> 0.0 }) : Matrix {
    return Array(height) { x -> Vector(width) { y -> init(x, y) } }
}

fun Matrix.transpose() = matrix(width(), height()) { i, j ->
    this[j][i]
}

infix fun Matrix.dot(right : Matrix): Matrix {
    val leftHeight = height()
    val leftWidth = width()
    val rightWidth = right.width()
    val result = matrix(leftHeight, rightWidth)
    for (i in 0 until leftHeight)
        for (j in 0 until rightWidth)
            for (k in 0 until leftWidth)
                result[i][j] += this[i][k] * right[k][j]
    return result
}

infix fun Matrix.minus(right : Double) : Matrix {
    return map { it minus right }.toTypedArray()
}


infix fun Matrix.times(right : Double) : Matrix {
    return map { it times right }.toTypedArray()
}

infix fun Double.minus(right : Vector) : Vector {
    return right.map { it - this }
}

operator fun Matrix.plusAssign(right : Matrix) {
    if (height() != right.height()) throw RuntimeException()
    for (i in 0 until height()) {
        val leftVector = this[i]
        val rightVector = right[i]
        if (leftVector.size != rightVector.size) throw RuntimeException()
        for (j in 0 until leftVector.size) {
            leftVector[j] += rightVector[j]
        }
    }
}

operator fun Matrix.plusAssign(right : Vector) {
    if (width() != 1) throw RuntimeException()
    if (height() != right.size) throw RuntimeException()
    for (i in 0 until height()) {
        this[i][0] += right[i]
    }
}

infix fun Matrix.minus(right : Matrix) : Matrix {
    return zip(right).map { (l : Vector, r) -> l.minus(r) }.toTypedArray()
}


fun Matrix.toZeroes() : Matrix {
    return map { it.toZeroes() }.toTypedArray()
}

fun Matrix.clear() {
    for (i in 0 until height()) {
        val row = this[i]
        for (j in 0 until row.size) {
            row[j] = 0.0
        }
    }
}

infix fun Matrix.dot(right : Vector) : Matrix {
    if (width() != right.size) {
        throw RuntimeException("Dot of mismatched vectors")
    }
    return this dot vecToMatrix(right)
}

fun sigmoid(value : Double) : Double {
    return 1.0 / (1.0 + Math.exp(-value))
}

fun Matrix.sigmoid() : Matrix {
    return map { vector -> vector.map { sigmoid(it) } }.toTypedArray()

}

fun Matrix.height() = size

fun Matrix.width() : Int {
    if (!isRectangular()) {
        throw RuntimeException("Not rectangular")
    }
    return get(0).size
}

fun Matrix.isRectangular() : Boolean {
    val firstSize = get(0).size
    forEach { row ->
        if (row.size != firstSize) {
            return false
        }
    }
    return true
}

fun String.toMatrix() : Matrix {
    return split("\n").map { row ->
        Vector(row.split(" ").mapNotNull { it.toDoubleOrNull() }.toTypedArray())
    }.toTypedArray()
}