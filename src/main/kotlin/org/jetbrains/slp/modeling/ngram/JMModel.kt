package org.jetbrains.slp.modeling.ngram

import org.jetbrains.slp.counting.Counter
import org.jetbrains.slp.counting.trie.MapTrieCounter
import java.io.File

class JMModel(
    order: Int = 6,
    private val lambda: Double = DEFAULT_LAMBDA,
    counter: Counter = MapTrieCounter()
)
    : NGramModel(order, counter) {


    override fun modelWithConfidence(`in`: List<Int>, counts: LongArray): Pair<Double, Double> {
        val count = counts[0]
        val contextCount = counts[1]

        // Probability calculation
        val MLE = count / contextCount.toDouble()
        return Pair(MLE, lambda)
    }

    class JMMConfig(order: Int, name: String, val lambda: Double): Config(order, name)

    override val config = JMMConfig(order, this::class.java.toString(), lambda)

    override fun load(directory: File): JMModel {
        val counter = loadCounter(directory)
        val config = loadConfig<JMMConfig>(directory)

        return JMModel(config.order, config.lambda, counter)
    }

    companion object {
        private val DEFAULT_LAMBDA = 0.5

        fun load(directory: File) = JMModel().load(directory)
        fun save(directory: File, model: JMModel) = model.save(directory)

    }
}
