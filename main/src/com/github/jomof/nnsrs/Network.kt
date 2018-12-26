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
                activations = nodeCounts.map { count -> vecToMatrix(vectorOfSize(count)) }.toTypedArray())
        report()
        for (i in 0 until epochs) {
            td.shuffle()
            var w = 0
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
            val activations : Array<Matrix>
    ) {

    }

    private fun backPropagate(
            activation: Vector,
            output: Vector,
            state : PropState) {
        with(state) {
            nablaBiases *= 0.0
            nablaWeights.forEach { matrix -> matrix *= 0.0 }

            writeIntoMatrix(activations[0], activation)

            for (i in 0 until layerCount) {
                forwardPropagate(
                        state,
                        i,
                        weights[i],
                        biases[i])
            }

            //compute error of last layer
            writeIntoVec(nablaBiases[nablaBiases.size - 1], activations[activations.size - 1])
            nablaBiases[nablaBiases.size - 1] -= output
            nablaBiases[nablaBiases.size - 1] *= inputs[inputs.size - 1]
            mult(
                    nablaWeights[nablaWeights.size - 1],
                    nablaBiases[nablaBiases.size - 1],
                    activations[activations.size - 2])

            for (L in 2 until nodeCounts.size) {
                updateDelta(
                        nablaBiases[nablaBiases.size - L],
                        nablaBiases[nablaBiases.size - L + 1],
                        weights[weights.size - L + 1],
                        inputs[inputs.size - L])
                mult(
                        nablaWeights[nablaWeights.size - L],
                        nablaBiases[nablaBiases.size - L],
                        activations[activations.size - L - 1])
            }
        }
    }

    fun writeIntoMatrix(matrix : Matrix, vector : Vector) {
        for (i in 0 until vector.size) {
            matrix[i][0] = vector[i]
        }
    }

    fun writeIntoVec(target : Vector, source : Matrix) {
        for (i in 0 until target.size) {
            target[i] = source[i][0]
        }
    }

    fun forwardPropagate(
            thiz : PropState,
            layer : Int,
            weights : Matrix,
            biases : Vector) {
        val inputs = thiz.inputs[layer]
        val activationsOut = thiz.activations[layer + 1]
        val activations = thiz.activations[layer]
        for (i in 0 until weights.height) {
            var sum = 0.0
            for (k in 0 until weights.width) {
                sum += weights[i][k] * activations[k][0]
            }
            val result = sum + biases[i]
            inputs[i] = sigmoidPrime(result)
            activationsOut[i][0] = sigmoid(result)
        }
    }

    private fun mult(weights : Matrix, delta: Vector, right: Matrix) {
        for (i in 0 until delta.size) {
            val vec = weights[i]
            for (j in 0 until right.height) {
                vec[j] = delta[i] * right[j][0]
            }
        }
    }

    fun updateDelta(biases : Vector, delta : Vector, weights : Matrix, sp : Vector) {
        for (i in 0 until weights.width) {
            var sum = 0.0
            for (j in 0 until weights.height) {
                sum += weights[j][i] * delta[j]
            }
            biases[i] = sum * sp[i]
        }
    }
    private fun sigmoidPrime(value : Double) : Double {
        val s = sigmoid(value)
        return s * (1.0 - s)
    }

    private fun sigmoidPrime(z : Matrix) : Vector {
        return Vector(DoubleArray(z.size) { i ->
            sigmoidPrime(z[i][0])
        })
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

