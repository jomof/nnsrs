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
 *   n = the bias of the given layer of nodes
 *
 * weights -
 *   One per layer, each layer is rectangular.
 *   For each layer,
 *     width is number of input nodes
 *     height is number of output nodes
 */
class Network(
        private var values : Matrix,
        private val biases : Vector,
        private val weights : Array<Matrix>) {
    private val layerCount : Int = biases.size

    init {
        if (layerCount != weights.size) {
            throw RuntimeException("Biases size was not the same as layers size")
        }
        if (layerCount + 1 != values.size) {
            throw RuntimeException("Values was not size of layers plus one")
        }
        (0 until layerCount).forEach { layer ->
            if (!weights[layer].isRectangular()) {
                throw RuntimeException("Expected rectangular layer weights")
            }
        }
        (0 until layerCount).forEach { layer ->
            val layerWeights = weights[layer]
            val inputs = values[layer]
            val outputs = values[layer + 1]
            if (layerWeights.height() != outputs.size) {
                throw RuntimeException("Layer weights did not have same height as outputs")
            }
            if (layerWeights.width() != inputs.size) {
                throw RuntimeException("Layer weights did not have same width as inputs")
            }
        }
    }

    fun setValues(values : Matrix) {
        if (values.rowSizes() != this.values.rowSizes()) {
            throw RuntimeException("Replacement row sizes weren't the same")
        }
        this.values = values
    }

    fun setInputs(inputs : Vector) {
        if (values[0].size != inputs.size) {
            throw RuntimeException("Wrong input vector size")
        }
        values[0] = inputs
    }

    fun outputs() : Vector {
        return values[values.size - 1]
    }

    fun feedForward(inputs : Vector) : Vector {
        setInputs(inputs)
        weights.forEachIndexed { layerIndex, layerWeights ->
            val layerInputs = values[layerIndex]
            val layerOutputs = values[layerIndex + 1]
            val bias = biases[layerIndex]
            layerWeights.forEachIndexed { outputIndex, inputWeights ->
                assert(inputWeights.size == layerInputs.size)
                layerOutputs[outputIndex] = 1.0 / (1.0 + Math.exp(- (layerInputs dot inputWeights) - bias))
            }
        }
        return outputs()
    }

    fun randomize() {
        val random = Random()
        values.forEach { layer ->
            (0 until layer.size).onEach { node ->
                layer[node] = random.nextDouble()
            }
        }
        (0 until biases.size).forEach { layer ->
            biases[layer] = random.nextDouble()
        }
        weights.forEach { layerWeights ->
            (0 until layerWeights.height()).zip(0 until layerWeights.width()).forEach { (x,y) ->
                layerWeights[x][y] = random.nextDouble()
            }
        }
    }

    companion object {
        fun fromNodeCounts(nodeCounts : List<Int>) : Network {
            val biases = vectorOfSize(nodeCounts.size - 1)
            val values = nodeCounts.map { vectorOfSize(it) }.toTypedArray()
            val weights = biases.mapIndexed { index, _ ->
                val width = nodeCounts[index]
                val height = nodeCounts[index + 1]
                (0 until height).map { vectorOfSize(width) }.toTypedArray()
            }.toTypedArray()
            return Network(values, biases, weights)
        }

        fun fromValues(values : Matrix) : Network {
            val nodeCounts = values.map { it.size }
            val network = fromNodeCounts(nodeCounts)
            network.setValues(values)
            return network
        }
    }
}