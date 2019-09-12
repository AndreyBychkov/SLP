package org.jetbrains.slp.lexing

import org.jetbrains.slp.Language
import org.jetbrains.slp.filters.DefaultCodeFilter
import org.jetbrains.slp.filters.lang.CCodeFilter
import org.jetbrains.slp.filters.lang.JavaCodeFilter
import org.jetbrains.slp.filters.lang.KotlinCodeFilter
import org.jetbrains.slp.filters.lang.PythonCodeFilter

object LexerRunnerFactory {

    fun getLexerRunner(extension: String)  = when(extension) {
        in Language.JAVA.extensions -> makeLexerRunner(Language.JAVA, NaiveCodeLexer())
        in Language.PYTHON.extensions -> makeLexerRunner(Language.PYTHON, NaiveCodeLexer())
        in Language.KOTLIN.extensions -> makeLexerRunner(Language.KOTLIN, NaiveCodeLexer())
        in Language.C.extensions -> makeLexerRunner(Language.C, NaiveCodeLexer())
        else -> LexerRunner(NaiveCodeLexer(), true).apply {
            setSentenceMarkers(true)
        }
    }

    fun getLexerRunner(language: Language) = when(language) {
        Language.JAVA -> makeLexerRunner(Language.JAVA, NaiveCodeLexer())
        Language.PYTHON -> makeLexerRunner(Language.PYTHON, NaiveCodeLexer())
        Language.KOTLIN -> makeLexerRunner(Language.KOTLIN, NaiveCodeLexer())
        Language.C -> makeLexerRunner(Language.C, NaiveCodeLexer())
        Language.UNKNOWN -> LexerRunner(NaiveCodeLexer(), true).apply {
            setSentenceMarkers(true)
        }
    }

    private fun languageToFilter(language: Language) = when(language) {
        Language.JAVA -> JavaCodeFilter
        Language.PYTHON -> PythonCodeFilter
        Language.KOTLIN -> KotlinCodeFilter
        Language.C -> CCodeFilter
        Language.UNKNOWN -> DefaultCodeFilter
    }

    private fun makeLexerRunner(language: Language, lexer: Lexer) =
        LexerRunner(lexer, true, languageToFilter(language)).apply {
            regex = (extensionsToRegex(language.extensions))
            setSentenceMarkers(true)
        }

    private fun extensionsToRegex(extensions: List<String>) =
        """.*\.(${extensions.joinToString("|")})"""

}