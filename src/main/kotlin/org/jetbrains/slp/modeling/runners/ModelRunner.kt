package org.jetbrains.slp.modeling.runners

import org.jetbrains.slp.Language
import org.jetbrains.slp.lexing.Lexer
import org.jetbrains.slp.lexing.LexerResolver
import org.jetbrains.slp.lexing.LexerRunner
import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.mix.InverseMixModel
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
open class ModelRunner(val model: Model, val lexerRunner: LexerRunner, val vocabulary: Vocabulary = Vocabulary()) {

    private var selfTesting = false

    private val LEARN_PRINT_INTERVAL: Long = 1000000
    private var learnStats = LongArray(2)

    private val MODEL_PRINT_INTERVAL = 100000
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
        return ModelRunner(model, this.lexerRunner, this.vocabulary)
    }

    /**
     * Enables self testing: if we are testing on data that we also trained on, and our models are able to forget events,
     * we can simulated training on all-but one sequence (the one we are modeling) by temporarily forgetting
     * an event, modeling it and re-learning it afterwards. This maximizes use of context information and can be used
     * to simulate full cross-validation.
     *
     * @param selfTesting If true, will temporarily "forget" every sequence before modeling it and "re-learn" it afterwards
     */
    fun setSelfTesting(selfTesting: Boolean) {
        this.selfTesting = selfTesting
    }

    fun getSelfTesting(): Boolean {
        return this.selfTesting
    }

    fun learnDirectory(file: File) {
        learnStats = longArrayOf(0, -System.currentTimeMillis())
        lexerRunner.lexDirectory(file)!!
            .forEach { p ->
                model.notify(p.first)
                learnTokens(p.second)
            }
        if (learnStats[0] > LEARN_PRINT_INTERVAL && this.learnStats[1] != 0L) {
            System.out.printf(
                "Counting complete: %d tokens processed in %ds\n",
                this.learnStats[0], (System.currentTimeMillis() + this.learnStats[1]) / 1000
            )
        }
    }

    fun learnFile(f: File) {
        if (!lexerRunner.willLexFile(f))
            return

        model.notify(f)
        learnTokens(lexerRunner.lexFile(f))
    }

    fun learnContent(content: String) {
        learnTokens(lexerRunner.lexText(content))
    }

    private fun learnTokens(lexed: Sequence<Sequence<String>>) {
        if (lexerRunner.isPerLine) {
            lexed
                .map { vocabulary.toIndices(it) }
                .map { it.onEach { logLearningProgress() } }
                .map { it.toList() }
                .forEach { model.learn(it) }
        } else {
            model.learn(lexed
                .map { it.onEach { logLearningProgress() } }
                .flatMap { vocabulary.toIndices(it) }
                .toList()
            )
        }
    }

    private fun logLearningProgress() {
        if (++learnStats[0] % LEARN_PRINT_INTERVAL == 0L && learnStats[1] != 0L) {
            System.out.printf(
                "Counting: %dM tokens processed in %ds\n",
                (learnStats[0] / 1e6).roundToInt(),
                (System.currentTimeMillis() + learnStats[1]) / 1000
            )
        }
    }

    fun forgetDirectory(file: File) {
        try {
            Files.walk(file.toPath())
                .map { it.toFile() }
                .filter{ it.isFile }
                .forEach { forgetFile(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun forgetFile(f: File) {
        if (!lexerRunner.willLexFile(f))
            return
        this.model.notify(f)
        forgetTokens(lexerRunner.lexFile(f))
    }

    fun forgetContent(content: String) {
        forgetTokens(lexerRunner.lexText(content))
    }

    private fun forgetTokens(lexed: Sequence<Sequence<String>>) {
        if (lexerRunner.isPerLine) {
            lexed.map { vocabulary.toIndices(it) }
                .map { it.toList() }
                .forEach { model.forget(it) }
        } else {
            model.forget(
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
        if (modelStats[0] / MODEL_PRINT_INTERVAL > prevCount / MODEL_PRINT_INTERVAL && modelStats[1] != 0L) {
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

        if (modelStats[0] / MODEL_PRINT_INTERVAL > prevCount / MODEL_PRINT_INTERVAL && modelStats[1] != 0L) {
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

    fun save(directory: File) {
        model.save(directory)
        VocabularyRunner.write(vocabulary, getVocabularyFile(directory))
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

        fun load(directory: File, language: Language): ModelRunner {
            return ModelRunner(
                // TODO("make it work not only for InverseMix")
                InverseMixModel.load(directory),
                LexerResolver.extensionToLexer(language.extensions.first()),
                VocabularyRunner.read(getVocabularyFile(directory))
            )
        }

        private fun getVocabularyFile(directory: File) =
            File(directory.path + File.pathSeparator + "vocabulary.tsv")
    }
}
