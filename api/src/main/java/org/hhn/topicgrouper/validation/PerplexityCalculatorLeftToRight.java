package org.hhn.topicgrouper.validation;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;

/**
 * Perplexity calculator that estimates the document probability with the left-to-right
 * sampler (Wallach et al., "Evaluation Methods for Topic Models", 2009) instead of
 * using an explicit topic distribution of the test document.
 *
 * @param <T> the type of the words
 */
public class PerplexityCalculatorLeftToRight<T> extends
		BasicPerplexityCalculator<T> {
	/**
	 * Source of randomness for the sampler.
	 */
	protected final Random random;
	/**
	 * Number of particles used by the left-to-right sampler.
	 */
	protected final int particles;
	private LeftToRightSamplerBase<T> lrpSampler;

	/**
	 * Uses the default document splitter and 20 particles.
	 *
	 * @param random source of randomness
	 * @param bowFactor whether to include the bag-of-words factor in the log probability
	 */
	public PerplexityCalculatorLeftToRight(Random random, boolean bowFactor) {
		this(random, bowFactor, 20);
	}
	
	/**
	 * Uses the default document splitter.
	 *
	 * @param random source of randomness
	 * @param bowFactor whether to include the bag-of-words factor in the log probability
	 * @param particles number of particles of the sampler
	 */
	public PerplexityCalculatorLeftToRight(Random random, boolean bowFactor,
			int particles) {
		this(random, bowFactor, new DefaultDocumentSplitter<T>(), particles);
	}

	/**
	 * Creates a calculator; the sampler is created once the model is set with
	 * {@link #setTopicModeler(AbstractTopicModeler)}.
	 *
	 * @param random source of randomness
	 * @param bowFactor whether to include the bag-of-words factor in the log probability
	 * @param documentSplitter splits test documents into reference and test part
	 * @param particles number of particles of the sampler
	 */
	public PerplexityCalculatorLeftToRight(Random random, boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int particles) {
		super(bowFactor, documentSplitter, 1);
		this.random = random;
		this.particles = particles;
	}
	
	/**
	 * Whether the sequential (unbiased) sampler is used instead of the particle sampler.
	 *
	 * @return true by default
	 */
	protected boolean useUnbiased() {
		return true;
	}

	/**
	 * Whether the samplers restrict a word to the topics in which the model gives it a
	 * non-zero probability (default true). This does not change the distribution of
	 * the samples; it saves the work on the other topics, which matters for models in
	 * which words belong to few topics (Topic Grouper, also with word senses).
	 *
	 * @return whether the candidate topics of the words are precomputed
	 */
	protected boolean useSparseTopics() {
		return true;
	}

	/**
	 * The topics in which each word has a non-zero probability, or <code>null</code>
	 * if most entries of phi are non-zero (then the dense loops are cheaper).
	 *
	 * @param sampler the model
	 * @return per word its topics in ascending order, or <code>null</code>
	 */
	protected static <T> int[][] candidateTopics(AbstractTopicModeler<T> sampler) {
		int nTopics = sampler.getNTopics();
		int nWords = sampler.getNWords();
		int[][] candidates = new int[nWords][];
		long nonZero = 0;
		int[] buffer = new int[nTopics];
		for (int w = 0; w < nWords; w++) {
			int count = 0;
			for (int t = 0; t < nTopics; t++) {
				if (sampler.getPhi(t, w) > 0) {
					buffer[count++] = t;
				}
			}
			candidates[w] = Arrays.copyOf(buffer, count);
			nonZero += count;
		}
		return nonZero * 2 > (long) nTopics * nWords ? null : candidates;
	}

	@Override
	public void setTopicModeler(AbstractTopicModeler<T> topicModeler) {
		super.setTopicModeler(topicModeler);
		lrpSampler = createLeftToRightParticleSampler(topicModeler);
	}

	@Override
	protected double computeLogProbability(Document<T> refD, Document<T> d,
			AbstractTopicModeler<T> sampler) {
		return addBowFactor(lrpSampler.leftToRightDocCompletion(refD, d), d);
	}

	@Override
	protected void initPtd(Document<T> d, AbstractTopicModeler<T> sampler) {
	}

	@Override
	protected void updatePtd(Document<T> d, AbstractTopicModeler<T> sampler) {
	}

	/**
	 * Creates the left-to-right sampler that reads alpha, alpha sum and phi from the given modeler.
	 *
	 * @param sampler the topic modeler providing the model parameters
	 * @return a sequential sampler if {@link #useUnbiased()}, else a particle sampler
	 */
	protected LeftToRightSamplerBase<T> createLeftToRightParticleSampler(
			final AbstractTopicModeler<T> sampler) {
		final int[][] candidates = useSparseTopics() ? candidateTopics(sampler) : null;
		if (useUnbiased()) {
			return new LeftToRightSequentialSampler<T>(random, particles,
					sampler.getNTopics()) {
				@Override
				protected int[] getCandidateTopics(int wordIndex) {
					return candidates == null ? null : candidates[wordIndex];
				}

				@Override
				protected double getAlpha(int t) {
					return sampler.getAlpha(t);
				}

				@Override
				protected double getAlphaSum() {
					return sampler.getAlphaConc();
				}

				@Override
				protected double getPhi(int t, int wordIndex) {
					return sampler.getPhi(t, wordIndex);
				}
			};
		} else {
			return new LeftToRightParticleSampler<T>(random, particles,
					sampler.getNTopics()) {
				@Override
				protected int[] getCandidateTopics(int wordIndex) {
					return candidates == null ? null : candidates[wordIndex];
				}

				@Override
				protected double getAlpha(int t) {
					return sampler.getAlpha(t);
				}

				@Override
				protected double getAlphaSum() {
					return sampler.getAlphaConc();
				}

				@Override
				protected double getPhi(int t, int wordIndex) {
					return sampler.getPhi(t, wordIndex);
				}
			};
		}
	}
}
