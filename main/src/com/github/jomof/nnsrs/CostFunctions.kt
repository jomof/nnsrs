package com.github.jomof.nnsrs

fun reportBoolCost(network : Network, trainingData: List<Pair<Vector, Vector>>) : () -> Unit {
    var iteration = 0
    return {
        if (iteration % 1000 == 0) {
            val cost = cost(network, trainingData)
            val costBool = costBool(network, trainingData)
            if (iteration == 0) {
                println("Start: cost = $cost, costBool = $costBool")
            } else {
                println("Iteration $iteration: cost = $cost, costBool = $costBool")
            }
        }
        ++iteration
    }
}

fun cost(network : Network, trainingData : List<Pair<Vector, Vector>>) : Double {
    val samples = trainingData.map { it.first }
    val answers = trainingData.map { it.second }

    if (samples.size != answers.size) {
        throw RuntimeException()
    }
    val outputsSize = network.outputsSize
    val sampleSize = samples.size
    return (0 until sampleSize).sumByDouble { index ->
        val sample = samples[index]
        val answer = answers[index]
        val output: Vector = network.feedForward(sample)
        (0 until outputsSize).sumByDouble { outputIndex ->
            val out: Double = output[outputIndex]
            val ans: Double = answer[outputIndex]
            val diff = out - ans
            diff * diff
        }
    } / (2.0 * sampleSize)
}


fun costBool(network : Network, trainingData : List<Pair<Vector, Vector>>) : Double {
    val samples = trainingData.map { it.first }
    val answers = trainingData.map { it.second }

    if (samples.size != answers.size) {
        throw RuntimeException()
    }
    val outputsSize = network.outputsSize
    val sampleSize = samples.size
    var correct = 0
    var total = 0
    (0 until sampleSize).onEach { index ->
        val sample = samples[index]
        val answer = answers[index]
        val output: Vector = network.feedForward(sample)
        (0 until outputsSize).onEach { outputIndex ->
            val out: Double = output[outputIndex]
            val ans: Double = answer[outputIndex]
            val ab = ans > 0.5
            val ao = out > 0.5
            ++total
            correct += if (ab == ao) 1 else 0
        }
    }
    return (0.0 + total - correct) / (0.0 + total)
}