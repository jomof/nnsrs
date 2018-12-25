package com.github.jomof.nnsrs

class Vector(private val vec : Array<Double>) {

    constructor( height : Int,
                 init : (Int) -> Double) : this(Array(height, init))

    val size get() = vec.size
    operator fun get(index : Int) = vec[index]
    operator fun set(index : Int, value : Double) { vec[index] = value }

    fun map(transform: (Double) -> Double): Vector {
        return Vector(vec.map(transform).toTypedArray())
    }

    infix fun plus(right : Vector) = Vector(size) { this[it] + right[it] }
    infix fun plus(right : Double) = map { it + right }

    infix fun minus(right : Vector) = Vector(size) { this[it] - right[it] }
    infix fun minus(right : Double) = map { it - right }

    infix fun hadamard(right : Vector) = Vector(size) { this[it] * right[it] }
    infix fun times(right : Double) = map { it * right }

    fun copyOf() = map { it }
    fun toZeroes() = map { 0.0 }
}