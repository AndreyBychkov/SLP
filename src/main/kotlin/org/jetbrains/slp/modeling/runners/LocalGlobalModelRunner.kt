package org.jetbrains.slp.modeling.runners

import org.jetbrains.slp.Language
import org.jetbrains.slp.counting.giga.GigaCounter
import org.jetbrains.slp.counting.trie.ArrayTrieCounter
import org.jetbrains.slp.lexing.LexerRunnerFactory
import org.jetbrains.slp.lexing.LexerRunner
import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.modeling.mix.MixModel
import org.jetbrains.slp.modeling.ngram.JMModel
import org.jetbrains.slp.modeling.ngram.NGramModel
import org.jetbrains.slp.translating.Vocabulary
import org.jetbrains.slp.translating.VocabularyRunner
import java.io.File

class LocalGlobalModelRunner(localModel: Model = getDefaultLocalModel(),
                             globalModel: Model = getDefaultGlobalModel(),
                             lexerRunner: LexerRunner,
                             vocabulary: Vocabulary = Vocabulary()
) :
    ModelRunner(MixModel.standard(localModel, globalModel), lexerRunner, vocabulary) {

    private val localModel = (model as MixModel).left
    private val globalModel = (model as MixModel).right

    fun trainLocal(file: File) {
        train(file, localModel)
    }

    fun trainLocal(text: String) {
        learnContent(text, localModel)
    }

    fun trainGlobal(file: File) {
        train(file, globalModel)
    }

    fun forgetLocal(file: File) {
        forget(file, localModel)
    }

    fun forgetGlobal(file: File) {
        forget(file, globalModel)
    }

    fun clearLocal() {
        try {
            (localModel as NGramModel).clearCounter()
        } catch (e: ClassCastException) {
            println("Can not cast local model to NgramModel. Make sure that local model is Ngram based")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun save(directory: File) {
        globalModel.save(directory)
        VocabularyRunner.write(vocabulary, directory)
    }

    companion object {
        fun load(directory: File, language: Language): LocalGlobalModelRunner {
            return LocalGlobalModelRunner(
                getDefaultLocalModel(),
                JMModel.load(directory),
                LexerRunnerFactory.getLexerRunner(language),
                VocabularyRunner.read(directory)
            )
        }

        fun load(directory: File, extension: String): LocalGlobalModelRunner {
            return LocalGlobalModelRunner(
                getDefaultLocalModel(),
                JMModel.load(directory),
                LexerRunnerFactory.getLexerRunner(extension),
                VocabularyRunner.read(directory)
            )
        }

        private fun getDefaultLocalModel(): Model {
            return JMModel(10)
        }

        private fun getDefaultGlobalModel(): Model {
            return JMModel(6, counter = GigaCounter())
        }
    }
}