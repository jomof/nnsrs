package com.github.jomof.nnsrs


fun sigmoid(value : Double) : Double {
    return 1.0 / (1.0 + Math.exp(-value))
}

fun sigmoidPrime(value : Double) : Double {
    val s = sigmoid(value)
    return s * (1.0 - s)
}