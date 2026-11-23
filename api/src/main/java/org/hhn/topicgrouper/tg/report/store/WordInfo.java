package org.hhn.topicgrouper.tg.report.store;

import java.io.Serializable;

/**
 * Serializable record of a word type: its id, its frequency and the word
 * itself. A type is either a word (sense 0) or a <em>sense</em> of a word that
 * was split off by a sense splitting solver (sense 1, 2, ...; see
 * {@link org.hhn.topicgrouper.tg.TGSenseSolution}). Natural order is by
 * descending frequency; two infos are equal if they have the same id.
 *
 * @param <T>
 *            the word type
 */
public class WordInfo<T> implements Comparable<WordInfo<T>>, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Id of the word type: the index of the word in the vocabulary for a word,
	 * the type index for a sense.
	 */
	private final int wordId;
	/**
	 * Frequency of the type (the global frequency of the word for a word, the
	 * number of occurrences assigned to the sense for a sense).
	 */
	private final int frequency;
	/**
	 * The word.
	 */
	private final T word;
	/**
	 * The sense number, 0 for the word itself.
	 */
	private final int sense;
	/**
	 * Number of documents in which the word has this sense, 0 if not recorded.
	 */
	private final int documents;

	/**
	 * Creates the info of a word (sense 0).
	 *
	 * @param wordId
	 *            vocabulary id
	 * @param frequency
	 *            global frequency
	 * @param word
	 *            the word
	 */
	public WordInfo(int wordId, int frequency, T word) {
		this(wordId, frequency, word, 0, 0);
	}

	/**
	 * Creates the info of a word type.
	 *
	 * @param wordId
	 *            id of the type (the vocabulary id for a word)
	 * @param frequency
	 *            frequency of the type
	 * @param word
	 *            the word
	 * @param sense
	 *            the sense number, 0 for the word itself
	 * @param documents
	 *            the number of documents in which the word has the sense, 0 if
	 *            unknown
	 */
	public WordInfo(int wordId, int frequency, T word, int sense, int documents) {
		this.word = word;
		this.wordId = wordId;
		this.frequency = frequency;
		this.sense = sense;
		this.documents = documents;
	}

	@Override
	public int compareTo(WordInfo<T> o) {
		return frequency < o.frequency ? 1 : (frequency == o.frequency ? 0 : -1);
	}

	/**
	 * Returns the word.
	 *
	 * @return the word
	 */
	public T getWord() {
		return word;
	}

	/**
	 * Returns the id of the type.
	 *
	 * @return the word id (the vocabulary id for a word, the type index for a
	 *         sense)
	 */
	public int getWordId() {
		return wordId;
	}

	/**
	 * Returns the frequency of the type.
	 *
	 * @return the frequency
	 */
	public int getFrequency() {
		return frequency;
	}

	/**
	 * Returns the sense number.
	 *
	 * @return 0 for a word, 1, 2, ... for its senses
	 */
	public int getSense() {
		return sense;
	}

	/**
	 * Whether this is a sense of a word rather than the word itself.
	 *
	 * @return true if {@link #getSense()} is positive
	 */
	public boolean isSense() {
		return sense > 0;
	}

	/**
	 * Returns the number of documents in which the word has this sense.
	 *
	 * @return the number of documents, 0 if not recorded (always 0 for a word)
	 */
	public int getDocuments() {
		return documents;
	}

	/**
	 * Returns the word with the sense number appended (<code>word'2</code>)
	 * for a sense, the word alone otherwise.
	 *
	 * @return the label of the type
	 */
	public String getLabel() {
		String w = word == null ? String.valueOf(wordId) : word.toString();
		return sense > 0 ? w + "'" + sense : w;
	}

	@Override
	public int hashCode() {
		return wordId;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof WordInfo && ((WordInfo<?>) obj).wordId == wordId;
	}

	@Override
	public String toString() {
		return getLabel() + " (" + frequency + ") ";
	}
}
