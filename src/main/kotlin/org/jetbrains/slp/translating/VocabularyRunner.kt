package org.jetbrains.slp.translating

import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

import org.jetbrains.slp.io.Reader
import org.jetbrains.slp.lexing.LexerRunner
import kotlin.math.roundToLong

object VocabularyRunner {

    private const val PRINT_FREQ = 1000000
    private var cutOff = 0

    /**
     * Set counts cut-off so that only events seen >= cutOff are considered. Default: 0, which includes every seen token
     * <br></br>
     * *Note:* this has been shown to give a distorted perspective on models of particularly source code,
     * but may be applicable in some circumstances
     *
     * @param cutOff The minimum number of counts of an event in order for it to be considered.
     */
    fun cutOff(cutOff: Int) {
        var cutOff = cutOff
        if (cutOff < 0) {
            println("VocabularyBuilder.cutOff(): negative cut-off given, set to 0 (which includes every token)")
            cutOff = 0
        }
        VocabularyRunner.cutOff = cutOff
    }

    /**
     * Build vocabulary on all files reachable from (constructor) provided root,
     * possibly filtering by name/extension (see [.setRegex]/[.setExtension]).
     * @return
     */
    fun build(lexerRunner: LexerRunner, root: File): Vocabulary {
        val vocabulary = Vocabulary()
        val iterationCount = intArrayOf(0)

        val counts = lexerRunner
            .lexDirectory(root)!!
            .flatMap { it.second }
            .onEach {
                if (++iterationCount[0] % PRINT_FREQ == 0)
                    System.out.printf(
                        "Building vocabulary, %dM tokens processed\n",
                        (iterationCount[0] / PRINT_FREQ).toFloat().roundToLong()
                    )
            }
            .groupingBy { it }
            .eachCount()
            .mapKeys { it.toString() }

        val ordered = counts.entries.sortedByDescending { it.value }

        var unkCount = 0
        for ((token, count) in ordered) {
            if (count < cutOff) {
                unkCount += count
            } else {
                vocabulary.store(token, count)
            }
        }
        vocabulary.store(Vocabulary.unknownCharacter, vocabulary.getCount(Vocabulary.unknownCharacter)!! + unkCount)

        if (iterationCount[0] > PRINT_FREQ)
            println("Vocabulary constructed on ${iterationCount[0]} tokens, size: ${vocabulary.size()}")

        return vocabulary
    }


    fun read(file: File) = when {
        file.isDirectory -> readFile(getVocabularyFile(file))
        file.isFile -> readFile(file)
        else -> throw IllegalArgumentException("Argument must be directory of file")
    }


    private fun readFile(file: File): Vocabulary {
        val vocabulary = Vocabulary()

        Reader.readLines(file)
            .map { it.split("\t".toRegex(), 3) }
            .filter { it[0].toInt() >= cutOff }
            .forEach { split ->
                val count = split[0].toInt()
                val index = split[1].toInt()
                if (index > 0 && index != vocabulary.size()) {
                    println("VocabularyRunner.read(): non-consecutive indices while reading vocabulary!")
                }
                val token = split[2]
                vocabulary.store(token, count)
            }

        return vocabulary
    }

    fun write(vocabulary: Vocabulary, file: File) = when {
        file.isDirectory -> writeFile(vocabulary, getVocabularyFile(file))
        file.isFile -> writeFile(vocabulary, file)
        else -> throw IllegalArgumentException("Argument must be directory of file")
    }

    private fun writeFile(vocabulary: Vocabulary, file: File) {
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { fw ->
                for (i in 0 until vocabulary.size()) {
                    val count = vocabulary.counts[i]
                    val word = vocabulary.words[i]
                    fw.append(count.toString() + "\t" + i + "\t" + word + "\n")
                }
            }
        } catch (e: IOException) {
            println("Error writing vocabulary in Vocabulary.toFile()")
            e.printStackTrace()
        }
    }

    private fun getVocabularyFile(directory: File) =
        File(directory.path + File.pathSeparator + "vocabulary.tsv")
}