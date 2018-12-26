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
        private val nodeCounts : Array<Int>,
        private val biases : Matrix,
        private val weights : Array<Matrix>) {

    private val layerCount: Int = biases.size
    val outputsSize = nodeCounts[nodeCounts.size - 1]

    init {
        if (nodeCounts.size - 1 != layerCount) throw RuntimeException("${nodeCounts.size - 1} != $layerCount")
        if (weights.size != layerCount) throw RuntimeException("${weights.size} != $layerCount")
        weights.forEach { weight ->
            if (!weight.isRectangular) throw RuntimeException()
        }
    }

    fun train(
            trainingData : List<Pair<Vector, Vector>>,
            eta : Double,
            epochs: Int,
            batchSize: Int,
            report : () -> Unit) {
        val td = trainingData.toMutableList()
        val state = PropState(
                nablaBiases = biases.map { biases -> biases.toZeroes() },
                nablaWeights = weights.map { matrix -> matrix.toZeroes() }.toTypedArray(),
                inputs = nodeCounts.drop(1).map { count -> vectorOfSize(count) }.toTypedArray(),
                activations = nodeCounts.map { count -> vectorOfSize(count) }.toTypedArray())
        report()
        for (i in 0 until epochs) {
            td.shuffle()
            td.windowed(batchSize, batchSize, true) { batch ->
                updateBatch(batch, eta, state)
            }
            report()
        }
    }

    fun feedForward(a: Vector): Vector {
        var output = vecToMatrix(a)
        for (i in 0 until layerCount) {
            val wa = weights[i] dot output
            wa += biases[i]
            wa.assignInto { value -> sigmoid(value) }
            output = wa
        }
        return matrixToVec(output)
    }

    private fun updateBatch(
            trainingData: List<Pair<Vector, Vector>>,
            eta: Double,
            state: PropState) {

        trainingData.forEach { (input, output) ->
            backPropagate(input, output, state)
        }

        val etaFraction = eta/trainingData.size
        (0 until weights.size).forEach { i ->
            val nw = state.nablaWeights[i]
            nw *= etaFraction
            weights[i] -= nw
        }
        (0 until biases.size).onEach { i ->
            val nb = state.nablaBiases[i]
            nb *= etaFraction
            biases[i] -= nb
        }
    }

    class PropState(
            val nablaBiases : Matrix,
            val nablaWeights : Array<Matrix>,
            val inputs: Array<Vector>,
            val activations : Array<Vector>
    )

    private fun backPropagate(
            input: Vector,
            output: Vector,
            state : PropState) {
        with(state) {

            // Forward propagate
            copyInto(activations[0], input)
            for (layer in 0 until layerCount) {
                val inputs = inputs[layer]
                val activationsOut = activations[layer + 1]
                val activations = activations[layer]
                val weights = weights[layer]
                val biases = biases[layer]
                for (j in 0 until weights.height) {
                    var sum = 0.0
                    for (k in 0 until weights.width) {
                        sum += weights[j][k] * activations[k]
                    }
                    val result = sum + biases[j]
                    inputs[j] = sigmoidPrime(result)
                    activationsOut[j] = sigmoid(result)
                }
            }

            // Backward propagate
            copyInto(nablaBiases[layerCount - 1], activations[layerCount])
            nablaBiases[layerCount - 1] -= output
            nablaBiases[layerCount - 1] *= inputs[layerCount - 1]

            for (backwardLayer in 1 until nodeCounts.size) {
                val layer = layerCount - backwardLayer
                val nb = nablaBiases[layer]
                val nw = nablaWeights[layer]
                val activations = activations[layer]
                if (backwardLayer > 1) {
                    updateNablaBiases(
                            nablaBiases = nb,
                            nablaBiasesPrior = nablaBiases[layer + 1],
                            weights = weights[layer + 1],
                            inputs = inputs[layer])
                }
                updateNablaWeights(
                        nablaWeights = nw,
                        nablaBiases = nb,
                        activations = activations)
            }
        }
    }


    private fun copyInto(target : Vector, source : Vector) {
        for (i in 0 until target.size) {
            target[i] = source[i]
        }
    }

    private fun updateNablaWeights(nablaWeights : Matrix, nablaBiases: Vector, activations: Vector) {
        for (i in 0 until nablaBiases.size) {
            val vec = nablaWeights[i]
            for (j in 0 until activations.size) {
                vec[j] = nablaBiases[i] * activations[j]
            }
        }
    }

    private fun updateNablaBiases(nablaBiases : Vector, nablaBiasesPrior : Vector, weights : Matrix, inputs : Vector) {
        for (i in 0 until weights.width) {
            var sum = 0.0
            for (j in 0 until weights.height) {
                sum += weights[j][i] * nablaBiasesPrior[j]
            }
            nablaBiases[i] = sum * inputs[i]
        }
    }

    private fun sigmoidPrime(value : Double) : Double {
        val s = sigmoid(value)
        return s * (1.0 - s)
    }

    companion object {
        fun fromNodeCounts(nodeCounts : Array<Int>) : Network {
            val layerCount = nodeCounts.size - 1
            val random = Random()
            val biases = Matrix(layerCount) {layer ->
                Vector(DoubleArray(nodeCounts[layer + 1]) { random.nextGaussian() })
            }

            val weights = (0 until layerCount).map { Matrix(nodeCounts[it + 1], nodeCounts[it]) { _,_ -> random.nextGaussian() }}.toTypedArray()
            return Network(nodeCounts, biases, weights)
        }
    }
}

