package org.jetbrains.slp.modeling

import org.jetbrains.slp.Language
import org.jetbrains.slp.lexing.LexerRunnerFactory
import org.jetbrains.slp.modeling.runners.LocalGlobalModelRunner

class ModelRunnerManager {
    private val modelsHolder = mutableMapOf<String, LocalGlobalModelRunner>()

    fun getModelRunner(extension: String): LocalGlobalModelRunner {
        if (extension !in modelsHolder.keys)
            registerModelRunner(extension)

        return modelsHolder[extension]!!
    }

    fun getModelRunner(language: Language): LocalGlobalModelRunner {
        if (language.extensions.none { it in modelsHolder.keys })
            language.extensions.forEach { registerModelRunner(it) }

        return modelsHolder[language.extensions.first()]!!
    }

    fun registerModelRunner(extension: String) {
        val modelRunner = LocalGlobalModelRunner(lexerRunner = LexerRunnerFactory.extensionToLexerRunner(extension))
        registerModelRunner(modelRunner, extension)
    }

    fun registerModelRunner(language: Language) {
        val unusedExtensions = language.extensions.intersect(modelsHolder.keys)
        val modelKey = modelsHolder.keys.intersect(language.extensions).first()
        val modelRunner =
            modelsHolder[modelKey] ?: LocalGlobalModelRunner(lexerRunner = LexerRunnerFactory.languageToLexerRunner(language))

        unusedExtensions.forEach {
            registerModelRunner(modelRunner, it)
        }
    }

    fun registerModelRunner(modelRunner: LocalGlobalModelRunner, language: Language) {
        val unusedExtensions = language.extensions.intersect(modelsHolder.keys)
        unusedExtensions.forEach {
            registerModelRunner(modelRunner, it)
        }
    }

    fun registerModelRunner(modelRunner: LocalGlobalModelRunner, extension: String) {
        modelsHolder[extension] = modelRunner
    }

}