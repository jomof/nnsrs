package com.github.jomof.nnsrs

import java.util.*

/**
 * values -
 *   Not necessarily rectangular.
 *   Height is number of layers + 1 for inputs
 *
 *   row 0 are the inputs, height is number
 *   row 1 .. N-1 are the computed node outputs
 *   row N is the final output
 *
 * biases -
 *   One per layer.
 *   n = The bias of the n-th node of the given layer
 *
 * weights -
 *   One per layer, each layer is rectangular.
 *   For each layer,
 *     width is number of input nodes
 *     height is number of output nodes
 */
class Network(
        private val nodeCounts: Array<Int>,
        private var biases: Matrix,
        private var weights: Array<Matrix>) {

    private val layerCount: Int = biases.size

    init {
        if (nodeCounts.size - 1 != layerCount) throw RuntimeException("${nodeCounts.size - 1} != $layerCount")
        if (weights.size != layerCount) throw RuntimeException("${weights.size} != $layerCount")
        weights.forEach { weight ->
            if (!weight.isRectangular()) throw RuntimeException()
        }

    }

    fun randomize() {
        val random = Random()
        biases.forEach { layerBiases ->
            (0 until layerBiases.size).onEach { node ->
                layerBiases[node] = random.nextGaussian()
            }
        }
        weights.forEach { layerWeights ->
            (0 until layerWeights.height()).zip(0 until layerWeights.width()).forEach { (x, y) ->
                layerWeights[x][y] = random.nextGaussian()
            }
        }
    }

    fun cost(trainingData : List<Pair<Vector, Vector>>) : Double {
        val samples = trainingData.map { it.first }
        val answers = trainingData.map { it.second }

        if (samples.size != answers.size) {
            throw RuntimeException()
        }
        val outputsSize = nodeCounts[nodeCounts.size - 1]
        val sampleSize = samples.size
        return (0 until sampleSize).sumByDouble { index ->
            val sample = samples[index]
            val answer = answers[index]
            val output: Vector = feedforward(sample)
            (0 until outputsSize).sumByDouble { outputIndex ->
                val out: Double = output[outputIndex]
                val ans: Double = answer[outputIndex]
                val diff = out - ans
                diff * diff
            }
        } / (2.0 * sampleSize)
        return 0.0
    }

    fun costBool(trainingData : List<Pair<Vector, Vector>>) : Double {
        val samples = trainingData.map { it.first }
        val answers = trainingData.map { it.second }

        if (samples.size != answers.size) {
            throw RuntimeException()
        }
        val outputsSize = nodeCounts[nodeCounts.size - 1]
        val sampleSize = samples.size
        var correct = 0
        var total = 0
        (0 until sampleSize).onEach { index ->
            val sample = samples[index]
            val answer = answers[index]
            val output: Vector = feedforward(sample)
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

    fun train(trainingData : List<Pair<Vector, Vector>>, eta : Double, epochs: Int, mini_batch_size: Int) {
        val td = trainingData.toMutableList()
        val n = trainingData.size
        val numOfMiniBatches = n / mini_batch_size
        val startingCost = cost(trainingData)
        val startingCostBool = costBool(trainingData)
        println("Starting costBool = $startingCostBool, cost = $startingCost")
        var lastNow = System.currentTimeMillis()
        for (i in 0 until epochs) {
            td.shuffle()
            var start = 0

            var j = 0
            while (j < numOfMiniBatches) {
                val mini_batch = td.drop(start).take(mini_batch_size)
                updateMinibatch(mini_batch, eta)
                start += mini_batch_size
                j += mini_batch_size
            }
            if ((i % 2001) == 2001 || (System.currentTimeMillis() - lastNow) > 5000) {
                val cost = cost(trainingData)
                val costBool = costBool(trainingData)
                println("Epoch $i complete, costBool = $costBool, cost = $cost")
                lastNow = System.currentTimeMillis()
            }
        }
        println("Training Complete")
    }

    fun feedforward(a: Array<Double>): Array<Double> {
        var output = vecToMatrix(a)
        for (i in 0 until layerCount) {
            val wa = weights[i] dot output
            val z = wa plus biases[i]
            output = z.sigmoid()
        }
        return matrixToVec(output)
    }

    fun updateMinibatch(trainingData : List<Pair<Vector, Vector>>, eta : Double) {
        val inputs = trainingData.map { it.first }
        val outputs = trainingData.map { it.second }
        var nablaBiases : Matrix = biases.map { biases -> biases.toZeroes() }.toTypedArray()
        var nablaWeights : Array<Matrix> = weights.map { matrix -> matrix.toZeroes() }.toTypedArray()

        inputs.zip(outputs).forEach { (input, output) ->
            val (deltaNablaB, deltaNableW) = backprop(input, output)
            nablaBiases = nablaBiases plus deltaNablaB
            nablaWeights = nablaWeights.zip(deltaNableW).map { (nw, dnw) -> nw plus dnw }.toTypedArray()
        }

        val etaFraction = eta/inputs.size
        val newW = weights.zip(nablaWeights).map{ (w,nw) ->
            val t = nw times etaFraction
            val result = w minus t
            result
        }.toTypedArray()

        val newB = biases.zip(nablaBiases).map { (b, nb) ->
            b minus (nb times etaFraction)
        }.toTypedArray()

        weights = newW
        biases = newB
    }

    fun backprop(a_activation : Vector, a_output : Vector) : Pair<Matrix, Array<Matrix>> {
        if (a_activation.size != nodeCounts[0]) throw RuntimeException("${a_activation.size} != ${nodeCounts[0]}")
        if (a_output.size != nodeCounts[nodeCounts.size - 1]) throw RuntimeException("${a_activation.size} != ${nodeCounts[0]}")

        val nb : Matrix = biases.map { biases -> biases.toZeroes() }.toTypedArray()
        val nw : Array<Matrix> = weights.map { matrix -> matrix.toZeroes() }.toTypedArray()

        val activation : Array<Double> = a_activation
        val y : Array<Double> = a_output

        val activations : Array<Array<Double>> = matrix(nodeCounts.size, 0)
        val inputs : Array<Array<Double>> = matrix(layerCount, 0)
        activations[0] = activation

        var output : Array<Array<Double>> = vecToMatrix(activation)

        for (i in 0 until layerCount) {
            val wa = weights[i] dot output
            val layer_input = wa plus biases[i]
            inputs[i] = matrixToVec(layer_input)
            output = layer_input.sigmoid()
            activations[i + 1] = matrixToVec(output)
        }

        //compute error of last layer
        var delta = cost_derivative(activations[activations.size - 1], y)

        delta = delta hadamard sigmoid_prime(inputs[inputs.size - 1])
        nb[nb.size - 1] = delta.copyOf()
        nw[nw.size - 1] = vecToMatrix(delta) dot vecToMatrix(activations[activations.size - 2]).transpose()

        for (L in 2 until nodeCounts.size) {
            val z = inputs[inputs.size - L]
            val sp = sigmoid_prime(z)
            val transposed = weights[weights.size - L + 1].transpose()

            delta = matrixToVec(transposed dot delta)
            delta = delta hadamard sp

            nb[nb.size - L] = delta.copyOf()
            nw[nw.size - L] = vecToMatrix(delta) dot vecToMatrix(activations[activations.size - L - 1]).transpose()
        }
        return Pair(nb, nw)
    }

    fun cost_derivative(output_activation: Vector, y: Vector): Vector {
        assertSameShape(output_activation, y)
        return output_activation minus y
    }

    private fun sigmoid_prime(z : Vector) : Vector {
        val a = z.sigmoid()
        val b = (a times -1.0) plus 1.0
        return a hadamard b
    }

    companion object {
        fun fromNodeCounts(nodeCounts : Array<Int>) : Network {
            val layerCount = nodeCounts.size - 1
            val random = Random()
            val biases = (0 until layerCount).map { vector(nodeCounts[it + 1]) { random.nextGaussian() }}.toTypedArray()
            val weights = (0 until layerCount).map { matrix(nodeCounts[it + 1], nodeCounts[it]) { _,_ -> random.nextGaussian() }}.toTypedArray()
            return Network(nodeCounts, biases, weights)

        }

//        fun fromNodeCounts(nodeCounts: List<Int>): Network {
//            val biases = vectorOfSize(nodeCounts.size - 1)
//            val values = nodeCounts.map { vectorOfSize(it) }.toTypedArray()
//            val weights = biases.mapIndexed { index, _ ->
//                val width = nodeCounts[index]
//                val height = nodeCounts[index + 1]
//                (0 until height).map { vectorOfSize(width) }.toTypedArray()
//            }.toTypedArray()
//            return Network(values, biases, weights)
//        }

//        fun fromValues(values: Matrix): Network {
//            val nodeCounts = values.map { it.size }
//            val network = fromNodeCounts(nodeCounts)
//            network.setValues(values)
//            return network
//        }
    }
}