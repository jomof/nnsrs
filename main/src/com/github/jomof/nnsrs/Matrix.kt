package com.github.jomof.nnsrs

import java.lang.RuntimeException

typealias Vector = Array<Double>
typealias Matrix = Array<Vector>


infix fun Vector.dot(right : Vector) : Double {
    if (size != right.size) {
        throw RuntimeException("Dot of mismatched vectors")
    }
    return (this zip right).sumByDouble { (l,r) -> l * r}
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