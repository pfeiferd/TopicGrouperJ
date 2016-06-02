package org.hhn.topicgrouper.base;

import gnu.trove.list.TIntList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;

import java.util.Collections;
import java.util.List;

public class WordMapDocumentProvider<T> implements DocumentProvider<T> {
	protected final TObjectIntMap<T> wordToIndex;
	protected final TIntObjectMap<T> indexToWord;
	private final List<Document<T>> immutableList;
	protected final List<Document<T>> entries;
	protected final TIntList indexToFr;

	public WordMapDocumentProvider(TObjectIntMap<T> wordToIndex,
			TIntObjectMap<T> indexToWord, List<Document<T>> entries,
			TIntList indexToFr) {
		this.wordToIndex = wordToIndex;
		this.indexToWord = indexToWord;
		this.entries = entries;
		this.immutableList = Collections.unmodifiableList(entries);
		this.indexToFr = indexToFr;
	}

	@Override
	public int getNumberOfWords() {
		return wordToIndex.size();
	}

	@Override
	public List<Document<T>> getDocuments() {
		return immutableList;
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
	public int getWordFrequency(int index) {
		return indexToFr.get(index);
	}
}
