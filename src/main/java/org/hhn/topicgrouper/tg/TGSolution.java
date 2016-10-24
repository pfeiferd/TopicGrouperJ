package org.hhn.topicgrouper.tg;

import gnu.trove.TIntCollection;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

public interface TGSolution<T> {
	public int getNumberOfTopics();
	
	public TIntCollection[] getTopics();
	
	public int[] getTopicIds();
		
	public int getTopicFrequency(int topicIndex);
	
	public int getSize();
	
	public int getTopicForWord(int wordIndex);
	
	public int getGlobalWordFrequency(int wordIndex);
		
	public double getTotalLogLikelhood();
	
	public Vocab<T> getVocab();
		
	public TIntCollection getHomonymns();
	
	public double[] getTopicLogLikelihoods();
}
