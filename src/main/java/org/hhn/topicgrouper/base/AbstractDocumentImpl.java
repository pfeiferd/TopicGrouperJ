package org.hhn.topicgrouper.base;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;

public abstract class AbstractDocumentImpl<T> implements Document<T> {
	private int size;
	private final TIntIntMap frequencies;

	public AbstractDocumentImpl() {
		this.frequencies = new TIntIntHashMap();
		size = 0;
	}

	@Override
	public int getWords() {
		return frequencies.size();
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getWordFrequency(int index) {
		int f = frequencies.get(index);
		return f == gnu.trove.impl.Constants.DEFAULT_INT_NO_ENTRY_VALUE ? 0 : f;
	}

	@Override
	public TIntSet getWordIndices() {
		return frequencies.keySet();
	}

	public void addWordOccurrence(int index) {
		addWordOccurrence(index, 1);
	}

	public void addWordOccurrence(int index, int times) {
		Integer f = frequencies.get(index);
		frequencies.put(index, f == null ? times : f + times);
		size += times;
	}
}
