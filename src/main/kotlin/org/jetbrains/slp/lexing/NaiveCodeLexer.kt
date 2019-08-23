// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.slp.lexing

class NaiveCodeLexer : Lexer {

  override fun lexLine(line: String) =
    splitKeepDelimiters(line, delimiters, false).asSequence()

  val delimiters = Regex("""
        \.
        |,
        |\s
        |:
        |;
        # Braces
        |\(
        |\)
        |\{
        |}
        |\[
        |]
        # Doubled operators
        |\+\+
        |--
        |\*\*
        |==
        |!=
        |>=
        |<=
        |\+=
        |-=
        |\*=
        |/=
        |%=
        # Single operators
        |\+
        |-
        |\*
        |=
        |%
        |/
        """, RegexOption.COMMENTS)

  private fun splitKeepDelimiters(input: String, regex: Regex, keep_empty: Boolean = false) : List<String> {
    val result = mutableListOf<String>()
    var start = 0
    regex.findAll(input).forEach {
      val substrBefore = input.substring(start, it.range.first())
      if (substrBefore.isNotEmpty() || keep_empty) {
        result.add(substrBefore)
      }
      result.add(it.value)
      start = it.range.last() + 1
    }
    if ( start != input.length ) result.add(input.substring(start))
    return result
  }

}