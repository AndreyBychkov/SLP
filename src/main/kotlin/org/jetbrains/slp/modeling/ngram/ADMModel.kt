package org.jetbrains.slp.modeling.ngram

import org.jetbrains.slp.counting.Counter
import org.jetbrains.slp.counting.trie.MapTrieCounter
import kotlin.math.max
import kotlin.math.min

class ADMModel(order: Int = 6, counter: Counter = MapTrieCounter()) : NGramModel(order, counter) {
    
    override fun modelWithConfidence(`in`: List<Int>, counts: LongArray): Pair<Double, Double> {
        val count = counts[0]
        val contextCount = counts[1]

        // Parameters for discount weight
        val n1 = counter.getCountOfCount(`in`.size, 1)
        val n2 = counter.getCountOfCount(`in`.size, 2)
        val n3 = counter.getCountOfCount(`in`.size, 3)
        val n4 = counter.getCountOfCount(`in`.size, 4)
        val Y = n1.toDouble() / (n1.toDouble() + 2 * n2)
        val Ds = doubleArrayOf(Y, 2 - 3.0 * Y * n3.toDouble() / n2, 3 - 4.0 * Y * n4.toDouble() / n3)
        // Smooth out extreme (possibly non-finite) discount factors (in case of few observations)
        for (i in Ds.indices) {
            if (java.lang.Double.isNaN(Ds[i]) || Ds[i] < 0.25 * (i + 1) || Ds[i] > i + 1) Ds[i] = 0.6 * (i + 1)
        }
        val Ns = counter.getDistinctCounts(3, `in`.subList(0, `in`.size - 1))

        // Probability calculation
        val discount = if (count > 0) Ds[(min(count, Ds.size.toLong()) - 1).toInt()] else 0.0
        val MLEDisc = max(0.0, count - discount) / contextCount
        val lambda = 1 - (Ds[0] * Ns[0] + Ds[1] * Ns[1] + Ds[2] * Ns[2]) / contextCount
        // Must divide MLE by lambda to match contract
        return Pair(MLEDisc / lambda, lambda)
    }

    override val config = Config(order, "${this::class.java}")


/*  IO functionality temporally excluded to get rid of jboss-marshalling dependency

    companion object {
        fun load(directory: File) = ADMModel().load(directory)
        fun save(directory: File, model: ADMModel) = model.save(directory)
    }

 */
}
