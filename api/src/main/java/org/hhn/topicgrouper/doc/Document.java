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
	 *
	 * @return the index of this document in the list returned by {@link DocumentProvider#getDocuments()}
	 */
	public int getIndex();

	/**
	 * The number of distinct words in this document.
	 *
	 * @return the number of word indices with a frequency greater than zero
	 */
	public int getWords();

	/**
	 * The total number of word occurrences in this document.
	 *
	 * @return the sum of all word frequencies
	 */
	public int getSize();

	/**
	 * The indices of all distinct words occurring in this document.
	 *
	 * @return the set of word indices with a frequency greater than zero
	 */
	public TIntSet getWordIndices();

	/**
	 * The frequency of a word in this document.
	 *
	 * @param index the word index (as defined by the provider's vocabulary)
	 * @return the number of occurrences of the word in this document (0 if absent).
	 */
	public int getWordFrequency(int index);

	/**
	 * The provider (and thereby vocabulary) this document belongs to.
	 *
	 * @return the provider
	 */
	public DocumentProvider<T> getProvider();
}
