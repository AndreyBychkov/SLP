package org.jetbrains.slp.modeling.dynamic

import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.Arrays

import org.jetbrains.slp.lexing.LexerRunner
import org.jetbrains.slp.modeling.AbstractModel
import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.mix.MixModel
import org.jetbrains.slp.modeling.ngram.NGramModel
import org.jetbrains.slp.modeling.runners.ModelRunner
import org.jetbrains.slp.translating.Vocabulary

class NestedModel @JvmOverloads constructor(
    private val global: Model,
    private val lexerRunner: LexerRunner,
    private val vocabulary: Vocabulary,
    testRoot: File,
    testBaseModel: Model? = null
) : AbstractModel() {

    private val modelRunners: MutableList<ModelRunner>
    private val files: MutableList<File>
    var mix: Model? = null
        private set

    val testRoot: File
        get() = files[0]

    constructor(baseRunner: ModelRunner, testRoot: File) : this(
        baseRunner.model,
        baseRunner.lexerRunner,
        baseRunner.vocabulary,
        testRoot
    )

    constructor(baseRunner: ModelRunner, testRoot: File, testBaseModel: Model) : this(
        baseRunner.model,
        baseRunner.lexerRunner,
        baseRunner.vocabulary,
        testRoot,
        testBaseModel
    )

    init {
        modelRunners = ArrayList()
        files = ArrayList()

        modelRunners.add(getBaseRunner(testRoot, testBaseModel))
        files.add(testRoot)
        mix = MixModel.standard(global, modelRunners[0].model)
    }

    private fun getBaseRunner(testRoot: File, testBaseModel: Model?): ModelRunner {
        var testBaseModel = testBaseModel
        val baseModelRunner: ModelRunner
        if (testBaseModel == null) {
            testBaseModel = newModel()
            baseModelRunner = newModelRunner(testBaseModel)
            baseModelRunner.learnDirectory(testRoot)
        } else {
            baseModelRunner = newModelRunner(testBaseModel)
        }
        return baseModelRunner
    }

    private fun newModel(): Model {
        try {
            return global.javaClass.getDeclaredConstructor().newInstance()
        } catch (e: InstantiationException) {
            e.printStackTrace()
            return NGramModel.standard()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
            return NGramModel.standard()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            return NGramModel.standard()
        } catch (e: SecurityException) {
            e.printStackTrace()
            return NGramModel.standard()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
            return NGramModel.standard()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
            return NGramModel.standard()
        }

    }

    private fun newModelRunner(model: Model): ModelRunner {
        return ModelRunner(model, lexerRunner, vocabulary)
    }

    /**
     * When notified of a new file, update the nesting accordingly
     */
    override fun notify(next: File) {
        updateNesting(next)
    }

    // Defer all learning/forgetting to the global model; the nested part is update dynamically
    override fun learn(input: List<Int>) {
        global.learn(input)
    }

    override fun learnToken(input: List<Int>, index: Int) {
        global.learnToken(input, index)
    }

    override fun forget(input: List<Int>) {
        global.forget(input)
    }

    override fun forgetToken(input: List<Int>, index: Int) {
        global.forgetToken(input, index)
    }

    // Answer all modeling calls with the mixed model
    override fun modelAtIndex(input: List<Int>, index: Int): Pair<Double, Double> {
        return mix!!.modelToken(input, index)
    }

    override fun predictAtIndex(input: List<Int>?, index: Int): Map<Int, Pair<Double, Double>> {
        return mix!!.predictToken(input!!, index)
    }

    private fun updateNesting(next: File) {
        val lineage = getLineage(next)
        // If lineage is empty, the current model is the (first meaningful) parent of next and is appropriate
        if (lineage == null || lineage.isEmpty()) return
        var pos = 1
        while (pos < files.size) {
            if (pos >= lineage.size || files[pos] != lineage[pos]) {
                modelRunners[pos - 1].learnDirectory(files[pos])
                files.subList(pos, files.size).clear()
                modelRunners.subList(pos, modelRunners.size).clear()
                break
            }
            pos++
        }
        for (i in pos until lineage.size) {
            val file = lineage[i]
            val model = newModel()
            files.add(file)
            modelRunners.add(newModelRunner(model))
            modelRunners[modelRunners.size - 1].learnDirectory(file)
            modelRunners[modelRunners.size - 2].forgetDirectory(file)
        }
        files.add(next)
        modelRunners[modelRunners.size - 1].forgetDirectory(next)
        mix = MixModel.standard(global, modelRunners[0].model)
        for (i in 1 until modelRunners.size) {
            mix = MixModel.standard(mix!!, modelRunners[i].model)
        }
        mix!!.notify(next)
    }

    /**
     * Returns all non-trivial directories starting from the root file to the new file.
     * Non-trivial meaning a directory containing more than one regex-matching file or dir itself;
     * building a separate nested model for such directories is pointless.
     *
     * @param file The next file to be modeled
     * @return Path containing all relevant directories from root file inclusive to `file` exclusive
     */
    private fun getLineage(file: File): List<File>? {
        var file = file
        val lineage = ArrayList<File>()
        while (file.parentFile != files[0]) {
            if (file.parentFile.list()!!.size > 1 && Arrays.stream(file.parentFile.listFiles()!!)
                    .anyMatch { f -> f.isDirectory || lexerRunner.willLexFile(f) }
            ) {
                lineage.add(file.parentFile)
            }
            file = file.parentFile
            if (file.parentFile == null) return null
        }
        lineage.add(files[0])
        lineage.reverse()
        return lineage
    }

}