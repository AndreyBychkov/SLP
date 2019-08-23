package org.jetbrains.slp.translating

import java.io.Serializable
import java.util.*

/**
 * Translation (to integers) is the second step (after Lexing) before any modeling takes place.
 * The this is global (static) and is open by default; it can be initialized through
 * the [thisRunner] class or simply left open to be filled by the modeling code
 * (as has been shown to be more appropriate for modeling source code).
 * <br></br>
 * *Note:* the counts in this class are for informative purposes only:
 * these are not (to be) used by any model nor updated with training.
 *
 * @author Vincent Hellendoorn
 */
open class Vocabulary : Serializable {

    val wordIndices: MutableMap<String, Int>
    val words: MutableList<String>
    val counts: MutableList<Int>

    private var closed: Boolean = false

    private var checkPoint: Int = 0

    init {
        wordIndices = HashMap()
        words = ArrayList()
        counts = ArrayList()
        closed = false
        addUnk()
    }

    private fun addUnk() {
        wordIndices[unknownCharacter] = 0
        words.add(unknownCharacter)
        counts.add(0)
    }

    fun size(): Int {
        return words.size
    }

    fun close() {
        closed = true
    }

    fun open() {
        closed = false
    }

    fun setCheckpoint() {
        checkPoint = words.size
    }

    fun restoreCheckpoint() {
        for (i in words.size downTo checkPoint + 1) {
            counts.removeAt(counts.size - 1)
            val word = words.removeAt(words.size - 1)
            wordIndices.remove(word)
        }
    }

    fun store(token: String, count: Int = 1): Int {
        var index: Int? = wordIndices[token]
        if (index == null) {
            index = wordIndices.size
            wordIndices[token] = index
            words.add(token)
            counts.add(count)
        } else {
            counts[index] = count
        }
        return index
    }

    fun toIndices(tokens: Sequence<String>): Sequence<Int> {
        return tokens.map { toIndex(it) }
    }

    fun toIndices(tokens: List<String>): List<Int?> {
        return tokens.map { toIndex(it) }
    }

    fun toIndex(token: String): Int {
        var index: Int? = wordIndices[token]
        if (index == null) {
            if (closed) {
                return wordIndices[unknownCharacter]!!
            } else {
                index = wordIndices.size
                wordIndices[token] = index
                words.add(token)
                counts.add(1)
            }
        }
        return index
    }

    fun getCount(token: String): Int? {
        val index = wordIndices[token]
        return index?.let { getCount(it) } ?: 0
    }

    private fun getCount(index: Int?): Int? {
        return counts[index!!]
    }

    fun toWords(indices: Sequence<Int>): Sequence<String> {
        return indices.map { toWord(it) }
    }

    fun toWords(indices: List<Int>): List<String> {
        return indices.map { toWord(it) }
    }

    fun toWord(index: Int): String {
        return words[index]
    }

    companion object {

        const val unknownCharacter = "<unknownCharacter>"
        const val beginOfString = "<s>"
        const val endOfString = "</s>"
    }
}