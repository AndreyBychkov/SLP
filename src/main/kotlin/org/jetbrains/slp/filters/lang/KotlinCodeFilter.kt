package org.jetbrains.slp.filters.lang

import org.jetbrains.slp.filters.CodeFilter
import org.jetbrains.slp.filters.Filters

object KotlinCodeFilter: CodeFilter() {
    override val codeDelimiters = listOf(";", "{", "}")
    override val symbolsToExclude: List<String> = listOf(" ")

    init {
        KotlinCodeFilter.addFilters(
            { Filters.dotFilter(it) },
            { Filters.commaFilter(it) },
            { Filters.leftBraceFilter(it) },
            { Filters.rightBraceFilter(it) },
            { Filters.invokeFilter(it) },
            { Filters.semicolonFilter(it) },
            { Filters.leftIncrementDecrementOperatorFilter(it) },
            { Filters.rightIncrementDecrementOperatorFilter(it) },
            { Filters.arrowFilter(it) },
            { Filters.templateFilter(it) },
            { Filters.vocabularyFilter(it) },
            { it.trim() }
        )
    }
}