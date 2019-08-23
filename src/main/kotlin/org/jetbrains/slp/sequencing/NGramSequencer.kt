package org.jetbrains.slp.sequencing

import java.util.ArrayList

import org.jetbrains.slp.modeling.runners.ModelRunner
import kotlin.math.max
import kotlin.math.min

object NGramSequencer {

    fun sequenceForward(tokens: List<Int>, maxOrder: Int): List<List<Int>> {
        val result = ArrayList<List<Int>>()
        for (start in tokens.indices) {
            val end = min(tokens.size, start + maxOrder)
            result.add(tokens.subList(start, end))
        }
        return result
    }

    fun sequenceAround(tokens: List<Int>, index: Int, maxOrder: Int): List<List<Int>> {
        val result = ArrayList<List<Int>>()
        val firstLoc = index - maxOrder + 1
        for (start in max(0, firstLoc)..index) {
            val end = min(tokens.size, start + maxOrder)
            result.add(tokens.subList(start, end))
        }
        return result
    }

    /**
     * Returns the longest possible sublist that doesn't exceed [ModelRunner.DEFAULT_NGRAM_ORDER] in length
     * and <u>includes</u> the token at index `index`.
     */
    fun sequenceAt(tokens: List<Int>, index: Int, maxOrder: Int): List<Int> {
        return tokens.subList(max(0, index - maxOrder + 1), index + 1)
    }
}
