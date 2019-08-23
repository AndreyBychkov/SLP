package org.jetbrains.slp.modeling

/**
 * Implementation of [slp.core.modeling.Model] interface that serves as base class for most models.<br></br>
 * This class extends [Model] by imposing per-token control and maintenance
 *
 * @see {@link MixModel}, {@link NGramModel}
 *
 *
 * @author Vincent Hellendoorn
 */
abstract class AbstractModel : Model {

    override var dynamic = false
        set(value) {
            field = value
            wasDynamic = value
        }

    private var wasDynamic = false
    private var dynamicDepth = 0

    override fun pauseDynamic() {
        this.dynamicDepth++
        this.dynamic = false
    }

    override fun unPauseDynamic() {
        if (this.wasDynamic && this.dynamicDepth > 0 && --this.dynamicDepth == 0) {
            this.dynamic = true
        }
    }

    override fun getConfidence(input: List<Int>, index: Int): Double {
        pauseDynamic()

        val confidence = predictAtIndex(input, index)
            .map { it.value.first }
            .sortedByDescending { it }
            .take(1)
            .sum()

        unPauseDynamic()
        return confidence
    }

    /**
     * Default implementation of [.modelToken],
     * which invokes [.modelAtIndex] at each index and takes care of dynamic updating after each token.
     */
    override fun modelToken(input: List<Int>, index: Int): Pair<Double, Double> {
        return modelAtIndex(input, index)
            .also {
                if (dynamic)
                    learnToken(input, index)
            }
    }

    abstract fun modelAtIndex(input: List<Int>, index: Int): Pair<Double, Double>

    /**
     * Default implementation of [.predictToken],
     * which invokes [.predictAtIndex] at each index and takes care of dynamic updating for each token.
     */
    override fun predictToken(input: List<Int>, index: Int): Map<Int, Pair<Double, Double>> {
        val temp = dynamic
        dynamic = false

        val predictions = predictAtIndex(input, index)

        dynamic = temp
        if (dynamic)
            learnToken(input, index)

        return predictions
    }

    abstract fun predictAtIndex(input: List<Int>?, index: Int): Map<Int, Pair<Double, Double>>

}
