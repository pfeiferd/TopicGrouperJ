package org.hhn.topicgrouper.tg.validation;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PerplexityCalculatorLeftToRight;

/**
 * TopicGrouper perplexity calculator that uses the left-to-right sampler.
 *
 * @param <T> the type of the words
 */
public class TGLRPerplexityCalculator<T> extends TGPerplexityCalculator<T> {
	private final Random random;

	/**
	 * @param random source of randomness
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param documentSplitter splits test documents into reference and test part
	 * @param alphaConc the concentration of the Dirichlet prior over topics
	 */
	public TGLRPerplexityCalculator(Random random,
			boolean bowFactor, DocumentSplitter<T> documentSplitter, 
			double alphaConc) {
		super(bowFactor, documentSplitter, alphaConc);
		this.random = random;
	}
	
	/**
	 * @return the number of particles (20)
	 */
	protected int initParticles() {
		return 20;
	}

	/**
	 * @return a left-to-right perplexity calculator
	 */
	protected BasicPerplexityCalculator<T> createBasicPerplexityCalculator() {
		return new PerplexityCalculatorLeftToRight<T>(random, bowFactor, initParticles());
	}
}
