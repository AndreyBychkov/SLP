package org.jetbrains.slp.filters.lang

import org.jetbrains.slp.filters.CodeFilter
import org.jetbrains.slp.filters.Filters

object PythonCodeFilter: CodeFilter() {
    override val codeDelimiters = listOf(":")
    override val symbolsToExclude: List<String> = listOf(" ")

    init {
        addFilters(
            { Filters.dotFilter(it) },
            { Filters.commaFilter(it) },
            { Filters.invokeFilter(it) },
            { Filters.leftBraceFilter(it) },
            { Filters.rightBraceFilter(it) },
            { Filters.colonFilter(it) },
            { Filters.arrowFilter(it) }
        )
    }
}