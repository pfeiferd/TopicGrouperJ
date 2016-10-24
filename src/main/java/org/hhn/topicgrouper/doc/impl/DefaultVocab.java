package org.hhn.topicgrouper.doc.impl;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

public class DefaultVocab<T> implements Vocab<T> {
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
			wordToIndex.put(word, index);
			indexToWord.put(index, word);
		}
		else {
			index = wordToIndex.get(word);
		}
		return index;
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
}
