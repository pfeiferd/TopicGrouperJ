package org.hhn.topicgrouper.tg.report.store;

import java.io.Serializable;

/**
 * Serializable record of a word: its id, global frequency and the word itself. Natural order is by descending frequency.
 * @param <T> the word type
 */
public class WordInfo<T> implements Comparable<WordInfo<T>>,
		Serializable {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Id of the word in the vocabulary.
	 */
	private final int wordId;
	/**
	 * Global frequency of the word.
	 */
	private final int frequency;
	/**
	 * The word.
	 */
	private final T word;

	/**
	 * Creates a word info.
	 * @param wordId vocabulary id
	 * @param frequency global frequency
	 * @param word the word
	 */
	public WordInfo(int wordId, int frequency, T word) {
		this.word = word;
		this.wordId = wordId;
		this.frequency = frequency;
	}

	@Override
	public int compareTo(WordInfo<T> o) {
		return frequency < o.frequency ? 1 : (frequency == o.frequency ? 0
				: -1);
	}

	/**
	 * Returns the word.
	 * @return the word
	 */
	public T getWord() {
		return word;
	}

	/**
	 * Returns the vocabulary id.
	 * @return the word id
	 */
	public int getWordId() {
		return wordId;
	}

	/**
	 * Returns the global frequency.
	 * @return the frequency
	 */
	public int getFrequency() {
		return frequency;
	}
	
	@Override
	public String toString() {
		return word + " (" + frequency + ") ";
	}
}