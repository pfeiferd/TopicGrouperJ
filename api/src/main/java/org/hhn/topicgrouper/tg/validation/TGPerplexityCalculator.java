package org.hhn.topicgrouper.tg.validation;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PeakValueOptimizer;

/**
 * Computes the perplexity of a {@link TGSolution} on test documents. The solution is turned into
 * a topic model in which every word belongs to exactly one topic; p(t|d) is smoothed between the
 * relative topic frequency in the reference document and the global topic probability.
 *
 * @param <T> the type of the words
 */
public class TGPerplexityCalculator<T> {
	/**
	 * Whether the bag-of-words factor is included.
	 */
	protected final boolean bowFactor;
	/**
	 * Splits test documents into reference and test part.
	 */
	protected final DocumentSplitter<T> documentSplitter;
	/**
	 * The concentration of the Dirichlet prior over topics.
	 */
	protected double alphaConc;

	private double smoothingLambda = 0.5;

	private int[] topicIds;
	private TIntCollection[] topics;
	// Not null if the current solution has word senses.
	private TGSenseSolution<T> senseSolution;

	private BasicPerplexityCalculator<T> perplexityCalculator;

	/**
	 * Without bag-of-words factor, default splitter and alpha concentration 1.
	 */
	public TGPerplexityCalculator() {
		this(false, new DefaultDocumentSplitter<T>(), 1);
	}

