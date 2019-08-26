package org.jetbrains.slp.filters

abstract class CodeFilter {
    abstract val codeDelimiters: List<String>
    abstract val symbolsToExclude: List<String>

    private val filters: MutableList<(String) -> String> = mutableListOf()

    protected fun addFilters(vararg filters: (String) -> String) {
        this.filters.addAll(filters)
    }

    fun getCodeBetweenDelimiters(input: String): List<String> {
        val results = Regex("""(.*?)([${codeDelimiters.joinToString("")}])""").findAll(input).toList()

        return if (results.isNotEmpty())
            results.map { "${it.groupValues[1]}${it.groupValues[2]}" }
        else
            listOf(input)
    }

    fun applyFilter(input: String) =
        filters.fold(input, { acc, function -> function(acc) })
    
    fun excludeForbiddenSymbols(input: List<String>) =
        input.filter { it !in symbolsToExclude }

    fun excludeForbiddenSymbols(input: Sequence<String>) =
        input.filter { it !in symbolsToExclude }
}