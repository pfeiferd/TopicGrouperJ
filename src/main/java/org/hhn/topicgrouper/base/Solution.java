package org.hhn.topicgrouper.base;

import gnu.trove.TIntCollection;

public interface Solution<T> {
	public int getNumberOfTopics();
	
	public TIntCollection[] getTopics();
		
	public int getTopicFrequency(int topicIndex);
	
	public int getTopicForWord(int wordIndex);
	
	public int getGlobalWordFrequency(int wordIndex);
		
	public double getTotalLikelhood();
	
	public T getWord(int wordIndex);
	
	public int getIndex(T word);
	
	public TIntCollection getHomonymns();
	
	public double[] getTopicLikelihoods();
}
