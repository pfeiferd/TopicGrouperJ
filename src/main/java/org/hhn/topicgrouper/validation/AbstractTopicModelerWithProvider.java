package org.hhn.topicgrouper.validation;

import java.io.Serializable;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;

public abstract class AbstractTopicModelerWithProvider<T> extends AbstractTopicModeler<T> implements Serializable {
	private static final long serialVersionUID = 4277347140213560841L;

	protected Random random;
	protected DocumentProvider<T> provider;

	protected AbstractTopicModelerWithProvider() {
		//no-args -> serialization constructor
	}

	public AbstractTopicModelerWithProvider(Random random, DocumentProvider<T> provider, int nTopics) {
		super(provider.getVocab(), nTopics);
		this.random = random;
		this.provider = provider;
	}
	
	@Override
	public double getWordProb(int wordIndex) {
		return ((double) provider.getWordFrequency(wordIndex)) / provider.getSize();
	}
}
