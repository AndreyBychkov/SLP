package org.jetbrains.slp.filters

internal object DefaultCodeFilter: CodeFilter() {
    override val codeDelimiters = listOf(";")
    override val symbolsToExclude: List<String> = listOf(" ")

    init {
        addFilters(
            { Filters.dotFilter(it) },
            { Filters.commaFilter(it) },
            { Filters.invokeFilter(it) },
            { Filters.leftBraceFilter(it) },
            { Filters.rightBraceFilter(it) },
            { Filters.vocabularyFilter(it) },
            { it.trim() }
        )
    }
}