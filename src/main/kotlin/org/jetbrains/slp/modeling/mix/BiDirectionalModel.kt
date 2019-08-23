package org.jetbrains.slp.modeling.mix

import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.dynamic.CacheModel
import org.jetbrains.slp.modeling.ngram.JMModel
import java.io.File
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.max

/**
 * @author Vincent Hellendoorn
 */
class BiDirectionalModel : MixModel {

    val forward: Model
        get() = left

    val reverse: Model
        get() = right

    /**
     * Use only for stateless models!
     */
    constructor(model: Model = JMModel()) : super(model, model)

    constructor(model1: Model = JMModel(), model2: Model = CacheModel()) : super(model1, model2)

    private fun getReverse(input: List<Int>): List<Int> {
        return IntStream.range(0, input.size)
            .mapToObj { i -> input[input.size - i - 1] }
            .collect(Collectors.toList())
    }

    override fun notifyRight(next: File) {
        this.right.notify(next)
    }

    override fun learnRight(input: List<Int>) {
        var input = input
        input = getReverse(input)
        right.learn(input)
    }

    override fun learnRight(input: List<Int>, index: Int) {
        var input = input
        input = getReverse(input)
        right.learnToken(input, input.size - index - 1)
    }

    override fun forgetRight(input: List<Int>) {
        var input = input
        input = getReverse(input)
        right.forget(input)
    }

    override fun forgetRight(input: List<Int>, index: Int) {
        var input = input
        input = getReverse(input)
        right.forgetToken(input, input.size - index - 1)
    }

    override fun modelRight(input: List<Int>): List<Pair<Double, Double>> {
        var input = input
        input = getReverse(input)
        val modeled = right.model(input)
        return modeled.reversed()
    }

    override fun modelRight(input: List<Int>, index: Int): Pair<Double, Double> {
        var input = input
        input = getReverse(input)
        return right.modelToken(input, input.size - index - 1)
    }

    override fun predictRight(input: List<Int>): List<Map<Int, Pair<Double, Double>>> {
        var input = input
        input = getReverse(input)
        val predictions = right.predict(input)
        return predictions.reversed()
    }

    override fun predictRight(input: List<Int>, index: Int): Map<Int, Pair<Double, Double>> {
        var input = input
        input = getReverse(input)
        return right.predictToken(input, input.size - index - 1)
    }

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

        var lNorm = 1 / (1 - res1.second)
        var rNorm = 1 / (1 - res2.second)

        if (lNorm > 1000)
            lNorm = 1000.0
        if (rNorm > 1000)
            rNorm = 1000.0

        val probability = (res1.first * lNorm + res2.first * rNorm) / (lNorm + rNorm)
        val confidence = max(res1.second, res2.second)

        return Pair(probability, confidence)
    }
/*  IO functionality temporally excluded to get rid of jboss-marshalling dependency

    override fun load(directory: File): MixModel {
        val leftModel = left.load(getLeftDirectoryName(directory))
        val rightModel = right.load(getRightDirectoryName(directory))

        return BiDirectionalModel(leftModel, rightModel)
    }

    companion object {
        fun load(directory: File) = BiDirectionalModel().load(directory)
        fun save(directory: File, model: BiDirectionalModel) = model.save(directory)
    }
 */
}
