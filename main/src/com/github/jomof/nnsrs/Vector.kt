package com.github.jomof.nnsrs

class Vector(val vec : DoubleArray) {
    inline val size get() = vec.size

    operator fun get(index : Int) = vec[index]
    operator fun set(index : Int, value : Double) { vec[index] = value }

    inline fun map(transform: (Double) -> Double): Vector {
        return Vector(vec.map(transform).toDoubleArray())
    }

    inline fun assignInto(f : (Int, Double) -> Double) {
        for (i in 0 until size) {
            this[i] = f(i, this[i])
        }
    }

    operator fun minusAssign(right : Double) = assignInto { _, v -> v - right}
    operator fun minusAssign(right : Vector) = assignInto { i, v -> v - right[i] }
    operator fun plusAssign(right : Double) = assignInto { _, v -> v + right}
    operator fun plusAssign(right : Vector) = assignInto { i, v -> v + right[i] }
    operator fun timesAssign(right : Double) = assignInto { _, v -> v * right}
    operator fun timesAssign(right : Vector) = assignInto { i, v -> v * right[i]}

    fun toZeroes() = Vector(DoubleArray(size) { 0.0 })
}

fun vectorOf(vararg args : Double) : Vector = Vector(args.map { it }.toDoubleArray())

