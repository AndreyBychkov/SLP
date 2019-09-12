package org.jetbrains.slp.modeling

import org.jetbrains.slp.Language
import org.jetbrains.slp.lexing.LexerRunnerFactory
import org.jetbrains.slp.modeling.runners.LocalGlobalModelRunner
import java.io.File

class ModelRunnerManager {
    private val modelsHolder = mutableMapOf<String, LocalGlobalModelRunner>()

    fun contains(extension: String) =
        modelsHolder.keys.contains(extension)

    fun getModelRunner(extension: String): LocalGlobalModelRunner {
        if (extension !in modelsHolder.keys)
            registerModelRunner(extension)

        return modelsHolder[extension]!!
    }

    fun getModelRunner(language: Language): LocalGlobalModelRunner {
        registerModelRunner(language)

        return modelsHolder[language.extensions.first()]!!
    }

    fun registerModelRunner(extension: String) {
        val modelRunner = LocalGlobalModelRunner(lexerRunner = LexerRunnerFactory.getLexerRunner(extension))
        registerModelRunner(modelRunner, extension)
    }

    fun registerModelRunner(language: Language) {
        val unusedExtensions = language.extensions.minus(modelsHolder.keys)
        val modelKey = modelsHolder.keys.intersect(language.extensions).firstOrNull()
        val modelRunner =
            modelsHolder[modelKey] ?: LocalGlobalModelRunner(lexerRunner = LexerRunnerFactory.getLexerRunner(language))

        unusedExtensions.forEach {
            registerModelRunner(modelRunner, it)
        }
    }

    fun registerModelRunner(modelRunner: LocalGlobalModelRunner, language: Language) {
        val unusedExtensions = language.extensions.minus(modelsHolder.keys)
        unusedExtensions.forEach {
            registerModelRunner(modelRunner, it)
        }
    }

    fun registerModelRunner(modelRunner: LocalGlobalModelRunner, extension: String) {
        modelsHolder[extension] = modelRunner
    }

    fun save(directory: File) {
        modelsHolder
            .mapKeys { entry -> Language.getLanguage(entry.key).toString() }
            .forEach {
                File(makePath(directory, it.key)).apply {
                    mkdir()
                    it.value.save(this)
                }
            }
    }

    fun load(directory: File) {
        modelsHolder.clear()
        val subdirectories = directory.listFiles(File::isDirectory)!!.filterNotNull()
        val languages = subdirectories.map { Language.valueOf(it.name) }

        for ((subdir, language) in subdirectories.zip(languages)) {
            registerModelRunner(LocalGlobalModelRunner.load(subdir, language), language)
        }
    }

    private fun makePath(directory: File, subdirectoryName: String) =
        directory.path + File.separator + subdirectoryName

}