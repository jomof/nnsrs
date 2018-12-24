package com.github.jomof.nnsrs

import kotlin.RuntimeException

typealias Vector = Array<Double>
typealias Matrix = Array<Vector>

fun assertSameShape(left : Vector, right : Vector) {
    if (left.size != right.size) throw RuntimeException("${left.size} != ${right.size}")
}

fun vectorOf(vararg args : Double) : Vector = args.map { it }.toTypedArray()

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
    return vector(input.size) { i ->
        input[i][0]
    }
}

fun vector(
        height : Int,
        init : (Int) -> Double = { _ -> 0.0 }) : Vector {
    return Array(height, init)
}

fun matrix(
        height : Int,
        width : Int,
        init : (Int, Int) -> Double = { _, _ -> 0.0 }) : Matrix {
    return Array(height) { x -> Array(width) { y -> init(x, y) } }
}

fun Vector.transpose() : Matrix {
    return vecToMatrix(this).transpose()
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

infix fun Vector.times(right : Double) : Vector {
    return map { it * right }.toTypedArray()
}

infix fun Matrix.times(right : Double) : Matrix {
    return map { it times right }.toTypedArray()
}

infix fun Matrix.hadamard(right: Matrix): Matrix {

    val a = matrixToVec(this)
    val b = matrixToVec(right)

    return vecToMatrix(a hadamard b)
}

infix fun Vector.hadamard(right: Vector) = vector(size) { i ->
    this[i] * right[i]
}

infix fun Double.minus(right : Vector) : Vector {
    return right.map { it - this }.toTypedArray()
}

infix fun Vector.minus(right : Double) : Vector {
    return map { it - right }.toTypedArray()
}

infix fun Vector.minus(right : Vector) : Vector {
    assertSameShape(this, right)
    return zip(right).map { (l, r) -> l - r }.toTypedArray()
}

infix fun Vector.plus(b : Double) : Vector {
    return map { it + b }.toTypedArray()
}

infix fun Vector.plus(right : Vector) : Vector {
    return zip(right).map { (l, r) -> l + r }.toTypedArray()
}

infix fun Matrix.plus(right : Matrix) : Matrix {
    return zip(right).map { (l, r) -> l plus r }.toTypedArray()
}

infix fun Matrix.plus(right : Vector) : Matrix {
    //wrapper method to add two arrays together element-wise
    //shape of the arrays is a[size][1] and b[size]
    if (width() != 1) throw RuntimeException()
    if (height() != right.size) throw RuntimeException()
    return matrix(height(), 1) { i, _ ->
        this[i][0] + right[i]

    }
}

infix fun Matrix.minus(right : Matrix) : Matrix {
    return zip(right).map { (l, r) -> l minus r }.toTypedArray()
}

fun Vector.toZeroes() : Vector {
    return (0 until size).map { 0.0 }.toTypedArray()
}

fun Matrix.toZeroes() : Matrix {
    return map { it.toZeroes() }.toTypedArray()
}

infix fun Vector.dot(right : Vector) : Double {
    if (size != right.size) {
        throw RuntimeException("Dot of mismatched vectors")
    }
    return (this zip right).sumByDouble { (l,r) -> l * r}
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

fun Vector.sigmoid() : Vector {
    return map { sigmoid(it) }.toTypedArray()
}

fun Matrix.sigmoid() : Matrix {
    return map { it.sigmoid() }.toTypedArray()

}


fun vectorOfSize(size : Int) : Vector {
    return (0 until size).map { 0.0 }.toTypedArray()
}

fun Matrix.height() = size

fun Matrix.width() : Int {
    if (!isRectangular()) {
        throw RuntimeException("Not rectangular")
    }
    return get(0).size
}

fun Matrix.rowSizes() = map { it.size }

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
        row.split(" ").mapNotNull { it.toDoubleOrNull() }.toTypedArray()
    }.toTypedArray()
}