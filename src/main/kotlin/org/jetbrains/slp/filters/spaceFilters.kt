package org.jetbrains.slp.filters

fun codeSpaceFilter(input: String) =
  dotFilter(
    commaFilter(
      leftBraceFilter(
        rightBraceFilter(
          invokeFilter(
            incrementDecrementFilter(
              delimiterFilter(
                arrowFilter(
                  input
                )
              )
            )
          )
        )
      )
    )
  ).trim()

private fun dotFilter(input: String) =
  Regex("""\s*([.])\s*""").replace(input, "$1")

fun commaFilter(input: String) =
  Regex("""\s,\s""").replace(input, """, """)

private fun leftBraceFilter(input: String) =
  Regex("""([(\[])\s*""").replace(input, "$1")

private fun rightBraceFilter(input: String) =
  Regex("""\s*([)\]])""").replace(input, "$1")

private fun invokeFilter(input: String) =
  Regex("""\s([(\[])""").replace(input, "$1")

private fun delimiterFilter(input: String) =
  Regex("""\s([;])""").replace(input, "$1")

private fun incrementDecrementFilter(input: String) =
  leftIncrementDecrementOperatorFilter(
    rightIncrementDecrementOperatorFilter(
      input
    )
  )

private fun leftIncrementDecrementOperatorFilter(input: String) =
  Regex("""([+-]{2})\s?""").replace(input, "$1")

private fun rightIncrementDecrementOperatorFilter(input: String) =
  Regex("""(\w)\s?([+-]{2})""").replace(input, "$1$2")

private fun arrowFilter(input: String) =
  Regex("""-\s>""").replace(input, "->")



