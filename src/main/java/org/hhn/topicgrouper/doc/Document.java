package org.hhn.topicgrouper.doc;

import gnu.trove.set.TIntSet;

public interface Document<T> {
	public int getWords();
	
	public int getSize();
	
	public TIntSet getWordIndices();
	
	public int getWordFrequency(int index);
	
	public DocumentProvider<T> getProvider();
}
