package org.jetbrains.slp.modeling.mix

import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.dynamic.CacheModel
import org.jetbrains.slp.modeling.ngram.JMModel
import kotlin.math.max

class ProportionalMixModel(model1: Model = JMModel(), model2: Model = CacheModel()) : MixModel(model1, model2) {

    override fun mix(
        input: List<Int>,
        index: Int,
        res1: Pair<Double, Double>,
        res2: Pair<Double, Double>
    ): Pair<Double, Double> {
        if (res1.second == 0.0)
            return res2
        else
            if (res2.second == 0.0)
                return res1
        val confidence = max(res1.second, res2.second)
        val probability = (res1.first * res1.second + res2.first * res2.second) / (res1.second + res2.second)

        return Pair(probability, confidence)
    }

/*  IO functionality temporally excluded to get rid of jboss-marshalling dependency

    override fun load(directory: File): MixModel {
        val leftModel = left.load(getLeftDirectoryName(directory))
        val rightModel = right.load(getRightDirectoryName(directory))

        return ProportionalMixModel(leftModel, rightModel)
    }

    companion object {
        fun load(directory: File) = ProportionalMixModel().load(directory)
        fun save(directory: File, model: ProportionalMixModel) = model.save(directory)
    }

 */
}
