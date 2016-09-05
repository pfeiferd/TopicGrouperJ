package org.hhn.topicgrouper.tg;

import gnu.trove.TIntCollection;

public interface TGSolution<T> {
	public int getNumberOfTopics();
	
	public TIntCollection[] getTopics();
	
	public int[] getTopicIds();
		
	public int getTopicFrequency(int topicIndex);
	
	public int getTopicForWord(int wordIndex);
	
	public int getGlobalWordFrequency(int wordIndex);
		
	public double getTotalLikelhood();
	
	public T getWord(int wordIndex);
	
	public int getIndex(T word);
	
	public TIntCollection getHomonymns();
	
	public double[] getTopicLikelihoods();
}
