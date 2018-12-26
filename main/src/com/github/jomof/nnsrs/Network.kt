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
        val nablaBiases = biases.map { biases -> biases.toZeroes() }
        val nablaWeights : Array<Matrix> = weights.map { matrix -> matrix.toZeroes() }.toTypedArray()
        report()
        for (i in 0 until epochs) {
            td.shuffle()
            //println("Epoch $i")
            var w = 0
            td.windowed(batchSize, batchSize, true) { batch ->
                //println("  window $w")
                updateBatch(batch, eta, nablaBiases, nablaWeights)
                ++w
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
            nablaBiases: Matrix,
            nablaWeights: Array<Matrix>) {

        trainingData.forEach { (input, output) ->
            backPropagate(input, output, nablaBiases, nablaWeights)
        }

        val etaFraction = eta/trainingData.size
        (0 until weights.size).forEach { i ->
            val nw = nablaWeights[i]
            nw *= etaFraction
            weights[i] -= nw
        }
        (0 until biases.size).onEach { i ->
            val nb = nablaBiases[i]
            nb *= etaFraction
            biases[i] -= nb
        }
    }

    private fun backPropagate(
            activation: Vector,
            output: Vector,
            nablaBiases: Matrix,
            nablaWeights: Array<Matrix>) {
        nablaBiases *= 0.0
        nablaWeights.forEach { matrix -> matrix *= 0.0 }

        val activations = Array<Matrix?>(layerCount + 1) { null }
        val inputs = Matrix(layerCount) { vectorOf() }
        activations[0] = vecToMatrix(activation)

        for (i in 0 until layerCount) {
            val wa = weights[i] dot activations[i]!!
            wa += biases[i]
            inputs[i] = sigmoidPrime(wa)
            wa.assignInto { _, _, value -> sigmoid(value) }
            activations[i + 1] = wa
        }

        //compute error of last layer
        var delta = matrixToVec(activations[activations.size - 1]!!)
        delta -= output
        delta *= inputs[inputs.size - 1]
        nablaBiases[nablaBiases.size - 1] = delta
        mult(nablaWeights[nablaWeights.size - 1], delta, activations[activations.size - 2]!!)

        for (L in 2 until nodeCounts.size) {
            delta = updateDelta(
                    delta,
                    weights[weights.size - L + 1],
                    inputs[inputs.size - L])

            nablaBiases[nablaBiases.size - L] = delta
            mult(nablaWeights[nablaWeights.size - L], delta, activations[activations.size - L - 1]!!)
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

    fun updateDelta(delta : Vector, weights : Matrix, sp : Vector) : Vector {
        return Vector(DoubleArray(weights.width) { i ->
            var sum = 0.0
            for (k in 0 until weights.height) {
                sum += weights[k][i] * delta[k]
            }
            sum * sp[i]
        })
    }

    private fun sigmoidPrime(z : Matrix) : Vector {
        return Vector(DoubleArray(z.size) { i ->
            val s = sigmoid(z[i][0])
            s * (1.0 - s)
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

