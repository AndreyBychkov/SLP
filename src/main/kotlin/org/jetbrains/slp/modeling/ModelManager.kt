package org.jetbrains.slp.modeling

import org.jetbrains.slp.Language
import org.jetbrains.slp.lexing.LexerResolver
import org.jetbrains.slp.modeling.runners.LocalGlobalModelRunner
import org.jetbrains.slp.modeling.runners.ModelRunner

class ModelManager {
    private val modelsHolder = mutableMapOf<String, LocalGlobalModelRunner>()

    fun getModelForExtension(extension: String): LocalGlobalModelRunner {
        if (extension !in modelsHolder.keys)
            registerModel(extension)

        return modelsHolder[extension]!!
    }

    fun getModelForLanguage(language: Language): LocalGlobalModelRunner {
        if (language.extensions.none { it in modelsHolder.keys })
            language.extensions.forEach { registerModel(it) }

        return modelsHolder[language.extensions.first()]!!

    }

    private fun registerModel(extension: String) {
        val model = LocalGlobalModelRunner(lexerRunner = LexerResolver.extensionToLexer(extension))
        modelsHolder[extension] = model
    }

}