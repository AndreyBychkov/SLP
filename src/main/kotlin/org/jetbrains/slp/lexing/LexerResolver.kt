package org.jetbrains.slp.lexing

import org.jetbrains.slp.Language
import org.jetbrains.slp.filters.DefaultCodeFilter
import org.jetbrains.slp.filters.lang.JavaCodeFilter
import org.jetbrains.slp.filters.lang.PythonCodeFilter

object LexerResolver {

    fun extensionToLexer(extension: String)  = when(extension) {
        in Language.JAVA.extensions -> makeLexer(Language.JAVA, NaiveCodeLexer())
        in Language.PYTHON.extensions -> makeLexer(Language.PYTHON, NaiveCodeLexer())
        else -> LexerRunner(NaiveCodeLexer(), true).apply {
            setSentenceMarkers(true)
        }
    }

    private fun languageToFilter(language: Language) = when(language) {
        Language.JAVA -> JavaCodeFilter
        Language.PYTHON -> PythonCodeFilter
        Language.UNKNOWN -> DefaultCodeFilter
    }

    private fun makeLexer(language: Language, lexer: Lexer) =
        LexerRunner(lexer, true, languageToFilter(language)).apply {
            regex = (extensionsToRegex(language.extensions))
            setSentenceMarkers(true)
        }

    private fun extensionsToRegex(extensions: List<String>) =
        """.*\.(${extensions.joinToString("|")})"""

}