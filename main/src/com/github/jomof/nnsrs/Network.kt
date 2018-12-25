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
        private var biases : Matrix,
        private var weights : Array<Matrix>) {

    private val layerCount: Int = biases.size
    val outputsSize = nodeCounts[nodeCounts.size - 1]

    init {
        if (nodeCounts.size - 1 != layerCount) throw RuntimeException("${nodeCounts.size - 1} != $layerCount")
        if (weights.size != layerCount) throw RuntimeException("${weights.size} != $layerCount")
        weights.forEach { weight ->
            if (!weight.isRectangular()) throw RuntimeException()
        }
    }

    fun train(trainingData : List<Pair<Vector, Vector>>, eta : Double, epochs: Int, batchSize: Int, report : () -> Unit) {
        val td = trainingData.toMutableList()
        val nablaBiases : Matrix = biases.map { biases -> biases.toZeroes() }.toTypedArray()
        val nablaWeights : Array<Matrix> = weights.map { matrix -> matrix.toZeroes() }.toTypedArray()
        report()
        for (i in 0 until epochs) {
            td.shuffle()
            td.windowed(batchSize, batchSize, true).forEach { batch ->
                updateBatch(batch, eta, nablaBiases, nablaWeights)
            }
            report()
        }
    }

    fun feedForward(a: Vector): Vector {
        var output = vecToMatrix(a)
        for (i in 0 until layerCount) {
            val wa = weights[i] dot output
            wa.plusAssign(biases[i])
            output = wa.sigmoid()
        }
        return matrixToVec(output)
    }

    private fun updateBatch(
            trainingData: List<Pair<Vector, Vector>>,
            eta: Double,
            nablaBiases: Matrix,
            nablaWeights: Array<Matrix>) {
        val inputs = trainingData.map { it.first }
        val outputs = trainingData.map { it.second }

        inputs.zip(outputs).forEach { (input, output) ->
            backPropagate(input, output, nablaBiases, nablaWeights)
            nablaBiases.plusAssign(nablaBiases)
            nablaWeights.zip(nablaWeights).onEach { (nw, dnw) -> nw.plusAssign(dnw) }
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

    private fun backPropagate(
            a_activation: Vector,
            a_output: Vector,
            nablaBiases: Matrix,
            nablaWeights: Array<Matrix>) {
        nablaBiases.clear()
        nablaWeights.forEach { matrix -> matrix.clear() }
        val activation : Vector = a_activation
        val y : Vector = a_output

        val activations : Matrix= matrix(nodeCounts.size, 0)
        val inputs : Matrix= matrix(layerCount, 0)
        activations[0] = activation

        var output : Matrix= vecToMatrix(activation)

        for (i in 0 until layerCount) {
            val wa = weights[i] dot output
            wa.plusAssign(biases[i])
            inputs[i] = matrixToVec(wa)
            output = wa.sigmoid()
            activations[i + 1] = matrixToVec(output)
        }

        //compute error of last layer
        var delta = activations[activations.size - 1] minus y

        delta = delta hadamard sigmoidPrime(inputs[inputs.size - 1])
        nablaBiases[nablaBiases.size - 1] = delta.copyOf()
        nablaWeights[nablaWeights.size - 1] = vecToMatrix(delta) dot vecToMatrix(activations[activations.size - 2]).transpose()

        for (L in 2 until nodeCounts.size) {
            val z = inputs[inputs.size - L]
            val sp = sigmoidPrime(z)
            val transposed = weights[weights.size - L + 1].transpose()

            delta = matrixToVec(transposed dot delta)
            delta = delta hadamard sp

            nablaBiases[nablaBiases.size - L] = delta.copyOf()
            nablaWeights[nablaWeights.size - L] = vecToMatrix(delta) dot vecToMatrix(activations[activations.size - L - 1]).transpose()
        }
    }

    private fun sigmoidPrime(z : Vector) : Vector {
        val a = z.map { sigmoid(it) }
        val b = (a times -1.0) plus 1.0
        return a hadamard b
    }

    companion object {
        fun fromNodeCounts(nodeCounts : Array<Int>) : Network {
            val layerCount = nodeCounts.size - 1
            val random = Random()
            val biases = (0 until layerCount).map { Vector(nodeCounts[it + 1]) { random.nextGaussian() } }.toTypedArray()
            val weights = (0 until layerCount).map { matrix(nodeCounts[it + 1], nodeCounts[it]) { _,_ -> random.nextGaussian() }}.toTypedArray()
            return Network(nodeCounts, biases, weights)

        }
    }
}