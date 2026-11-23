package org.hhn.topicgrouper.doc.impl;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

/**
 * Vocabulary backed by hash maps; word indices are assigned consecutively starting with 0 in insertion order.
 *
 * @param <T> the word type
 */
public class DefaultVocab<T> implements Vocab<T> {

	private static final long serialVersionUID = 6159283740829188125L;

	/**
	 * Word to index.
	 */
	protected final TObjectIntMap<T> wordToIndex;
	/**
	 * Index to word.
	 */
	protected final TIntObjectMap<T> indexToWord;
	/**
	 * The index assigned to the next new word, i.e. the current number of words.
	 */
	private int nextIndex;

	/**
	 * Creates an empty vocabulary.
	 */
	public DefaultVocab() {
		this.wordToIndex = new TObjectIntHashMap<T>();
		this.indexToWord = new TIntObjectHashMap<T>();
		nextIndex = 0;
	}
	
	/**
	 * Adds a word if not yet present.
	 *
	 * @param word the word
	 * @return the index of the word (new or existing)
	 */
	public int addEntry(T word) {
		int index;
		if (!wordToIndex.containsKey(word)) {
			index = nextIndex++;
			putEntry(word, index);
		}
		else {
			index = wordToIndex.get(word);
		}
		return index;
	}
	
	/**
	 * Registers a word under a given index in both maps.
	 *
	 * @param word the word
	 * @param index its index
	 */
	protected void putEntry(T word, int index) {
		wordToIndex.put(word, index);
		indexToWord.put(index, word);		
	}

	/**
	 * @return the number of words
	 */
	@Override
	public int getNumberOfWords() {
		return wordToIndex.size();
	}

	/**
	 * @param index the word index
	 * @return the word, or <code>null</code> if the index is unknown
	 */
	@Override
	public T getWord(int index) {
		return indexToWord.get(index);
	}
	
	/**
	 * @param word the word
	 * @return the index of the word or -1 if it is not in the vocabulary
	 */
	@Override
	public int getIndex(T word) {
		return wordToIndex.containsKey(word) ? wordToIndex.get(word) : -1;
	}
	
	/**
	 * Two vocabularies are equal if they map the same words to the same indices.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DefaultVocab<?>) {
			DefaultVocab<T> other = (DefaultVocab<T>) obj;
			return wordToIndex.equals(other.wordToIndex);
		}
		return false;
	}
	
	/**
	 * @return a hash code consistent with {@link #equals(Object)}
	 */
	@Override
	public int hashCode() {
		return wordToIndex.hashCode();
	}
}
