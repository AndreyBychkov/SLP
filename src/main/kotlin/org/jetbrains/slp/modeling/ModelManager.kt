package org.jetbrains.slp.modeling

import org.jetbrains.slp.Language
import org.jetbrains.slp.counting.giga.GigaCounter
import org.jetbrains.slp.counting.trie.MapTrieCounter
import org.jetbrains.slp.lexing.LexerResolver
import org.jetbrains.slp.modeling.dynamic.CacheModel
import org.jetbrains.slp.modeling.mix.MixModel
import org.jetbrains.slp.modeling.ngram.JMModel
import org.jetbrains.slp.modeling.runners.ModelRunner

object ModelManager {
    private val modelsHolder = mutableMapOf<String, ModelRunner>()

    fun getModelForExtension(extension: String): ModelRunner {
        if (extension !in modelsHolder.keys)
            registerModel(extension)

        return modelsHolder[extension]!!
    }

    fun getModelForLanguage(language: Language): ModelRunner {
        if (language.extensions.none { it in modelsHolder.keys })
            language.extensions.forEach { registerModel(it) }

        return modelsHolder[language.extensions.first()]!!

    }

    private fun registerModel(extension: String) {
        val model = ModelRunner(configDefaultModel(), LexerResolver.extensionToLexer(extension))
        modelsHolder[extension] = model
    }

    private fun configDefaultModel(): Model {
        var model: Model = JMModel(10, counter = MapTrieCounter())
        model = MixModel.standard(model, CacheModel())
        return model
    }
}