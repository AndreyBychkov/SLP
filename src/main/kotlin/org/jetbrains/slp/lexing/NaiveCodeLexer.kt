// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.slp.lexing

import java.util.*

class NaiveCodeLexer : Lexer {

  override fun lexLine(line: String): Sequence<String> {
    val tokenizer = StringTokenizer(line, ".,:;!?+-*/%()[]{}&|~^<>= \t", true)
    return object : Iterator<String> {
      override fun hasNext(): Boolean {
        return tokenizer.hasMoreTokens()
      }

      override fun next(): String {
        return tokenizer.nextToken()
      }
    }.asSequence().filter { token -> token.trim() != "" }

  }

}