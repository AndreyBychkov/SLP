package org.jetbrains.slp.modeling.dynamic

import org.jetbrains.slp.modeling.AbstractModel
import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.ngram.NGramModel
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.*

class CacheModel(private var model: Model = NGramModel.standard(), private val capacity: Int = DEFAULT_CAPACITY) :
  AbstractModel() {
    private val cache: Deque<Pair<List<Int>, Int>>
    private val cachedRefs: MutableMap<Int, List<Int>>

    init {
        // A cache is dynamic by default and only acts statically in prediction tasks
        dynamic = true
        cache = ArrayDeque(capacity)
        cachedRefs = HashMap()
    }

    override fun notify(next: File) {
        try {
            model = model.javaClass.getConstructor().newInstance()
        } catch (e: InstantiationException) {
            model = NGramModel.standard()
        } catch (e: IllegalAccessException) {
            model = NGramModel.standard()
        } catch (e: IllegalArgumentException) {
            model = NGramModel.standard()
        } catch (e: InvocationTargetException) {
            model = NGramModel.standard()
        } catch (e: NoSuchMethodException) {
            model = NGramModel.standard()
        } catch (e: SecurityException) {
            model = NGramModel.standard()
        }

        cache.clear()
        cachedRefs.clear()
    }

    // The cache model cannot be taught new events, it only learns after modeling
    override fun learn(input: List<Int>) {}

    override fun learnToken(input: List<Int>, index: Int) {}
    override fun forget(input: List<Int>) {}
    override fun forgetToken(input: List<Int>, index: Int) {}

    override fun modelAtIndex(input: List<Int>, index: Int): Pair<Double, Double> {
        val modeled = model.modelToken(input, index)
        updateCache(input, index)
        return modeled
    }

    private fun updateCache(input: List<Int>, index: Int) {
        if (capacity > 0 && dynamic) {
            store(input, index)
            model.learnToken(input, index)
            if (cache.size > capacity) {
                val removed = cache.removeFirst()
                model.forgetToken(removed.first, removed.second)
            }
        }
    }

    private fun store(input: List<Int>, index: Int) {
        val hash = input.hashCode()
        var list: List<Int>? = cachedRefs[hash]
        if (list == null) {
            list = ArrayList(input)
            cachedRefs[hash] = list
        }
        cache.addLast(Pair(list, index))
    }

    override fun predictAtIndex(input: List<Int>?, index: Int): Map<Int, Pair<Double, Double>> {
        return model.predictToken(input!!, index)
    }


    override fun toString(): String {
        return javaClass.simpleName
    }


    companion object {

        const val DEFAULT_CAPACITY = 5000
    }
}
