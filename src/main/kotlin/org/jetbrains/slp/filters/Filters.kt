package org.jetbrains.slp.filters

object Filters {
    fun dotFilter(input: String) =
        Regex("""\s*([.])\s*""").replace(input, "$1")

    fun commaFilter(input: String) =
        Regex("""\s,\s""").replace(input, """, """)

    fun leftBraceFilter(input: String) =
        Regex("""([(\[])\s*""").replace(input, "$1")

    fun rightBraceFilter(input: String) =
        Regex("""\s*([)\]])""").replace(input, "$1")

    fun invokeFilter(input: String) =
        Regex("""\s([(\[])""").replace(input, "$1")

    fun colonFilter(input: String) =
        Regex("""\s*:""").replace(input, ":")

    fun semicolonFilter(input: String) =
        Regex("""\s([;])""").replace(input, "$1")

    fun leftIncrementDecrementOperatorFilter(input: String) =
        Regex("""([+-]{2})\s?""").replace(input, "$1")

    fun rightIncrementDecrementOperatorFilter(input: String) =
        Regex("""(\w)\s?([+-]{2})""").replace(input, "$1$2")

    fun arrowFilter(input: String) =
        Regex("""-\s>""").replace(input, "->")
}