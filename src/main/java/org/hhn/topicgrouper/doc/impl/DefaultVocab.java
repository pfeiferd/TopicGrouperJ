package org.hhn.topicgrouper.doc.impl;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

public class DefaultVocab<T> implements Vocab<T> {

	private static final long serialVersionUID = 6159283740829188125L;

	protected final TObjectIntMap<T> wordToIndex;
	protected final TIntObjectMap<T> indexToWord;
	private int nextIndex;

	public DefaultVocab() {
		this.wordToIndex = new TObjectIntHashMap<T>();
		this.indexToWord = new TIntObjectHashMap<T>();
		nextIndex = 0;
	}
	
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
	
	protected void putEntry(T word, int index) {
		wordToIndex.put(word, index);
		indexToWord.put(index, word);		
	}

	@Override
	public int getNumberOfWords() {
		return wordToIndex.size();
	}

	@Override
	public T getWord(int index) {
		return indexToWord.get(index);
	}
	
	@Override
	public int getIndex(T word) {
		return wordToIndex.containsKey(word) ? wordToIndex.get(word) : -1;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DefaultVocab<?>) {
			DefaultVocab<T> other = (DefaultVocab<T>) obj;
			return wordToIndex.equals(other.wordToIndex);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return wordToIndex.hashCode();
	}
}
