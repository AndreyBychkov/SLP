package org.jetbrains.slp.modeling.ngram

import org.jetbrains.slp.counting.Counter
import org.jetbrains.slp.counting.trie.MapTrieCounter
import org.jetbrains.slp.modeling.Model
import java.io.File
import kotlin.math.max


class ADModel(order: Int = 6, counter: Counter = MapTrieCounter()) : NGramModel(order, counter) {

    override fun modelWithConfidence(subList: List<Int>, counts: LongArray): Pair<Double, Double> {
        val count = counts[0]
        val contextCount = counts[1]

        // Parameters for discount weight
        val n1 = counter.getCountOfCount(subList.size, 1)
        val n2 = counter.getCountOfCount(subList.size, 2)
        val D = n1.toDouble() / (n1.toDouble() + 2 * n2)
        val distinctContext = counter.getDistinctCounts(1, subList.subList(0, subList.size - 1))
        val N1Plus = distinctContext[0]

        // Probability calculation
        val MLEDisc = max(0.0, count - D) / contextCount
        val lambda = 1 - N1Plus * D / contextCount
        // Must divide MLE by lambda to match contract
        return Pair(MLEDisc / lambda, lambda)
    }

    override val config = Config(order, "${this::class.java}")

    override fun load(directory: File): Model {
        val counter = loadCounter(directory)
        val config = loadConfig<Config>(directory)

        return ADModel(config.order, counter)
    }

    companion object {
        fun load(directory: File) = ADModel().load(directory)
        fun save(directory: File, model: ADModel) = model.save(directory)
    }
}
