package com.github.jomof.nnsrs

import java.lang.RuntimeException
import java.util.*

fun <E> List<E>.sample(sampleSize : Int, random : Random = Random()) : Sequence<E> {
    if (size < sampleSize) throw RuntimeException()
    if (size == sampleSize) return this.asSequence()
    val seen = mutableSetOf<Int>()
    while(seen.size != sampleSize) {
        seen += random.nextInt(sampleSize)
    }
    return seen.asSequence().map { get(it) }
}