package org.hhn.topicgrouper.doc;

import gnu.trove.set.TIntSet;

import java.io.Serializable;

/**
 * A bag-of-words document: a mapping from word indices to word frequencies.
 *
 * @param <T> the type of the words (e.g. {@link String})
 */
public interface Document<T> extends Serializable {
	/**
	 * The position of this document in its {@link DocumentProvider}.
	 */
	public int getIndex();
	
	/**
	 * The number of distinct words in this document.
	 */
	public int getWords();
	
	/**
	 * The total number of word occurrences in this document.
	 */
	public int getSize();
	
	/**
	 * The indices of all distinct words occurring in this document.
	 */
	public TIntSet getWordIndices();
	
	/**
	 * @param index the word index (as defined by the provider's vocabulary)
	 * @return the number of occurrences of the word in this document (0 if absent).
	 */
	public int getWordFrequency(int index);
	
	/**
	 * The provider (and thereby vocabulary) this document belongs to.
	 */
	public DocumentProvider<T> getProvider();
}
