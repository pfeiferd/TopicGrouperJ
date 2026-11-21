package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;

/**
 * Computes the perplexity of a test corpus under a topic model. Each test document is split into
 * a reference part (used to estimate the document topic distribution) and a test part (whose
 * words are scored). Subclasses vary the estimation of the topic distribution p(t|d).
 *
 * @param <T> the type of the words
 */
public class BasicPerplexityCalculator<T> {
	/**
	 * Whether the bag-of-words (multinomial coefficient) factor is added to the log probability.
	 */
	protected final boolean bowFactor;
	/**
	 * Splits test documents into reference and test documents.
	 */
	protected final DocumentSplitter<T> documentSplitter;
	/**
	 * Log probabilities of the document for the individual samples.
	 */
	protected final double[] sValues;
	/**
	 * Current estimate of p(t|d) for the document being evaluated.
	 */
	protected double[] ptd;

	/**
	 * The model to be evaluated.
	 */
	protected AbstractTopicModeler<T> topicModeler;

	/**
	 * Uses the default document splitter and a single sample.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 */
	public BasicPerplexityCalculator(boolean bowFactor) {
		this(bowFactor, new DefaultDocumentSplitter<T>(), 1);
	}

	/**
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param documentSplitter splits test documents into reference and test part
	 * @param samplingMax number of samples of p(t|d) over which the document probability is averaged
	 */
	public BasicPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int samplingMax) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
		sValues = new double[samplingMax];
	}
	
	/**
	 * @return the model being evaluated
	 */
	public AbstractTopicModeler<T> getTopicModeler() {
		return topicModeler;
	}

	/**
	 * Sets the model to be evaluated and allocates the p(t|d) buffer.
	 *
	 * @param topicModeler the model
	 */
	public void setTopicModeler(AbstractTopicModeler<T> topicModeler) {
		this.topicModeler = topicModeler;
		ptd = new double[topicModeler.getNTopics()];
	}

	/**
	 * Computes exp(-logLikelihood / numberOfTestWords) over all test documents.
	 *
	 * @param testDocumentProvider the test corpus; must have the same vocabulary as the model
	 * @return the perplexity
	 * @throws IllegalStateException if the vocabularies differ
	 */
	public double computePerplexity(DocumentProvider<T> testDocumentProvider) {
		if (!testDocumentProvider.getVocab().equals(topicModeler.getVocab())) {
			throw new IllegalStateException(
					"training and test vocab not identical");
		}
		double sumA = 0;
		long sumB = 0;
		// int n = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			documentSplitter.setDocument(doc);
			int splits = documentSplitter.getSplits();
			for (int i = 0; i < splits; i++) {
				Split<T> nextSplit = documentSplitter.nextSplit();
				Document<T> rd = nextSplit.getRefDoc();
				Document<T> d = nextSplit.getTestDoc();
				double b = d.getSize();
				if (b != 0 && (rd == null || rd.getSize() != 0)) {
					double a = computeLogProbability(rd, d, topicModeler);
					// System.out.println(n++ + ", " + a + ", " + b );
					sumA += a; // computeLogProbability(rd, d, sampler);
					sumB += b; // d.getSize();
				}
			}
		}
		return Math.exp(-sumA / sumB);
	}

	/**
	 * Computes the log probability of the test document given the reference document, averaging
	 * over the samples in non-log space.
	 *
	 * @param refD the reference document (may be null)
	 * @param d the test document
	 * @param sampler the model
	 * @return the log probability
	 */
	protected double computeLogProbability(Document<T> refD, Document<T> d,
			AbstractTopicModeler<T> sampler) {
		// update ptd via reference document
		initPtd(refD, sampler);

		double sValuesAvg = 0;

		for (int i = 0; i < sValues.length; i++) {
			double sValue = 0;
			// update ptd via reference document
			updatePtd(refD, sampler);

			// Loop over d
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					sValue += wordFr
							* computeWordLogProbability(index, refD, sampler);
				}
			}

			sValues[i] = sValue;
			sValuesAvg += sValue;
		}

		double res;
		if (sValues.length > 1) {
			// Compute the average of sValue in the "non-log space" without
			// numeric problems by
			// factoring out a common prefactor of all sValues:
			sValuesAvg /= sValues.length;
			for (int i = 0; i < sValues.length; i++) {
				sValues[i] -= sValuesAvg;
			}
			double nonLogAvg = 0;
			for (int i = 0; i < sValues.length; i++) {
				nonLogAvg += Math.exp(sValues[i]);
			}
			nonLogAvg /= sValues.length;

			res = sValuesAvg + Math.log(nonLogAvg);
		} else {
			res = sValuesAvg;
		}

		if (bowFactor) {
			// Loop over d
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					res -= logFacN(wordFr);
				}
			}
			res += logFacN(d.getSize());
		}
		return res;
	}

	/**
	 * Hook called once per document before sampling p(t|d); does nothing by default.
	 *
	 * @param d the reference document
	 * @param sampler the model
	 */
	protected void initPtd(Document<T> d, AbstractTopicModeler<T> sampler) {
	}

	/**
	 * Sets p(t|d) for the next sample; by default to the global topic probabilities of the model.
	 *
	 * @param d the reference document
	 * @param sampler the model
	 */
	protected void updatePtd(Document<T> d, AbstractTopicModeler<T> sampler) {
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = sampler.getTopicProb(i);
		}
	}

	/**
	 * Log probability of a word under the current p(t|d), i.e. log of sum_t phi(t,w) * p(t|d).
	 *
	 * @param sIndex the word index
	 * @param refD the reference document
	 * @param sampler the model
	 * @return the log probability of the word
	 */
	protected double computeWordLogProbability(int sIndex, Document<T> refD,
			AbstractTopicModeler<T> sampler) {
		double sum = 0;
		for (int i = 0; i < sampler.getNTopics(); i++) {
			sum += sampler.getPhi(i, sIndex) * ptd[i];
		}

		return Math.log(sum);
	}

	/**
	 * @param n a non-negative number
	 * @return the natural logarithm of n factorial
	 */
	public static double logFacN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}

	/**
	 * Optimizes the alpha concentration of the model by minimizing the perplexity on the given
	 * documents, using a {@link PeakValueOptimizer}. The model is left with the last tested value.
	 *
	 * @param minLambda lower bound of the concentration
	 * @param maxLambda upper bound of the concentration
	 * @param provider documents on which the perplexity is evaluated
	 * @param steps number of search steps
	 */
	public void optimizeAlphaConc(double minLambda, double maxLambda,
			final DocumentProvider<T> provider, int steps) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				topicModeler.setAlphaConc(value);
				return -computePerplexity(provider);
			}
		};
		peakValueOptimizer.optimizeLambda(minLambda, maxLambda, steps);
	}
}
