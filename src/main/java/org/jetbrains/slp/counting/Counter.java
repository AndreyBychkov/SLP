package org.jetbrains.slp.counting;

import org.jetbrains.slp.modeling.ngram.NGramModel;

import java.io.Externalizable;
import java.util.List;

/**
 * Interface for counter implementations that can be used by count-based models,
 * most notably the {@link org.jetbrains.slp.counting.trie.ArrayTrieCounter} which provides a rather efficient implementation
 * that is currently used by the {@link NGramModel}s.
 * 
 * @author Vincent Hellendoorn
 *
 */
public interface Counter extends Externalizable {
	
	/**
	 * Convenience method, returns count of Counter object (e.g. root node in trie-counter)
	 * 
	 * @return count of this Counter
	 */
	int getCount();
	
	/**
	 * Returns [context-count, count] pair of {@code indices}, for convenient MLE.
	 * Note: poorly defined on empty list.
	 * 
	 * @param indices Sequence of stored, translated tokens to return counts for
	 * @return The stored [context-count, count] pair of indices
	 */
	long[] getCounts(List<Integer> indices);

	/**
	 * Returns the number of sequences of length n seen `count' times
	 */
	int getCountOfCount(int n, int count);
	
	int getSuccessorCount();
	int getSuccessorCount(List<Integer> indices);
	List<Integer> getTopSuccessors(List<Integer> indices, int limit);
	
	int[] getDistinctCounts(int range, List<Integer> indices);

	void count(List<Integer> indices);
	void unCount(List<Integer> indices);

	default void countBatch(List<List<Integer>> indices) {
		indices.forEach(this::count);
	}
	default void unCountBatch(List<List<Integer>> indices) {
		indices.forEach(this::unCount);
	}
}
