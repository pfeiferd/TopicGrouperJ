package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

public abstract class AbstractTopicModeler<T> {
	protected final Vocab<T> vocab;
	protected final double phi[][];
	protected final double topicProb[];
	protected final int nWords;
	protected final int nTopics;

	public AbstractTopicModeler(Vocab<T> vocab,
			int nTopics) {
		this.vocab = vocab;
		this.nTopics = nTopics;
		nWords = vocab.getNumberOfWords();
		topicProb = new double[nTopics];
		initTopicProb();
		phi = new double[nTopics][nWords];
		initPhi();
	}
	
	
	public abstract double getWordProb(int wordIndex);
		
	public Vocab<T> getVocab() {
		return vocab;
	}
	
	protected void initPhi() {		
	}
	
	protected void initTopicProb() {		
	}

	public int getNTopics() {
		return nTopics;
	}
	
	public int getNWords() {
		return nWords;
	}

	public double getPhi(int topicIndex, int wordIndex) {
		return phi[topicIndex][wordIndex];
	}

	public double getTopicProb(int topicIndex) {
		return topicProb[topicIndex];
	}
	
	public abstract double getAlpha(int i);

	public abstract double getAlphaConc();
	
	public abstract void setAlphaConc(double alphaConc);
}
