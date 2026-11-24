package org.hhn.topicgrouper.tg.validation;

import gnu.trove.iterator.TIntIterator;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;

/**
 * Computes the perplexity of a {@link TGSolution} exactly, in closed form. Under
 * Topic Grouper every word belongs to exactly one topic, so the topic of each
 * occurrence of a test document is known and the left-to-right estimator of
 * {@link TGLRPerplexityCalculator} is exact; its resampling step is a point
 * mass and its particles are identical. The probability of a test document
 * <code>d</code> given the Dirichlet prior <code>alpha</code> on the topic
 * proportions is therefore
 *
 * <pre>
 * p(d) = prod_w phi(t(w), w)^f_d(w) * DCM(c_d; alpha)
 * </pre>
 *
 * where <code>c_d</code> are the topic counts of the document and
 * <code>DCM</code> the Dirichlet-multinomial likelihood of a count vector,
 * <code>Gamma(sum alpha) / Gamma(|d| + sum alpha) * prod_t Gamma(c_t + alpha_t) / Gamma(alpha_t)</code>
 * (word order does not matter). With a reference document (document
 * completion) the counts of the reference are added to the prior. This
 * calculator gives the same numbers as {@link TGLRPerplexityCalculator} (up to
 * rounding) at a cost linear instead of quadratic in the document length and
 * independent of the number of topics and particles.
 * <p>
 * Solutions with word senses ({@link TGSenseSolution}) are refused: the topic
 * model handed to the estimator sums over the senses of a word, so a word may
 * have several topics and the closed form does not apply.
 *
 * @param <T> the type of the words
 */
public class TGExactPerplexityCalculator<T> extends TGPerplexityCalculator<T> {
	/** topicPos[w]: the position of the topic of word w in the topic model, -1 if the word has no topic. */
	private int[] topicPos;

	/**
	 * Without bag-of-words factor, default splitter and alpha concentration 1.
	 */
	public TGExactPerplexityCalculator() {
		this(false, new DefaultDocumentSplitter<T>(), 1);
	}

	/**
	 * Creates a calculator; {@link #init()} must be called before use.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param documentSplitter splits test documents into reference and test part
	 * @param alphaConc the concentration of the Dirichlet prior over topics
	 */
	public TGExactPerplexityCalculator(boolean bowFactor, DocumentSplitter<T> documentSplitter,
			double alphaConc) {
		super(bowFactor, documentSplitter, alphaConc);
	}

	/**
	 * Makes the given solution the model to be evaluated.
	 *
	 * @param s the solution, without word senses
	 * @throws IllegalArgumentException if the solution has word senses
	 */
	@Override
	public void setSolution(TGSolution<T> s) {
		if (s instanceof TGSenseSolution) {
			throw new IllegalArgumentException(
					"the closed form needs one topic per word; use TGLRPerplexityCalculator for solutions with word senses");
		}
		super.setSolution(s);
		int[] topicIds = s.getTopicIds();
		topicPos = new int[s.getVocab().getNumberOfWords()];
		java.util.Arrays.fill(topicPos, -1);
		for (int i = 0; i < topicIds.length; i++) {
			TIntIterator it = s.getTopics()[topicIds[i]].iterator();
			while (it.hasNext()) {
				topicPos[it.next()] = i;
			}
		}
	}

	/**
	 * Creates the underlying calculator (called by {@link #init()}).
	 *
	 * @return a perplexity calculator that computes the document probabilities in closed form
	 */
	@Override
	protected BasicPerplexityCalculator<T> createBasicPerplexityCalculator() {
		return new BasicPerplexityCalculator<T>(bowFactor, documentSplitter, 1) {
			private double[] counts = new double[0];
			private double[] refCounts = new double[0];

			@Override
			protected double computeLogProbability(Document<T> refD, Document<T> d,
					AbstractTopicModeler<T> sampler) {
				int k = sampler.getNTopics();
				if (counts.length != k) {
					counts = new double[k];
					refCounts = new double[k];
				} else {
					java.util.Arrays.fill(counts, 0);
					java.util.Arrays.fill(refCounts, 0);
				}
				double alphaSum = 0;
				for (int i = 0; i < k; i++) {
					alphaSum += sampler.getAlpha(i);
				}
				int refSize = refD == null ? 0 : countTopics(refD, refCounts);

				// prod_w phi(t(w), w)^f_d(w)
				double logP = 0;
				TIntIterator it = d.getWordIndices().iterator();
				while (it.hasNext()) {
					int w = it.next();
					int f = d.getWordFrequency(w);
					if (f > 0) {
						int t = topicPos[w];
						logP += f * Math.log(t < 0 ? 0 : sampler.getPhi(t, w));
					}
				}
				int size = countTopics(d, counts);

				// DCM(c_ref + c_d; alpha) / DCM(c_ref; alpha)
				for (int i = 0; i < k; i++) {
					if (counts[i] > 0) {
						double prior = refCounts[i] + sampler.getAlpha(i);
						logP += Gamma.logGamma(counts[i] + prior) - Gamma.logGamma(prior);
					}
				}
				logP += Gamma.logGamma(refSize + alphaSum) - Gamma.logGamma(refSize + size + alphaSum);
				return addBowFactor(logP, d);
			}

			/**
			 * Adds the topic counts of a document to <code>counts</code>.
			 *
			 * @return the size of the document (occurrences of words with a topic)
			 */
			private int countTopics(Document<T> doc, double[] counts) {
				int size = 0;
				TIntIterator it = doc.getWordIndices().iterator();
				while (it.hasNext()) {
					int w = it.next();
					int f = doc.getWordFrequency(w);
					int t = topicPos[w];
					if (f > 0 && t >= 0) {
						counts[t] += f;
						size += f;
					}
				}
				return size;
			}
		};
	}
}
