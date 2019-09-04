package org.jetbrains.slp

import org.jetbrains.slp.modeling.runners.ModelRunner
import org.jetbrains.slp.translating.Vocabulary
import java.io.File
import java.util.*


fun ModelRunner.getStatisticsOnDirectory(directoryPath: File): DoubleSummaryStatistics {
    val modeledFiles = modelDirectory(directoryPath)

    return getStats(modeledFiles)
}


fun ModelRunner.getSuggestion(code: String): String {
    return getSuggestionWithProbability(code).first
}

fun ModelRunner.getSuggestion(tokens: List<String>): String {
    return getSuggestionWithProbability(tokens).first
}

fun ModelRunner.getSuggestionWithProbability(code: String): Pair<String, Double> {
    val tokens = lexerRunner.lexLine(code).toList()

    return getSuggestionWithProbability(tokens)
}

fun ModelRunner.getSuggestionWithProbability(tokens: List<String>): Pair<String, Double> {
    val queryIndices = vocabulary.toIndices(tokens).filterNotNull()
    val prediction = model.predictToken(queryIndices, tokens.size - 1)
        .toList()
        .maxBy { (_, value) -> value.first }!!

    return Pair(vocabulary.toWord(prediction.first), prediction.second.first)
}

fun ModelRunner.getTopSuggestions(code: String, maxNumberOfSuggestions: Int = 5): List<String> {
    return getTopSuggestionsWithProbabilities(code, maxNumberOfSuggestions)
        .map { it.first }
}

fun ModelRunner.getTopSuggestionsWithProbabilities(
    code: String,
    maxNumberOfSuggestions: Int = 5
): List<Pair<String, Double>> {
    val tokens = lexerRunner.lexLine(code).toList()

    return getTopSuggestionsWithProbabilities(tokens, maxNumberOfSuggestions)
}

fun ModelRunner.getTopSuggestionsWithProbabilities(
    tokens: List<String>,
    maxNumberOfSuggestions: Int = 5
): List<Pair<String, Double>> {
    val queryIndices = vocabulary.toIndices(tokens).filterNotNull()
    val predictions = model.predictToken(queryIndices, tokens.size - 1)
        .toList()
        .map { (key, value) -> Pair(vocabulary.toWord(key), value.first) }
        .sortedByDescending { (_, value) -> value }
        .take(maxNumberOfSuggestions)

    return predictions
}

fun ModelRunner.getNGramProbability(tokens: List<String>): Double {
    val queryIndices = vocabulary.toIndices(tokens).filterNotNull()
    val probability = model.modelToken(queryIndices, queryIndices.size - 1).first

    return probability
}

fun ModelRunner.getExpandedSuggestion(code: String) =
    getAllExpandingSuggestions(code, 1).first()

fun ModelRunner.getAllExpandingSuggestions(code: String, limit: Int = 3): List<String> {
    fun String.getCodeBetweenCodeDelimiters() = lexerRunner.filter.getCodeBetweenDelimiters(this)
    fun List<String>.applyFilter() = this.map { lexerRunner.filter.applyFilter(it) }

    return expandCode(code)
        .removePrefix(code)
        .getCodeBetweenCodeDelimiters()
        .take(limit)
        .getExpandingMatches(1)
        .map { it.joinToString("") }
        .applyFilter()
        .filter { it.isNotEmpty() }
}

tailrec fun ModelRunner.expandCode(code: String, iterNum: Int = 100): String {
    if (iterNum == 0)
        return code

    if (code.endsWith("</s>"))
        return code.removeSuffix("</s>")

    val currentSuggestion = try {
        getSuggestion(code)
    } catch (exception: KotlinNullPointerException) {
        Vocabulary.endOfString
    }

    return expandCode("$code $currentSuggestion", iterNum - 1)
}

private fun <T> List<T>.getExpandingMatches(index: Int): List<List<T>> {
    if (index > size)
        return listOf()

    return listOf(subList(0, index)) + getExpandingMatches(index + 1)
}