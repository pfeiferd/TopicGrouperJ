package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;

import org.hhn.topicgrouper.doc.Document;

/**
 * Base class for documents that store word frequencies in a map from word index to count.
 * Subclasses supply the provider.
 *
 * @param <T> the word type
 */
public abstract class AbstractDocumentImpl<T> implements Document<T> {

	private static final long serialVersionUID = -7104682442715739555L;

	/**
	 * Total number of word occurrences.
	 */
	private int size;
	/**
	 * Word index to frequency.
	 */
	private final TIntIntMap frequencies;
	
	/**
	 * Index of the document in its provider.
	 */
	protected int index;

	/**
	 * Creates an empty document.
	 *
	 * @param index the index of the document in its provider
	 */
	public AbstractDocumentImpl(int index) {
		this.frequencies = new TIntIntHashMap();
		size = 0;
		this.index = index;
	}
	
	/**
	 * @return the index of the document in its provider
	 */
	@Override
	public int getIndex() {
		return index;
	}

	/**
	 * @return the number of distinct words
	 */
	@Override
	public int getWords() {
		return frequencies.size();
	}

	/**
	 * @return the total number of word occurrences
	 */
	@Override
	public int getSize() {
		return size;
	}

	/**
	 * @param index the word index
	 * @return the frequency of the word, 0 if it does not occur
	 */
	@Override
	public int getWordFrequency(int index) {
		int f = frequencies.get(index);
		return f == gnu.trove.impl.Constants.DEFAULT_INT_NO_ENTRY_VALUE ? 0 : f;
	}

	/**
	 * @return the indices of the distinct words (a live view, do not modify)
	 */
	@Override
	public TIntSet getWordIndices() {
		return frequencies.keySet();
	}

	/**
	 * Adds one occurrence of a word.
	 *
	 * @param index the word index
	 * @return the new frequency of the word
	 */
	public int addWordOccurrence(int index) {
		return addWordOccurrence(index, 1);
	}

	/**
	 * Adds several occurrences of a word.
	 *
	 * @param index the word index
	 * @param times number of occurrences to add
	 * @return the new frequency of the word
	 */
	public int addWordOccurrence(int index, int times) {
		int newF = frequencies.adjustOrPutValue(index, times, times);
		size += times;
		return newF;
	}

	/**
	 * @return the words with their frequencies, e.g. <code>{word(2) other(1) }</code>
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('{');
		TIntIterator it = getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			int fr = getWordFrequency(index);
			if (fr > 0) {
				T word = getProvider().getVocab().getWord(index);
				builder.append(word);
				builder.append('(');
				builder.append(fr);
				builder.append(") ");
			}
		}
		builder.append('}');
		return builder.toString();
	}
}
