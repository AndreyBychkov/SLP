// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.slp.lexing

import org.jetbrains.slp.filters.CodeFilter
import org.jetbrains.slp.filters.DefaultCodeFilter
import java.io.File
import java.io.IOException
import java.nio.file.Files

import org.jetbrains.slp.modeling.Model
import org.jetbrains.slp.translating.Vocabulary
import kotlin.streams.asSequence

/**
 * This class can be used to run a [Lexer] over bodies of code.
 * It differentiates between lexing each line separately or each file as a whole,
 * and adds several options, such as adding markers at the start and end of every sentence,
 * and only lexing files that match some extension/regular expression.
 * It also provides some util methods like [.lexDirectory] and variants.
 *
 * @author Vincent Hellendoorn
 */
class LexerRunner
/**
 * Create a LexerRunner that wraps a [Lexer] and adds line separation if needed.
 * <br></br>
 * In some tasks (especially in NLP), a file with unrelated individual sentences on each line tends to be used,
 * whereas in most code applications, we tend to use a complete code file in which the lines should be treated as a continuous block.
 * The LexerRunner (and ModelRunner, which uses this class) need to know this to allow appropriate training.
 *
 * @param lexer A [Lexer] that can produce a stream of tokens for each line in a File, or for single-line inputs.
 * @param isPerLine Whether the data that this LexerRunner will consider is logically grouped by lines or files.
 */
    (
    /**
     * Returns the lexer currently used by this class
     */
    val lexer: Lexer,
    /**
     * Returns whether lexing adds delimiters per line.
     */
    val isPerLine: Boolean,
    val filter: CodeFilter = DefaultCodeFilter
) {

    private var sentenceMarkers = false
    /**
     * Returns the regex currently used to filter input files to lex.
     */
    /**
     * Specify regex for file extensions to be kept.
     * <br></br>
     * *Note:* to just specify the extension, use the more convenient [.setExtension].
     * @param regex Regular expression to match file name against. E.g. ".*\\.(c|h)" for C source and header files.
     */
    var regex = ".*"

    // TODO(mb worth converting to property)
    /**
     * Convenience method that adds sentence markers if those aren't yet present in the data.
     * A [Model] always uses the first token as a ground truth (and thus does not model it)
     * and models up to and including the last token.
     * <br></br>
     * If set to 'true', this adds delimiters (i.e. "&lt;s&gt;" and "&lt;/s&gt;"; see [Vocabulary]) to each sentence.
     * A sentence is either every line in a file (if this LexerRunner is created to lex lines separately) or a whole file.
     * <br></br>
     * @param useDelimiters Whether to add delimiters to each sentence. Default: false, which assumes these have already been added.
     */
    fun setSentenceMarkers(useDelimiters: Boolean) {
        sentenceMarkers = useDelimiters
    }

    /**
     * Returns whether or not file/line (depending on `perLine`) sentence markers are added.
     */
    fun hasSentenceMarkers(): Boolean {
        return sentenceMarkers
    }

    /**
     * Alternative to [.setRegex] that allows you to specify just the extension.
     * <br></br>
     * *Note:* this prepends `.*\\.` to the provided extensionRegex!
     * @param extensionRegex Regular expression to match against extension of files. E.g. "(c|h)" for C source and header files.
     */
    fun setExtension(extensionRegex: String) {
        regex = ".*\\.$extensionRegex"
    }

    /**
     * Returns whether the file matches the regex and will thus be lexed by this class
     */
    fun willLexFile(file: File): Boolean {
        return file.name.matches(regex.toRegex())
    }

    // TODO(try to remove null)
    /**
     * Lex each file in this directory to a their tokens grouped by lines, subject to the underlying [Lexer]
     * and whether this Lexer is configured to work per line
     * @param directory
     * @return
     */
    fun lexDirectory(directory: File): Sequence<Pair<File, Sequence<Sequence<String>>>>? {
        return try {
            Files.walk(directory.toPath())
                .asSequence()
                .map { it.toFile() }
                .filter { it.isFile }
                .filter { willLexFile(it) }
                .map { fIn -> Pair(fIn, lexFile(fIn)) }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

    }

    /**
     * Lex the provided file to a stream of tokens per line. Note that this is preferred over lex(lines),
     * since knowing the file location/context can be helpful for most lexers!
     * <br></br>
     * *Note:* returns empty stream if the file does not match this builder's regex
     * (which accepts everything unless set otherwise in [.setRegex]).
     * @param file File to lex
     */
    fun lexFile(file: File): Sequence<Sequence<String>> {
        return if (!willLexFile(file))
            emptySequence()
        else
            lexTokens(lexer.lexFile(file))
    }

    /**
     * Lex the provided text to a stream of tokens per line.
     * **Note:** if possible, use lex(File) instead! Knowing the file location/context can benefit e.g. AST lexers.
     *
     * @param content Textual content to lex
     */
    fun lexText(content: String): Sequence<Sequence<String>> {
        return lexTokens(lexer.lexText(content))
    }

    fun lexLine(line: String): Sequence<String> {
        val lexed = lexer.lexLine(line)
        val filtered = filter.excludeForbiddenSymbols(lexed)

        return if (sentenceMarkers)
            (sequenceOf(Vocabulary.beginOfString) + (filtered + sequenceOf(Vocabulary.endOfString)))
        else
            filtered

    }

    fun clearSentenceMarkers(tokens: List<String>): List<String> {
        return tokens.filter { it !in listOf(Vocabulary.beginOfString, Vocabulary.endOfString) }
    }

    /**
     * Lex a directory recursively, provided for convenience.
     * Creates a mirror-structure in 'to' that has the lexed (and translated if [.preTranslate] is set) file for each input file
     * @param from Source file/directory to be lexed
     * @param to Target file/directory to be created with lexed (optionally translated) content from source
     */
    fun lexDirectory(from: File, to: File) {
        lexDirectoryToIndices(from, to, null)
    }

    // TODO(Why Vocabulary is here? Can we remove it?)
    /**
     * Lex a directory recursively, provided for convenience.
     * Creates a mirror-structure in 'to' that has the lexed (and translated if [.preTranslate] is set) file for each input file
     * @param from Source file/directory to be lexed
     * @param to Target file/directory to be created with lexed (optionally translated) content from source
     * @param vocabulary The Vocabulary to translate the words to indices in said Vocabulary.
     * If no translation is required, use [.lexDirectory]
     */
    fun lexDirectoryToIndices(from: File, to: File, vocabulary: Vocabulary?) {
        val count = intArrayOf(0)
        try {
            Files.walk(from.toPath())
                .map { it.toFile() }
                .filter { it.isFile }
                .forEach { fIn ->
                    if (++count[0] % 1000 == 0) {
                        println("Lexing at file " + count[0])
                    }
                    val path = to.absolutePath + fIn.absolutePath.substring(from.absolutePath.length)
                    val fOut = File(path)
                    val outDir = fOut.parentFile
                    outDir.mkdirs()

                    try {
                        val lexed = lexFile(fIn)
                        lexed.map { l -> l.map { w -> if (vocabulary == null) w else vocabulary.store(w).toString() + "" } }
                        //Writer.writeTokenized(fOut, lexed)
                    } catch (e: IOException) {
                        println("Exception in LexerBuilder.tokenize(), from $fIn to $fOut")
                        e.printStackTrace()
                    }
                }
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

    }

    private fun lexTokens(tokens: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        val lexed = if (sentenceMarkers) lexWithDelimiters(tokens) else tokens

        return lexed.map { filter.excludeForbiddenSymbols(it) }
    }

    private fun lexWithDelimiters(lexed: Sequence<Sequence<String>>): Sequence<Sequence<String>> {
        return if (isPerLine)
            lexed.map { sequenceOf(Vocabulary.beginOfString) + it + sequenceOf(Vocabulary.endOfString) }
        else
            sequenceOf(sequenceOf(Vocabulary.beginOfString)) + lexed + sequenceOf(sequenceOf(Vocabulary.endOfString))

    }


}
