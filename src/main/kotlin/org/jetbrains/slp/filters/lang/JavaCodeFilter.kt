package org.jetbrains.slp.filters.lang

import org.jetbrains.slp.filters.CodeFilter
import org.jetbrains.slp.filters.Filters

object JavaCodeFilter : CodeFilter() {
    override val codeDelimiters = listOf(";", "{", "}")
    override val symbolsToExclude: List<String> = listOf(" ")

    init {
        addFilters(
            { Filters.dotFilter(it) },
            { Filters.commaFilter(it) },
            { Filters.leftBraceFilter(it) },
            { Filters.rightBraceFilter(it) },
            { Filters.invokeFilter(it) },
            { Filters.semicolonFilter(it) },
            { Filters.leftIncrementDecrementOperatorFilter(it) },
            { Filters.rightIncrementDecrementOperatorFilter(it) },
            { Filters.arrowFilter(it) }
        )
    }
}