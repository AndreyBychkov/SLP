package org.jetbrains.slp.modeling.runners

import org.jetbrains.slp.counting.giga.GigaCounter
import org.jetbrains.slp.lexing.Lexer
import org.jetbrains.slp.lexing.LexerRunner
import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.dynamic.CacheModel
import org.jetbrains.slp.modeling.mix.MixModel
import org.jetbrains.slp.modeling.ngram.JMModel
import org.jetbrains.slp.modeling.ngram.NGramModel
import org.jetbrains.slp.translating.Vocabulary
import org.jetbrains.slp.translating.VocabularyRunner
import java.io.File
import java.io.IOException
import java.io.Reader
import java.nio.file.Files
import java.util.*
import java.util.stream.Collectors
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.streams.asStream

/**
 * This class can be used to run [Model]-related functions over bodies of code.
 * It provides the lexing and translation steps necessary to allow immediate learning and modeling from directories or files.
 * As such, it wraps the pipeline stages [Reader] --> [Lexer] --> Translate ([Vocabulary]) --> [Model].
 * <br></br>
 * This class uses a [LexerRunner], which differentiates between file and line data and provides a some additional utilities.
 * It also provides easier access to self-testing (in which each line is forgotten before modeling it and re-learned after),
 * which is helpful for count-based models such as [NGramModel]s.
 *
 * @author Vincent Hellendoorn
 */
open class ModelRunner(val model: Model = getDefaultModel(), val lexerRunner: LexerRunner, val vocabulary: Vocabulary = Vocabulary()) {

    var selfTesting = false

    private val learnPrintInterval: Long = 1000000
    private var learnStats = LongArray(2)

    private val modelPrintInterval = 100000
    private var modelStats = LongArray(2)
    private var ent = 0.0
    private var mrr = 0.0

    /**
     * Convenience function that creates a new [ModelRunner] instance for the provided [Model]
     * that is backed by the same [LexerRunner] and [Vocabulary].
     *
     * @param model The model to provide a [ModelRunner] for.
     * @return A new [ModelRunner] for this [Model],
     * with the current [ModelRunner]'s [LexerRunner] and [Vocabulary]
     */
    fun copyForModel(model: Model): ModelRunner {
        return ModelRunner(model, lexerRunner, vocabulary)
    }

    fun train(file: File, selectedModel: Model = model) {
        when {
            file.isDirectory -> learnDirectory(file, selectedModel)
            file.isFile -> learnFile(file, selectedModel)
            else -> throw IllegalArgumentException("Argument must be directory of file")
        }
    }

    fun train(text: String, selectedModel: Model = model) {
        learnContent(text, selectedModel)
    }

    protected fun learnDirectory(file: File, selectedModel: Model = model) {
        learnStats = longArrayOf(0, -System.currentTimeMillis())
        lexerRunner.lexDirectory(file)!!
            .forEach { p ->
                selectedModel.notify(p.first)
                learnTokens(p.second, selectedModel)
            }
        if (learnStats[0] > learnPrintInterval && learnStats[1] != 0L) {
            System.out.printf(
                "Counting complete: %d tokens processed in %ds\n",
                this.learnStats[0], (System.currentTimeMillis() + this.learnStats[1]) / 1000
            )
        }
    }

    protected fun learnFile(f: File, selectedModel: Model = model) {
        if (!lexerRunner.willLexFile(f))
            return

        selectedModel.notify(f)
        learnTokens(lexerRunner.lexFile(f), selectedModel)
    }

    protected fun learnContent(content: String, selectedModel: Model = model) {
        learnTokens(lexerRunner.lexText(content), selectedModel)
    }

    protected fun learnTokens(lexed: Sequence<Sequence<String>>, selectedModel: Model = model) {
        if (lexerRunner.isPerLine) {
            lexed
                .map { vocabulary.toIndices(it) }
                .map { it.onEach { logLearningProgress() } }
                .map { it.toList() }
                .forEach { selectedModel.learn(it) }
        } else {
            selectedModel.learn(lexed
                .map { it.onEach { logLearningProgress() } }
                .flatMap { vocabulary.toIndices(it) }
                .toList()
            )
        }
    }

    private fun logLearningProgress() {
        if (++learnStats[0] % learnPrintInterval == 0L && learnStats[1] != 0L) {
            System.out.printf(
                "Counting: %dM tokens processed in %ds\n",
                (learnStats[0] / 1e6).roundToInt(),
                (System.currentTimeMillis() + learnStats[1]) / 1000
            )
        }
    }

    fun forget(file: File, selectedModel: Model) {
        when {
            file.isDirectory -> forgetDirectory(file, selectedModel)
            file.isFile -> forgetFile(file, selectedModel)
            else -> throw IllegalArgumentException("Argument must be directory of file")
        }
    }

