package org.jetbrains.slp.io

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlin.streams.toList

object Reader {

    fun readLines(file: File): List<String> {
        return try {
            commonRead(file)
        } catch (e: IOException) {
            readLinesWithBufferedReader(file)
        } catch (e: UncheckedIOException) {
            readLinesWithBufferedReader(file)
        }
    }

    private fun commonRead(file: File): List<String> {
        val decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.IGNORE)

        return BufferedReader(
            Channels.newReader(
                FileChannel.open(file.toPath()), decoder, -1
            )
        ).use { br -> br.lines().toList() }
    }

    private fun readLinesWithBufferedReader(file: File): List<String> {
        System.err.println("Reader.readLines(): Files.lines failed, reading full file using BufferedReader instead")

        return try {
            val bufferedReader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))

            readFromBufferedReader(bufferedReader)

        } catch (e2: IOException) {
            e2.printStackTrace()
            listOf()
        }
    }

    private fun readFromBufferedReader(br: BufferedReader): List<String> {
        val lines = mutableListOf<String>()
        var line: String?

        var reading = true

        while (reading) {
            line = br.readLine()
            if (line == null) {
                reading = false
            }
            else
                lines.add(line)
        }

        return lines
    }
}