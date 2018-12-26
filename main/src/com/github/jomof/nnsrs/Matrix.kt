package com.github.jomof.nnsrs


open class Matrix {
    val mat : Array<Vector>
    val isRectangular : Boolean

    constructor(mat : Array<Vector>) {
        this.mat = mat
        this.isRectangular = isRectangular(mat)
    }

    constructor(
            height : Int,
            width : Int,
            init : (Int, Int) -> Double) {
        this.mat = Array(height) { x ->
            Vector(DoubleArray(width) { y -> init(x, y) } )
        }
        this.isRectangular = true
    }

    constructor(
            height : Int,
            init : (Int) -> Vector) {
        this.mat =  Array(height) { i -> init(i) }
        this.isRectangular = isRectangular(mat)
    }

    inline val size get() = mat.size

    inline val height get() = mat.size

    inline val width get() = this[0].size


    operator fun get(index : Int) = mat[index]
    operator fun set(index : Int, value : Vector) { mat[index] = value }

    fun transpose() : Matrix {
        return Matrix(Array(width) { i ->
            Vector(DoubleArray(height) { j ->
                this[j][i]
            })
        })
    }

    inline fun map(transform: (Vector) -> Vector): Matrix {
        return Matrix(mat.map(transform).toTypedArray())
    }

    inline fun assignInto(f : (Int, Int, Double) -> Double) {
        for (i in 0 until height) {
            this[i].assignInto { j, value ->
                f(i, j, value)
            }
        }
    }

    inline fun assignInto(f : (Double) -> Double) {
        for (i in 0 until height) {
            this[i].assignInto { _, value ->
                f(value)
            }
        }
    }

    infix fun dot(right : Matrix) : Matrix {
        return Matrix(Array(height) { i ->
            val thisVector = this[i]
            Vector(DoubleArray(right.width) { j ->
                var sum = 0.0
                for (k in 0 until width) {
                    sum += thisVector[k] * right[k][j]
                }
                sum
            })
        })
    }

    operator fun timesAssign(right : Double) = assignInto { _, _, value -> value * right }

    operator fun minusAssign(right : Matrix) = assignInto { i, j, value -> value - right[i][j] }
    operator fun minusAssign(right : Double) = assignInto { _, _, value -> value - right }

    operator fun plusAssign(right : Matrix) = assignInto { i, j, value -> value + right[i][j] }
    operator fun plusAssign(right : Vector) = assignInto { i, _, value -> value + right[i] }
    operator fun plusAssign(right : Double) = assignInto { _, _, value -> value + right }


    fun toZeroes() : Matrix {
        return map { it.toZeroes() }
    }

    companion object {
        private fun isRectangular(mat: Array<Vector>): Boolean {
            val firstSize = mat[0].size
            mat.forEach { row ->
                if (row.size != firstSize) {
                    return false
                }
            }
            return true
        }
    }
}


fun vecToMatrix(input: Vector): Matrix {
    // input[size] into output[size][1]
    return Matrix(Array(input.size) { i ->
        Vector(DoubleArray(1) { j ->
            input[i]
        })
    })
}

fun matrixToVec(input: Matrix): Vector {
    return Vector(DoubleArray(input.size) { i ->
        input[i][0]
    })
}

fun String.toMatrix() : Matrix {
    return Matrix(split("\n").map { row ->
        Vector(row.split(" ").mapNotNull { it.toDoubleOrNull() }.toDoubleArray())
    }.toTypedArray())
}