	/**
	 * Creates a calculator; {@link #init()} must be called before use.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param documentSplitter splits test documents into reference and test part
	 * @param alphaConc the concentration of the Dirichlet prior over topics
	 */
	public TGPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, double alphaConc) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
		this.alphaConc = alphaConc;
	}

	/**
	 * Creates the underlying perplexity calculator; must be called before use.
	 */
	public void init() {
		perplexityCalculator = createBasicPerplexityCalculator();
	}

	/**
	 * The calculator created by {@link #init()}.
	 *
	 * @return the underlying perplexity calculator
	 */
	public BasicPerplexityCalculator<T> getPerplexityCalculator() {
		return perplexityCalculator;
	}

	/**
	 * The current alpha concentration (changed by {@link #optimizeAlphaConc(double, double, DocumentProvider, int)}).
	 *
	 * @return the concentration of the Dirichlet prior over topics
	 */
	public double getAlphaConc() {
		return alphaConc;
	}

	/**
	 * Sets the alpha concentration.
	 *
	 * @param alphaConc the concentration of the Dirichlet prior over topics
	 */
	public void setAlphaConc(double alphaConc) {
		this.alphaConc = alphaConc;
	}

	/**
	 * Makes the given solution the model to be evaluated.
	 *
	 * @param s the solution
	 */
	public void setSolution(TGSolution<T> s) {
		perplexityCalculator.setTopicModeler(createTopicModeler(s));
	}

	/**
	 * Sets the alpha concentration to the value in the interval that minimizes the perplexity.
	 *
	 * @param minLambda lower bound
	 * @param maxLambda upper bound
	 * @param provider documents on which the perplexity is evaluated
	 * @param steps number of search steps
	 */
	public void optimizeAlphaConc(double minLambda, double maxLambda,
			final DocumentProvider<T> provider, int steps) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				alphaConc = value;
				return -perplexityCalculator.computePerplexity(provider);
			}
		};
		alphaConc = peakValueOptimizer.optimizeLambda(minLambda, maxLambda,
				steps);
	}

	/**
	 * Sets the smoothing weight of the global topic probability (the lambda in
	 * {@link #smoothedPtd(double, int, double)}) to the value in the interval that minimizes the perplexity.
	 *
	 * @param minLambda lower bound
	 * @param maxLambda upper bound
	 * @param provider documents on which the perplexity is evaluated
	 * @param steps number of search steps
	 */
	public void optimizeLambda(double minLambda, double maxLambda,
			final DocumentProvider<T> provider, int steps) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				smoothingLambda = value;
				// The optimizer searches for a maximum, perplexity is to be minimized.
				return -perplexityCalculator.computePerplexity(provider);
			}
		};
		smoothingLambda = peakValueOptimizer.optimizeLambda(minLambda,
				maxLambda, steps);
	}

	/**
	 * Evaluates the current solution.
	 *
	 * @param testDocumentProvider the test documents
	 * @return the perplexity of the current solution on the test documents
	 */
	public double computePerplexity(DocumentProvider<T> testDocumentProvider) {
		return perplexityCalculator.computePerplexity(testDocumentProvider);
	}

	/**
	 * Creates a topic model from the solution: phi(t,w) is the relative frequency of w within
	 * its topic, zero for other words.
	 *
	 * @param s the solution
	 * @return the topic model
	 */
	public AbstractTopicModeler<T> createTopicModeler(final TGSolution<T> s) {
		topicIds = s.getTopicIds();
		topics = s.getTopics();
		senseSolution = s instanceof TGSenseSolution ? (TGSenseSolution<T>) s : null;
		final TGSenseSolution<T> senses = senseSolution;

		AbstractTopicModeler<T> topicModeler = new AbstractTopicModeler<T>(
				s.getVocab(), s.getNumberOfTopics()) {
			@Override
			protected void initPhi() {
				if (senses != null) {
					// A word may occur in several topics via its senses.
					for (int i = 0; i < phi.length; i++) {
						double tf = s.getTopicFrequency(topicIds[i]);
						TIntIterator types = s.getTopics()[topicIds[i]].iterator();
						while (types.hasNext()) {
							int type = types.next();
							phi[i][senses.getOriginalWord(type)] += s.getGlobalWordFrequency(type) / tf;
						}
					}
					return;
				}
				for (int i = 0; i < phi.length; i++) {
					double tf = s.getTopicFrequency(topicIds[i]);
					for (int j = 0; j < phi[i].length; j++) {
						if (s.getTopicForWord(j) == topicIds[i]) {
							phi[i][j] = s.getGlobalWordFrequency(j) / tf;
						}
					}
				}
			}

			@Override
			protected void initTopicProb() {
				for (int i = 0; i < topicProb.length; i++) {
					topicProb[i] = ((double) s.getTopicFrequency(topicIds[i]))
							/ s.getSize();
				}
			}

			@Override
			public double getWordProb(int wordIndex) {
				return ((double) s.getGlobalWordFrequency(wordIndex))
						/ s.getSize();
			}

			@Override
			public double getAlpha(int i) {
				return topicProb[i] * alphaConc;
			}

			@Override
			public double getAlphaConc() {
				return alphaConc;
			}

			@Override
			public void setAlphaConc(double alphaConc) {
				TGPerplexityCalculator.this.alphaConc = alphaConc;
			}
		};

		return topicModeler;
	}

	/**
	 * Creates a topic model from the solution in which phi(t,w) is derived from the topic-word
	 * log-likelihood contributions of the solution, so words may have non-zero probability in
	 * several topics.
	 *
	 * @param s the solution, must be a {@code AbstractTopicGrouper.DefaultTGSolution}
	 * @return the topic model
	 */
	public AbstractTopicModeler<T> createSmoothedTopicModeler(
			final TGSolution<T> s) {
		topicIds = s.getTopicIds();
		topics = s.getTopics();

		final AbstractTopicGrouper<T>.DefaultTGSolution defaultSolution = (AbstractTopicGrouper<T>.DefaultTGSolution) s;

		AbstractTopicModeler<T> topicModeler = new AbstractTopicModeler<T>(
				s.getVocab(), s.getNumberOfTopics()) {
			@Override
			protected void initPhi() {
				double[] wSum = new double[nWords];

				for (int i = 0; i < phi.length; i++) {
					for (int j = 0; j < phi[i].length; j++) {
						double deltaH = defaultSolution
								.computeTopicWordLogLikelihood(topicIds[i], j);
						phi[i][j] = deltaH;
						wSum[j] += deltaH;
					}
				}
				for (int i = 0; i < phi.length; i++) {
					topicProb[i] = 0;
					double sum = 0;
					for (int j = 0; j < phi[i].length; j++) {
						int wordFr = s.getGlobalWordFrequency(j);
						topicProb[i] += wordFr * phi[i][j] / wSum[j] / s.getSize();
						phi[i][j] *= wordFr;
						sum += phi[i][j];
					}
					for (int j = 0; j < phi[i].length; j++) {
						phi[i][j] = phi[i][j] / sum;
					}
				}
			}

			@Override
			protected void initTopicProb() {
				// Nothing to do, all done in initPhi()...
			}

			@Override
			public double getWordProb(int wordIndex) {
				return ((double) s.getGlobalWordFrequency(wordIndex))
						/ s.getSize();
			}

			@Override
			public double getAlpha(int i) {
				return topicProb[i] * alphaConc;
			}

			@Override
			public double getAlphaConc() {
				return alphaConc;
			}

			@Override
			public void setAlphaConc(double alphaConc) {
				TGPerplexityCalculator.this.alphaConc = alphaConc;
			}
		};

		return topicModeler;
	}

	/**
	 * Creates the underlying calculator (called by {@link #init()}).
	 *
	 * @return a perplexity calculator that smooths p(t|d) with the topic frequencies in the reference document
	 */
	protected BasicPerplexityCalculator<T> createBasicPerplexityCalculator() {
		return new BasicPerplexityCalculator<T>(bowFactor, documentSplitter, 1) {
			@Override
			protected void updatePtd(Document<T> d,
					AbstractTopicModeler<T> sampler) {
				super.updatePtd(d, sampler);
				if (d != null && smoothingLambda < 1) {
					for (int i = 0; i < ptd.length; i++) {
						double topicFrInDoc = 0;
						TIntIterator it = topics[topicIds[i]].iterator();
						while (it.hasNext()) {
							int type = it.next();
							if (senseSolution == null) {
								topicFrInDoc += d.getWordFrequency(type);
							} else {
								// The sense of an occurrence in a new document is unknown: split it by sense share.
								topicFrInDoc += d.getWordFrequency(senseSolution.getOriginalWord(type))
										* senseSolution.getSenseShare(type);
							}
						}
						ptd[i] = smoothedPtd(topicFrInDoc, d.getSize(), ptd[i]);
					}
				}
			}
		};
	}

	/**
	 * Mixes the topic frequency in the document with the model topic probability.
	 *
	 * @param topicFrInDoc the number of words of the topic in the document
	 * @param docSize the size of the document
	 * @param ptd the unsmoothed topic probability
	 * @return (1 - lambda) * topicFrInDoc / docSize + lambda * ptd
	 */
	protected double smoothedPtd(double topicFrInDoc, int docSize, double ptd) {
		double h = (((1 - smoothingLambda) * topicFrInDoc) / docSize)
		// Smoothing via global frequency of topic;
				+ smoothingLambda * ptd;
		if (Double.isNaN(h)) {
			throw new IllegalStateException();
		}
		return h;
	}

	/**
	 * The smoothing weight (independent of the solution in this implementation).
	 *
	 * @param s the solution
	 * @return the weight of the model topic probability when smoothing
	 */
	protected double getSmoothingLambda(TGSolution<T> s) {
		return smoothingLambda;
	}
}
