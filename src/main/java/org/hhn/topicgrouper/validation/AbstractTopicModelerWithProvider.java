package org.hhn.topicgrouper.validation;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;

public abstract class AbstractTopicModelerWithProvider<T> extends AbstractTopicModeler<T> {
	protected final Random random;
	protected final DocumentProvider<T> provider;

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
