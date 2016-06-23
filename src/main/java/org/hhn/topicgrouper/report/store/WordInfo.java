package org.hhn.topicgrouper.report.store;

import java.io.Serializable;

public class WordInfo<T> implements Comparable<WordInfo<T>>,
		Serializable {
	private static final long serialVersionUID = 1L;
	
	private final int wordId;
	private final int frequency;
	private final T word;

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

	public T getWord() {
		return word;
	}

	public int getWordId() {
		return wordId;
	}

	public int getFrequency() {
		return frequency;
	}
	
	@Override
	public String toString() {
		return word + " (" + frequency + ") ";
	}
}