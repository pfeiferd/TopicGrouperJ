package org.hhn.topicgrouper.doc;

import java.util.List;

public interface DocumentProvider<T> {
	public int getNumberOfWords();
	public int getSize();
	public int getWordFrequency(int index);
	public T getWord(int index);
	public int getIndex(T word);
	public List<Document<T>> getDocuments();
}
