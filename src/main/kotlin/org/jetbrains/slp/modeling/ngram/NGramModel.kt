package org.jetbrains.slp.modeling.ngram


import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import org.jetbrains.slp.counting.Counter
import org.jetbrains.slp.counting.io.CounterIO
import org.jetbrains.slp.counting.trie.MapTrieCounter
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.HashSet
import java.util.stream.Collectors

import org.jetbrains.slp.modeling.AbstractModel
import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.runners.ModelRunner
import org.jetbrains.slp.sequencing.NGramSequencer
import java.io.FileReader
import java.io.FileWriter
import kotlin.math.pow


abstract class NGramModel @JvmOverloads constructor(
    val order: Int = ModelRunner.DEFAULT_NGRAM_ORDER,
    var counter: Counter = MapTrieCounter()
) : AbstractModel() {

    constructor(counter: Counter) : this(ModelRunner.DEFAULT_NGRAM_ORDER, counter)

    override fun notify(next: File) {}

    override fun learn(input: List<Int>) {
        counter.countBatch(NGramSequencer.sequenceForward(input, order))
    }

    override fun learnToken(input: List<Int>, index: Int) {
        val sequence = NGramSequencer.sequenceAt(input, index, order)
        for (i in sequence.indices) {
            counter.count(sequence.subList(i, sequence.size))
        }
    }

    override fun forget(input: List<Int>) {
        counter.unCountBatch(NGramSequencer.sequenceForward(input, order))
    }

    override fun forgetToken(input: List<Int>, index: Int) {
        val sequence = NGramSequencer.sequenceAt(input, index, order)
        for (i in sequence.indices) {
            counter.unCount(sequence.subList(i, sequence.size))
        }
    }

    override fun modelAtIndex(input: List<Int>, index: Int): Pair<Double, Double> {
        val sequence = NGramSequencer.sequenceAt(input, index, order)
        var probability = 0.0
        var mass = 0.0
        var hits = 0
        for (i in sequence.indices.reversed()) {
            val sub = sequence.subList(i, sequence.size)
            val counts = counter.getCounts(sub)
            if (counts[1] == 0L) break
            val resN = modelWithConfidence(sub, counts)
            val prob = resN.first
            val conf = resN.second
            mass = (1 - conf) * mass + conf
            probability = (1 - conf) * probability + conf * prob
            hits++
        }
        if (mass > 0) probability /= mass
        // In the new model, final confidence is asymptotically close to 1 for all n-gram models
        val confidence = 1 - 2.0.pow((-hits).toDouble())
        return Pair(probability, confidence)
    }

    protected abstract fun modelWithConfidence(subList: List<Int>, counts: LongArray): Pair<Double, Double>

    override fun predictAtIndex(input: List<Int>?, index: Int): Map<Int, Pair<Double, Double>> {
        val sequence = NGramSequencer.sequenceAt(input!!, index - 1, order)
        val predictions = HashSet<Int>()

        for (i in 0..sequence.size) {
            val limit = ModelRunner.predictionCutoff - predictions.size
            if (limit <= 0) break
            predictions.addAll(counter.getTopSuccessors(sequence.subList(i, sequence.size), limit))
        }
        return predictions.stream().collect(
            Collectors.toMap({ p -> p },
                { p -> prob(input.toMutableList(), index, p!!) })
        )
    }

    private fun prob(input: MutableList<Int>, index: Int, prediction: Int): Pair<Double, Double> {
        val added = index == input.size
        if (added)
            input.add(0)
        val prev = input.set(index, prediction)
        val prob = modelAtIndex(input, index)
        if (added)
            input.removeAt(input.size - 1)
        else
            input[index] = prev
        return prob
    }

    override fun toString(): String {
        return javaClass.simpleName
    }

    open class Config(val order: Int, val name: String)

    protected open val config: Config =
        Config(order, this::class.java.toString())

    override fun save(directory: File) {
        saveCounter(directory)
        saveConfig(directory)
    }

    private fun saveCounter(directory: File) {
        val counterFile = buildCounterFileName(directory)

        CounterIO.writeCounter(counter, counterFile)
    }

    private fun saveConfig(directory: File) {
        val configFile = buildConfigFileName(directory)
        val writer = FileWriter(configFile)

        Gson().toJson(config, writer)

        writer.flush()
        writer.close()
    }

    override fun load(directory: File): Model {
        val counter = loadCounter(directory)
        val config = loadConfig<Config>(directory)

        return Class
            .forName(config.name)
            .getConstructor(Int::class.java, Counter::class.java)
            .newInstance(config.order, counter) as Model
    }

    protected fun loadCounter(directory: File): Counter {
        val counterFile = buildCounterFileName(directory)
        return CounterIO.readCounter(counterFile) ?: counter
    }


    protected open fun <ConfigT> loadConfig(directory: File): ConfigT {
        val configFile = buildConfigFileName(directory)
        val config = Gson().fromJson<ConfigT>(JsonReader(FileReader(configFile)), config::class.java)

        return config.also { if (config == null) println("Load failed") }
    }

    protected open fun buildCounterFileName(directory: File): File {
        return File("${directory.absolutePath}${File.separator}counter.obj")
    }

    protected open fun buildConfigFileName(directory: File): File {
        return File("${directory.absolutePath}${File.separator}config.json")
    }

    companion object {

        private var standard: Class<out NGramModel> = JMModel::class.java
        fun setStandard(clazz: Class<out NGramModel>) {
            standard = clazz
        }

        fun standard(): NGramModel {
            try {
                return standard.getDeclaredConstructor().newInstance()
            } catch (e: InstantiationException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: SecurityException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                return JMModel()
            }

        }

        fun standard(counter: Counter): NGramModel {
            try {
                return standard.getConstructor(Counter::class.java).newInstance(counter)
            } catch (e: InstantiationException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: SecurityException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                return JMModel()
            } catch (e: NoSuchMethodException) {
                e.printStackTrace()
                return JMModel()
            }

        }
    }
}