    protected fun forgetDirectory(file: File, selectedModel: Model = model) {
        try {
            Files.walk(file.toPath())
                .map { it.toFile() }
                .filter{ it.isFile }
                .forEach { forgetFile(it, selectedModel) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    protected fun forgetFile(f: File, selectedModel: Model = model) {
        if (!lexerRunner.willLexFile(f))
            return
        selectedModel.notify(f)
        forgetTokens(lexerRunner.lexFile(f), selectedModel)
    }

    fun forgetContent(content: String, selectedModel: Model) {
        forgetTokens(lexerRunner.lexText(content), selectedModel)
    }

    private fun forgetTokens(lexed: Sequence<Sequence<String>>, selectedModel: Model = model) {
        if (lexerRunner.isPerLine) {
            lexed.map { vocabulary.toIndices(it) }
                .map { it.toList() }
                .forEach { selectedModel.forget(it) }
        } else {
            selectedModel.forget(
                lexed
                    .flatMap { vocabulary.toIndices(it) }
                    .toList()
            )
        }
    }

    fun modelDirectory(file: File): Sequence<Pair<File, List<List<Double>>>> {
        this.modelStats = longArrayOf(0, -System.currentTimeMillis())
        this.ent = 0.0
        return this.lexerRunner.lexDirectory(file)!!
            .map { p ->
                this.model.notify(p.first)
                Pair(p.first, modelTokens(p.second))
            }
    }

    fun modelFile(f: File): List<List<Double>>? {
        if (!this.lexerRunner.willLexFile(f)) return null
        this.model.notify(f)
        return modelTokens(this.lexerRunner.lexFile(f))
    }

    fun modelContent(content: String): List<List<Double>> {
        return modelTokens(this.lexerRunner.lexText(content))
    }

    private fun modelTokens(lexed: Sequence<Sequence<String>>): List<List<Double>> {
        this.vocabulary.setCheckpoint()
        val lineProbs: List<List<Double>>

        if (this.lexerRunner.isPerLine) {
            lineProbs = lexed
                .map { vocabulary.toIndices(it) }
                .map { it.toList() }
                .map { modelSequence(it) }
                .onEach { logModelingProgress(it) }
                .toList()
        } else {
            val lineLengths = ArrayList<Int>()
            val modeled = modelSequence(
                lexed
                    .map { vocabulary.toIndices(it) }
                    .map { it.toList() }
                    .onEach { lineLengths.add(it.size) }
                    .flatMap { it.asSequence() }
                    .toList()
            )
            lineProbs = toLines(modeled, lineLengths)
            logModelingProgress(modeled)
        }
        this.vocabulary.restoreCheckpoint()
        return lineProbs
    }

    protected fun modelSequence(tokens: List<Int>): List<Double> {
        if (this.selfTesting) this.model.forget(tokens)
        val entropies = this.model.model(tokens).stream()
            .map { toProb(it) }
            .map { toEntropy(it) }
            .collect(Collectors.toList())
        if (selfTesting)
            model.learn(tokens)
        return entropies
    }

    private fun logModelingProgress(modeled: List<Double>) {
        val stats = modeled.stream().skip(1)
            .mapToDouble { it.toDouble() }.summaryStatistics()
        val prevCount = modelStats[0]
        modelStats[0] += stats.count
        ent += stats.sum
        if (modelStats[0] / modelPrintInterval > prevCount / modelPrintInterval && modelStats[1] != 0L) {
            System.out.printf(
                "Modeling: %dK tokens processed in %ds, avg. entropy: %.4f\n",
                (modelStats[0] / 1e3).roundToInt(),
                (System.currentTimeMillis() + modelStats[1]) / 1000, ent / modelStats[0]
            )
        }
    }

    fun predict(file: File): Sequence<Pair<File, List<List<Double>>>> {
        modelStats = longArrayOf(0, -System.currentTimeMillis())
        mrr = 0.0
        return lexerRunner.lexDirectory(file)!!
            .map { p ->
                model.notify(p.first)
                Pair(p.first, predictTokens(p.second))
            }
    }

    fun predictFile(f: File): List<List<Double>>? {
        if (!lexerRunner.willLexFile(f))
            return null
        this.model.notify(f)
        return predictTokens(lexerRunner.lexFile(f))
    }

    fun predictContent(content: String): List<List<Double>> {
        return predictTokens(lexerRunner.lexText(content))
    }

    private fun predictTokens(lexed: Sequence<Sequence<String>>): List<List<Double>> {
        vocabulary.setCheckpoint()
        val lineProbs: List<List<Double>>

        if (lexerRunner.isPerLine) {
            lineProbs = lexed
                .map { vocabulary.toIndices(it) }
                .map { it.toList() }
                .map { predictSequence(it) }
                .onEach { logPredictionProgress(it) }
                .toList()
        } else {
            val lineLengths = ArrayList<Int>()
            val modeled = predictSequence(lexed
                .map { vocabulary.toIndices(it) }
                .map { it.toList() }
                .onEach { lineLengths.add(it.size) }
                .flatMap { it.asSequence() }
                .toList()
            )
            lineProbs = toLines(modeled, lineLengths)
            logPredictionProgress(modeled)
        }
        vocabulary.restoreCheckpoint()
        return lineProbs
    }

    private fun predictSequence(tokens: List<Int>): List<Double> {
        if (selfTesting)
            model.forget(tokens)
        val preds = toPredictions(model.predict(tokens))

        val mrrs = (0 until tokens.size)
            .map { preds[it].indexOf(tokens[it]) }
            .map { toMRR(it) }

        if (selfTesting)
            model.learn(tokens)
        return mrrs
    }

    private fun logPredictionProgress(modeled: List<Double>) {
        val stats = modeled
            .stream()
            .skip(1)
            .mapToDouble { it.toDouble() }
            .summaryStatistics()

        val prevCount = modelStats[0]
        modelStats[0] += stats.count
        mrr += stats.sum

        if (modelStats[0] / modelPrintInterval > prevCount / modelPrintInterval && modelStats[1] != 0L) {
            System.out.printf(
                "Predicting: %dK tokens processed in %ds, avg. MRR: %.4f\n",
                (modelStats[0] / 1e3).roundToInt(),
                (System.currentTimeMillis() + modelStats[1]) / 1000, mrr / modelStats[0]
            )
        }
    }

    fun toProb(probConfs: List<Pair<Double, Double>>): List<Double> {
        return probConfs.map { toProb(it) }
    }

    private fun toProb(probConf: Pair<Double, Double>): Double {
        val prob = probConf.first
        val conf = probConf.second
        return prob * conf + (1 - conf) / vocabulary.size()
    }

    private fun toPredictions(probConfs: List<Map<Int, Pair<Double, Double>>>): List<List<Int>> {
        return probConfs.map { toPredictions(it) }

    }

    private fun toPredictions(probConf: Map<Int, Pair<Double, Double>>): List<Int> {
        return probConf
            .map { Pair(it.key, toProb(it.value)) }
            .sortedByDescending { it.second }
            .take(predictionCutoff)
            .map { it.first }
    }

    private fun <K> toLines(modeled: List<K>, lineLengths: List<Int>): List<List<K>> {
        val perLine = ArrayList<List<K>>()
        var ix = 0
        for (i in lineLengths.indices) {
            val line = ArrayList<K>()
            for (j in 0 until lineLengths[i]) {
                line.add(modeled[ix++])
            }
            perLine.add(line)
        }
        return perLine
    }

    fun getStats(fileProbs: Map<File, List<List<Double>>>): DoubleSummaryStatistics {
        return getStats(fileProbs.map { Pair(it.key, it.value) }.asSequence())
    }

    fun getStats(fileProbs: Sequence<Pair<File, List<List<Double>>>>): DoubleSummaryStatistics {
        return getFileStats(fileProbs.map { p -> p.second })
    }

    fun getStats(fileProbs: List<List<Double>>): DoubleSummaryStatistics {
        return getFileStats(sequenceOf(fileProbs))
    }

    private fun getFileStats(fileProbs: Sequence<List<List<Double>>>): DoubleSummaryStatistics {
        return if (lexerRunner.isPerLine) {
            fileProbs
                .flatMap { it.asSequence() }
                .flatMap { it.asSequence().drop(1) }
                .asStream()
                .mapToDouble { it }
                .summaryStatistics()
        } else {
            fileProbs
                .flatMap { f ->
                    f.asSequence()
                    .flatMap { it.asSequence() }
                    .drop(1)
                }
                .asStream()
                .mapToDouble { it }
                .summaryStatistics()
        }
    }

    fun getOrder() : Int {
        if (model is NGramModel) return model.order
        return -1
    }

    open fun save(directory: File) {
        model.save(directory)
        VocabularyRunner.write(vocabulary, directory)
    }

    companion object {

        private val INV_NEG_LOG_2 = -1.0 / ln(2.0)
        const val DEFAULT_NGRAM_ORDER = 6

        var predictionCutoff = 10

        fun toEntropy(probability: Double): Double {
            return ln(probability) * INV_NEG_LOG_2
        }

        fun toMRR(ix: Int): Double {
            return if (ix >= 0) 1.0 / (ix + 1) else 0.0
        }

        private fun getDefaultModel(): Model {
            var model: Model = JMModel(counter = GigaCounter())
            model = MixModel.standard(model, CacheModel())
            return model
        }
    }
}
