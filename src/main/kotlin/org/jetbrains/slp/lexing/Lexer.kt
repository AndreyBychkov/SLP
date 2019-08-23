package org.jetbrains.slp.lexing

import org.jetbrains.slp.io.Reader
import java.io.File

interface Lexer {

    /**
     * Lex the provided text. The default implementation invokes [.lexLine] on each line in the text,
     * but sub-classes may opt to lex the text as a whole instead (e.g. JavaLexer needs to do so to handle comments correctly).
     *
     * @param text The text to be lexed
     * @return A Stream of lines, where every line is lexed to a Stream of tokens
     */

     fun lexText(text: String): Sequence<Sequence<String>> {
        return text
            .split('\n')
            .dropLastWhile { it.isEmpty() }
            .map { this.lexLine(it) }
            .asSequence()
    }

  /**
   * Lex all the lines in the provided file. Use of this method is preferred, since some Lexers benefit from knowing
   * the file path (e.g. AST Lexers can use this for type inference).
   * By default, invokes [.lexText] with content of file.
   *
   * @param file The file to be lexed
   * @return A Stream of lines, where every line is lexed to a Stream of tokens
   */
  fun lexFile(file: File): Sequence<Sequence<String>> {
    return lexText(Reader.readLines(file).joinToString("\n"))
  }

    /**
     * Lex the provided line into a stream of tokens.
     * The default implementations of [.lexFile] and [.lexText] refer to this method,
     * but sub-classes may override that behavior to take more advantage of the full content.
     *
     * @param line The line to be lexed
     * @return A Stream of tokens that are present on this line (may be an empty Stream).
     */
    fun lexLine(line: String): Sequence<String>
}
