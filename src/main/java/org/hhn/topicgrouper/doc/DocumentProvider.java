package org.hhn.topicgrouper.doc;

import java.util.List;

public interface DocumentProvider<T> {
	public int getSize();
	public int getWordFrequency(int index);
	public List<Document<T>> getDocuments();
	public Vocab<T> getVocab();
	
	public interface Vocab<T> {
		public T getWord(int index);
		public int getIndex(T word);		
		public int getNumberOfWords();
	}
}
