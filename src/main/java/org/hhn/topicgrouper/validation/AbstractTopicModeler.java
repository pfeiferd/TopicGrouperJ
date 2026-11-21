package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;

import java.io.Serializable;

/**
 * Common view of a topic model for validation: the topic-word distributions phi, the topic
 * probabilities and the Dirichlet prior over document topic distributions.
 *
 * @param <T> the type of the words
 */
public abstract class AbstractTopicModeler<T> implements Serializable {
	private static final long serialVersionUID = 8817316166524305524L;
	/**
	 * The vocabulary.
	 */
	protected Vocab<T> vocab;
	/**
	 * phi[t][w], the probability of word w in topic t.
	 */
	protected double phi[][];
	/**
	 * The overall probability of each topic.
	 */
	protected double topicProb[];
	/**
	 * The number of words in the vocabulary.
	 */
	protected int nWords;
	/**
	 * The number of topics.
	 */
	protected int nTopics;

	protected AbstractTopicModeler() {
		//no-args -> serialization constructor
	}

	/**
	 * Allocates phi and topicProb and calls {@link #initTopicProb()} and {@link #initPhi()}.
	 *
	 * @param vocab the vocabulary
	 * @param nTopics the number of topics
	 */
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
	
	
	/**
	 * @param wordIndex the word
	 * @return the overall probability of the word
	 */
	public abstract double getWordProb(int wordIndex);
		
	/**
	 * @return the vocabulary
	 */
	public Vocab<T> getVocab() {
		return vocab;
	}
	
	/**
	 * Hook to fill phi during construction; does nothing by default.
	 */
	protected void initPhi() {		
	}
	
	/**
	 * Hook to fill topicProb during construction; does nothing by default.
	 */
	protected void initTopicProb() {		
	}

	/**
	 * @return the number of topics
	 */
	public int getNTopics() {
		return nTopics;
	}
	
	/**
	 * @return the number of words in the vocabulary
	 */
	public int getNWords() {
		return nWords;
	}

	/**
	 * @param topicIndex the topic
	 * @param wordIndex the word
	 * @return the probability of the word in the topic
	 */
	public double getPhi(int topicIndex, int wordIndex) {
		return phi[topicIndex][wordIndex];
	}

	/**
	 * @param topicIndex the topic
	 * @return the overall probability of the topic
	 */
	public double getTopicProb(int topicIndex) {
		return topicProb[topicIndex];
	}
	
	/**
	 * @param i the topic
	 * @return the Dirichlet prior parameter of the topic
	 */
	public abstract double getAlpha(int i);

	/**
	 * @return the concentration (sum) of the Dirichlet prior over topics
	 */
	public abstract double getAlphaConc();
	
	/**
	 * Sets the concentration of the Dirichlet prior over topics.
	 *
	 * @param alphaConc the new concentration
	 */
	public abstract void setAlphaConc(double alphaConc);
}
