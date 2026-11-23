package org.hhn.topicgrouper.validation;

import java.io.Serializable;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * Topic modeler that is trained on the documents of a {@link DocumentProvider}
 * and holds a random number generator.
 *
 * @param <T> the type of the words
 */
public abstract class AbstractTopicModelerWithProvider<T> extends AbstractTopicModeler<T> implements Serializable {
	private static final long serialVersionUID = 4277347140213560841L;

	/**
	 * Source of randomness.
	 */
	protected Random random;
	/**
	 * The training documents.
	 */
	protected DocumentProvider<T> provider;

	/**
	 * No-arg constructor for deserialization; the fields are set by the serialization mechanism.
	 */
	protected AbstractTopicModelerWithProvider() {
		//no-args -> serialization constructor
	}

	/**
	 * Creates a model for the given documents.
	 *
	 * @param random source of randomness
	 * @param provider the training documents; its vocabulary is used
	 * @param nTopics the number of topics
	 */
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
