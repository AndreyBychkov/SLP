package org.jetbrains.slp.modeling.mix

import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.dynamic.CacheModel
import org.jetbrains.slp.modeling.ngram.JMModel
import kotlin.math.max

class InverseMixModel
/**
 * Assumes the two models report their confidence as asymptotically close to 1.
 * The way to mix probabilities is then to compare the inverses of the confidences
 * and use these to weight the respective models' guesses.
 */
    (model1: Model = JMModel(), model2: Model = CacheModel()) : MixModel(model1, model2) {

    override fun mix(
        input: List<Int>,
        index: Int,
        res1: Pair<Double, Double>,
        res2: Pair<Double, Double>
    ): Pair<Double, Double> {
        if (res1.second == 0.0 && res2.second == 0.0)
            return Pair(0.0, 0.0)
        else if (res2.second == 0.0)
            return res1
        else if (res1.second == 0.0)
            return res2

        val lNorm = if (res1.second > 0.999) 1000.0 else 1.0 / (1 - res1.second)
        val rNorm = if (res2.second > 0.999) 1000.0 else 1.0 / (1 - res2.second)

        val probability = (res1.first * lNorm + res2.first * rNorm) / (lNorm + rNorm)
        val confidence = max(res1.second, res2.second)

        return Pair(probability, confidence)
    }
/*  IO functionality temporally excluded to get rid of jboss-marshalling dependency

    override fun load(directory: File): MixModel {
        val leftModel = left.load(getLeftDirectoryName(directory))
        val rightModel = right.load(getRightDirectoryName(directory))

        return InverseMixModel(leftModel, rightModel)
    }

    companion object {
        fun load(directory: File) = InverseMixModel().load(directory)
        fun save(directory: File, model: InverseMixModel) = model.save(directory)
    }

 */
}
