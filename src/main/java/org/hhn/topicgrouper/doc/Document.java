package org.hhn.topicgrouper.doc;

import gnu.trove.set.TIntSet;

import java.io.Serializable;

public interface Document<T> extends Serializable {
	public int getIndex();
	
	public int getWords();
	
	public int getSize();
	
	public TIntSet getWordIndices();
	
	public int getWordFrequency(int index);
	
	public DocumentProvider<T> getProvider();
}
