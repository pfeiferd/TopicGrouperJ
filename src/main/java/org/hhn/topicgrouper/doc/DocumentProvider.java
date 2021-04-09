package org.hhn.topicgrouper.doc;

import java.io.Serializable;
import java.util.List;

public interface DocumentProvider<T> extends Serializable {
	public int getSize();
	public int getWordFrequency(int index);
	public List<Document<T>> getDocuments();
	public Vocab<T> getVocab();
	
	public interface Vocab<T> extends Serializable {
		public T getWord(int index);
		public int getIndex(T word);		
		public int getNumberOfWords();
	}
}
