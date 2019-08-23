package org.jetbrains.slp

import org.jetbrains.slp.filters.java.JavaCodeFilter
import org.jetbrains.slp.modeling.runners.ModelRunner
import java.io.File

object Printer {
    fun printStatisticsOnDirectory(model: ModelRunner, directoryPath: File) {
        val statistics = model.getStatisticsOnDirectory(directoryPath)

        println("Modeled ${statistics.count} tokens, average entropy: ${statistics.average}")
    }

    fun printSuggestionsOnCodeSamples(modelRunner: ModelRunner, codeSamples: Map<String, String>) {
        codeSamples.forEach {
            println()
            println("${it.key}:".toUpperCase())
            println("${it.value} ${modelRunner.getTopSuggestions(it.value)}")
        }
    }

    fun printSuggestionsOnCodeSamplesWithProbabilities(modelRunner: ModelRunner, codeSamples: Map<String, String>) {
        codeSamples.forEach {
            println()
            println("${it.key}:".toUpperCase())
            println("${it.value} ${modelRunner.getTopSuggestionsWithProbabilities(it.value)}")
        }
    }

    fun printExpandedCodeSamples(
      modelRunner: ModelRunner,
      codeSamples: Map<String, String>,
      maxIter: Int = 40
    ) {
        codeSamples.forEach {
            println()
            println("${it.key}:".toUpperCase())
            println(it.value)
            println("-".repeat(40))
            println(modelRunner.expandCode(it.value, maxIter))
        }
    }

    fun printFilteredExpandingSuggestionsOnCodeSamples(
      modelRunner: ModelRunner,
      codeSamples: Map<String, String>
    ) {
        codeSamples.forEach {
            println()
            println("${it.key}:".toUpperCase())
            println(it.value)
            println("-".repeat(40))
            println("${it.value}${JavaCodeFilter.applyFilter(
                modelRunner.getExpandedSuggestion(it.value)
            ).trim()}")
        }
    }
}