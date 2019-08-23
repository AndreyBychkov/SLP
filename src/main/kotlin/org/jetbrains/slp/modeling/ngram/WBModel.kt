package org.jetbrains.slp.modeling.ngram

import org.jetbrains.slp.counting.Counter
import org.jetbrains.slp.counting.trie.MapTrieCounter

class WBModel(order: Int = 6, counter: Counter = MapTrieCounter()) : NGramModel(order, counter) {

    override fun modelWithConfidence(subList: List<Int>, counts: LongArray): Pair<Double, Double> {
        val count = counts[0]
        val contextCount = counts[1]

        // Parameters for discount weight
        val distinctContext = counter.getDistinctCounts(1, subList.subList(0, subList.size - 1))
        val N1Plus = distinctContext[0]

        // Probability calculation
        val MLE = count / contextCount.toDouble()
        val lambda = contextCount.toDouble() / (N1Plus.toDouble() + contextCount)
        return Pair(MLE, lambda)
    }

    override val config = Config(order, "${this::class.java}")

/*  IO functionality temporally excluded to get rid of jboss-marshalling dependency

    companion object {
        fun load(directory: File) = WBModel().load(directory)
        fun save(directory: File, model: WBModel) = model.save(directory)
    }

 */
}
