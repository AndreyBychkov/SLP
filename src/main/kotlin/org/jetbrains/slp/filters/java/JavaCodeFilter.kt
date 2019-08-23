package org.jetbrains.slp.filters.java

import org.jetbrains.slp.filters.CodeFilter

object JavaCodeFilter : CodeFilter() {
    override val delimiters = listOf(";", "{", "}")

    init {
        addFilters(
            { dotFilter(it) },
            { commaFilter(it) },
            { leftBraceFilter(it) },
            { rightBraceFilter(it) },
            { invokeFilter(it) },
            { delimiterFilter(it) },
            { leftIncrementDecrementOperatorFilter(it) },
            { rightIncrementDecrementOperatorFilter(it) },
            { arrowFilter(it) }
        )
    }

    private fun dotFilter(input: String) =
        Regex("""\s*([.])\s*""").replace(input, "$1")

    private fun commaFilter(input: String) =
        Regex("""\s,\s""").replace(input, """, """)

    private fun leftBraceFilter(input: String) =
        Regex("""([(\[])\s*""").replace(input, "$1")

    private fun rightBraceFilter(input: String) =
        Regex("""\s*([)\]])""").replace(input, "$1")

    private fun invokeFilter(input: String) =
        Regex("""\s([(\[])""").replace(input, "$1")

    private fun delimiterFilter(input: String) =
        Regex("""\s([;])""").replace(input, "$1")

    private fun leftIncrementDecrementOperatorFilter(input: String) =
        Regex("""([+-]{2})\s?""").replace(input, "$1")

    private fun rightIncrementDecrementOperatorFilter(input: String) =
        Regex("""(\w)\s?([+-]{2})""").replace(input, "$1$2")

    private fun arrowFilter(input: String) =
        Regex("""-\s>""").replace(input, "->")
}