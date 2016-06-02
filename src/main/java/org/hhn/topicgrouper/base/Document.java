package org.hhn.topicgrouper.base;

import gnu.trove.set.TIntSet;

public interface Document<T> {
	int getWords();
	
	int getSize();
	
	TIntSet getWordIndices();
	
	int getWordFrequency(int index);
	
	DocumentProvider<T> getProvider();
}
