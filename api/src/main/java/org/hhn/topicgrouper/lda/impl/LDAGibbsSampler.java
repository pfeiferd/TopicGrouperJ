package org.hhn.topicgrouper.lda.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.validation.AbstractTopicModelerWithProvider;

/**
 * Latent Dirichlet Allocation trained by collapsed Gibbs sampling (Griffiths and Steyvers,
 * "Finding scientific topics", PNAS 2004). The sampler keeps the topic assignment of every
 * word occurrence and the count arrays derived from it: n_dk (words of document d assigned to
 * topic k, {@link #documentTopicAssignmentCount}), n_kw (occurrences of word w assigned to
 * topic k, {@link #topicWordAssignmentCount}) and n_k (all words assigned to topic k,
 * {@link #topicFrCount}). In each sweep every occurrence is reassigned with probability
 * proportional to (n_dk + alpha_k) * (n_kw + beta_kw) / (n_k + sum_w beta_kw), the counts
 * excluding the occurrence itself.
 * <p>
 * The estimate of phi is the average of (n_kw + beta_kw) / (n_k + sum_w beta_kw) over the
 * sampling iterations after burn-in; the topic probabilities are the averaged relative
 * values of n_k. During burn-in the hyper parameters can optionally be re-estimated every
 * {@link #getAlphaBetaUpdate()} iterations with the fixed point iteration of Minka,
 * "Estimating a Dirichlet distribution", 2000 (see {@link #setUpdateAlphaBeta(boolean)}).
 * The progress of alpha and beta can be logged to a stream set with
 * {@link #setAlphaBetaLog(PrintStream)}. Unseen documents can be folded in with
 * {@link #foldIn(int, Document)}.
 *
 * @param <T> the type of the words
 */
public class LDAGibbsSampler<T> extends AbstractTopicModelerWithProvider<T> implements Serializable {

	private static final long serialVersionUID = 4896261724252881347L;

	/**
	 * Dirichlet prior over the topics of a document.
	 */
	protected double[] alpha;
	/**
	 * Sum of alpha.
	 */
	protected double alphaSum;
	/**
	 * Symmetric Dirichlet prior over the words of a topic.
	 */
	protected double beta;
	/**
	 * Number of words times beta.
	 */
	protected double betaSum;

	/**
	 * Number of words per document and topic.
	 */
	protected int[][] documentTopicAssignmentCount;
	/**
	 * Number of words per document.
	 */
	protected int[] documentSize;
	/**
	 * Number of assignments per topic and word.
	 */
	protected int[][] topicWordAssignmentCount;

	/**
	 * Number of words assigned to each topic.
	 */
	protected int[] topicFrCount;
	/**
	 * Per document, per word index, the current topic of each occurrence of the word.
	 */
	protected TIntObjectMap<int[]>[] documentWordOccurrenceLastTopicAssignment;
	/**
	 * The training documents, in the order of the provider.
	 */
	private List<Document<T>> documents;

	/**
	 * Buffer for the unnormalized sampling probabilities of the topics.
	 */
	private double samplingRatios[];

	/**
	 * Whether alpha is re-estimated during burn-in.
	 */
	private boolean updateAlpha;
	/**
	 * Whether beta is re-estimated during burn-in.
	 */
	private boolean updateBeta;
	/**
	 * Interval (in iterations) of the alpha/beta updates and of the logging.
	 */
	private int alphaBetaUpdate;
	/**
	 * Number of fixed point iterations per alpha/beta update.
	 */
	private int minkasFixPointIterations;

	private transient PrintStream log;

	/**
	 * No-arg constructor for deserialization; the fields are set by the serialization mechanism.
	 */
	protected LDAGibbsSampler() {
		//no-args -> serialization constructor
	}

	/**
	 * Uses a symmetric alpha.
	 *
	 * @param random source of randomness
	 * @param documentProvider the training documents
	 * @param topics the number of topics
	 * @param alphaConc the sum of the symmetric alpha
	 * @param beta the symmetric beta
	 */
	public LDAGibbsSampler(Random random, DocumentProvider<T> documentProvider,
			int topics, double alphaConc, double beta) {
		this(random, documentProvider, symmetricAlpha(alphaConc, topics), beta);
	}

	/**
	 * Creates a sampler with an asymmetric alpha and allocates the count arrays; alpha and beta
	 * updates are disabled by default, with 10 fixed point iterations every 10 burn-in iterations once enabled.
	 *
	 * @param random source of randomness
	 * @param documentProvider the training documents
	 * @param alpha the Dirichlet prior over topics; its length is the number of topics
	 * @param beta the symmetric beta
	 */
	public LDAGibbsSampler(Random random, DocumentProvider<T> documentProvider,
			double[] alpha, double beta) {
		super(random, documentProvider, alpha.length);

		this.alpha = alpha;
		this.alphaSum = alphaSum();

		minkasFixPointIterations = 10;
		updateBeta = false;
		updateAlpha = false;
		alphaBetaUpdate = 10;

		this.beta = beta;
		this.betaSum = nWords * beta;

		documents = provider.getDocuments();
		documentTopicAssignmentCount = new int[documents.size()][alpha.length];
		documentSize = new int[documents.size()];
		topicWordAssignmentCount = new int[alpha.length][nWords];
		topicFrCount = new int[alpha.length];
		documentWordOccurrenceLastTopicAssignment = new TIntObjectMap[documentSize.length];
		int h = 0;
		for (Document<T> d : documents) {
			documentWordOccurrenceLastTopicAssignment[h] = new TIntObjectHashMap<int[]>();
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = d.getWordFrequency(wordIndex);
				documentWordOccurrenceLastTopicAssignment[h].put(wordIndex,
						new int[fr]);
			}
			h++;
		}
		samplingRatios = new double[alpha.length];
	}

	/**
	 * Sets the stream for logging the hyper parameters. The stream is not serialized.
	 *
	 * @param log stream to which the sums of alpha and beta are printed every
	 *            {@link #getAlphaBetaUpdate()} iterations, <code>null</code> (the default) for no logging
	 */
	public void setAlphaBetaLog(PrintStream log) {
		this.log = log;
	}

	/**
	 * The stream for logging the hyper parameters.
	 *
	 * @return the stream to which alpha and beta are logged, <code>null</code> if none
	 */
	public PrintStream getAlphaBetaLog() {
		return log;
	}

	/**
	 * The interval of the hyper parameter updates.
	 *
	 * @return the interval (in iterations) of the alpha/beta updates
	 */
	public int getAlphaBetaUpdate() {
		return alphaBetaUpdate;
	}

	/**
	 * Sets the interval of the hyper parameter updates (default 10).
	 *
	 * @param alphaBetaUpdate update alpha and beta every this many burn-in iterations
	 */
	public void setAlphaBetaUpdate(int alphaBetaUpdate) {
		this.alphaBetaUpdate = alphaBetaUpdate;
	}

	/**
	 * Sets how often Minka's fixed point update is iterated per hyper parameter update (default 10).
	 *
	 * @param minkasFixPointIterations number of fixed point iterations per update
	 */
	public void setMinkasFixPointIterations(int minkasFixPointIterations) {
		this.minkasFixPointIterations = minkasFixPointIterations;
	}

	/**
	 * How often Minka's fixed point update is iterated per hyper parameter update.
	 *
	 * @return the number of fixed point iterations per update
	 */
	public int getMinkasFixPointIterations() {
		return minkasFixPointIterations;
	}

	/**
	 * Whether alpha is re-estimated (default false).
	 *
	 * @return whether alpha is re-estimated during burn-in
	 */
	public boolean isUpdateAlpha() {
		return updateAlpha;
	}

	/**
	 * Whether beta is re-estimated (default false).
	 *
	 * @return whether beta is re-estimated during burn-in
	 */
	public boolean isUpdateBeta() {
		return updateBeta;
	}

	/**
	 * Enables or disables the re-estimation of both alpha and beta.
	 *
	 * @param updateAlphaBeta the flag
	 */
	public void setUpdateAlphaBeta(boolean updateAlphaBeta) {
		setUpdateAlpha(updateAlphaBeta);
		setUpdateBeta(updateAlphaBeta);
	}

	/**
	 * Enables or disables the re-estimation of alpha with Minka's fixed point update.
	 *
	 * @param updateAlpha whether alpha is re-estimated during burn-in
	 */
	public void setUpdateAlpha(boolean updateAlpha) {
		this.updateAlpha = updateAlpha;
	}

	/**
	 * Enables or disables the re-estimation of beta with Minka's fixed point update.
	 *
	 * @param updateBeta whether beta is re-estimated during burn-in
	 */
	public void setUpdateBeta(boolean updateBeta) {
		this.updateBeta = updateBeta;
	}

	/**
	 * A symmetric alpha vector with the given concentration (sum) spread over all topics.
	 *
	 * @param alphaConc the sum of the vector
	 * @param topics the number of topics, i.e. the length of the vector
	 * @return a new array with all entries equal to <code>alphaConc / topics</code>
	 */
	public static double[] symmetricAlpha(double alphaConc, int topics) {
		double[] v = new double[topics];
		double alphai = alphaConc / topics;
		Arrays.fill(v, alphai);
		return v;
	}

	/**
	 * Runs the sampler.
	 *
	 * @param burnIn number of initial iterations that are not used for the estimates
	 * @param iterations number of iterations over which phi and the topic probabilities are averaged
	 * @param solutionListener optional progress listener (may be null)
	 */
	public void solve(int burnIn, int iterations,
			LDASolutionListener<T> solutionListener) {
		if (solutionListener != null) {
			solutionListener.beforeInitialization(this);
		}
		initialize(solutionListener);
		if (solutionListener != null) {
			solutionListener.initialized(this);
		}
		int[] topicFrZeroCount = new int[topicFrCount.length];

		int all = burnIn + iterations;
		for (int i = 0; i < all; i++) {
			int h = 0;
			for (Document<T> d : documents) {
				TIntIterator it = d.getWordIndices().iterator();
				while (it.hasNext()) {
					int wordIndex = it.next();
					int fr = d.getWordFrequency(wordIndex);
					for (int j = 0; j < fr; j++) {
						int topic = documentWordOccurrenceLastTopicAssignment[h]
								.get(wordIndex)[j];
						documentTopicAssignmentCount[h][topic]--;
						topicWordAssignmentCount[topic][wordIndex]--;
						topicFrCount[topic]--;
						for (int k = 0; k < alpha.length; k++) {
							samplingRatios[k] = (documentTopicAssignmentCount[h][k] + alpha[k])
									* (topicWordAssignmentCount[k][wordIndex] + getBeta(
											k, wordIndex))
									/ (topicFrCount[k] + getBetaSum(k));
						}
						topic = nextDiscrete(samplingRatios);
						documentWordOccurrenceLastTopicAssignment[h]
								.get(wordIndex)[j] = topic;
						documentTopicAssignmentCount[h][topic]++;
						topicWordAssignmentCount[topic][wordIndex]++;
						topicFrCount[topic]++;
					}
				}
				h++;
			}

			if (i >= burnIn) {
				// Update phi by averaging the smoothed estimate
				// (n_kw + beta_kw) / (n_k + sum_w beta_kw) over the current counts.
				for (int k = 0; k < topicWordAssignmentCount.length; k++) {
					double denominator = topicFrCount[k] + getBetaSum(k);
					// Check to avoid division by zero if the topic is empty and beta is zero.
					if (denominator > 0) {
						for (int j = 0; j < topicWordAssignmentCount[k].length; j++) {
							phi[k][j] += (topicWordAssignmentCount[k][j] + getBeta(
									k, j)) / denominator;
						}
					} else {
						// Remember zero situations (once per iteration) for
						// division below in order to exclude the iteration
						// from the denominator.
						topicFrZeroCount[k]++;
					}
					topicProb[k] += topicFrCount[k];
				}
			} else if (i > 0 && i % alphaBetaUpdate == 0) {
				if (updateAlpha) {
					for (int j = 0; j < minkasFixPointIterations; j++) {
						updateAlpha();
					}
				}
				if (updateBeta) {
					for (int j = 0; j < minkasFixPointIterations; j++) {
						updateBeta();
					}
				}
			}

			afterSampling(i, iterations);
			if (solutionListener != null) {
				if (solutionListener != null) {
					solutionListener.updatedSolution(this, i);
				}
			}
		}
		for (int k = 0; k < topicWordAssignmentCount.length; k++) {
			int usedIterations = iterations - topicFrZeroCount[k];
			if (usedIterations > 0) {
				for (int j = 0; j < topicWordAssignmentCount[k].length; j++) {
					phi[k][j] /= usedIterations;
				}
			}
			topicProb[k] = topicProb[k] / iterations / provider.getSize();
		}

		if (solutionListener != null) {
			solutionListener.done(this);
		}
	}

	// protected void reportBeta() {
	// }

	/**
	 * Computes the sum of the current alpha vector.
	 *
	 * @return the sum of alpha
	 */
	protected double alphaSum() {
		double alphaSum = 0;
		for (int i = 0; i < alpha.length; i++) {
			alphaSum += alpha[i];
		}
		return alphaSum;
	}

	/**
	 * Re-estimates beta; the symmetric beta by default.
	 */
	protected void updateBeta() {
		updateSymmetricBeta();
	}

	/**
	 * The Dirichlet prior parameter beta_kw of a word in a topic; the symmetric beta for all words
	 * and topics in this class.
	 *
	 * @param topicIndex the topic
	 * @param wordIndex the word
	 * @return the Dirichlet prior parameter of the word in the topic
	 */
	public double getBeta(int topicIndex, int wordIndex) {
		return beta;
	}

	/**
	 * The sum of the beta values over all words of a topic; the number of words times beta in this class.
	 *
	 * @param topicIndex the topic
	 * @return the sum of the beta values of the topic
	 */
	public double getBetaSum(int topicIndex) {
		return betaSum;
	}

	/**
	 * Hook called after each sampling iteration; logs alpha and beta sums if a log stream is set.
	 *
	 * @param i the iteration
	 * @param numberOfIterations the number of averaging iterations
	 */
	protected void afterSampling(int i, int numberOfIterations) {
		// log is transient and thus null after deserialization.
		if (log != null && i % alphaBetaUpdate == 0) {
			log.println(i);
			log.println("betaSum: " + betaSum);
			log.println("alphaSum: " + alphaSum);
		}
	}

	/**
	 * Resets counts and estimates and randomly assigns topics to all words.
	 *
	 * @param listener optional listener, may be null
	 */
	protected void initialize(LDASolutionListener<T> listener) {
		for (int i = 0; i < phi.length; i++) {
			Arrays.fill(phi[i], 0);
		}
		Arrays.fill(topicProb, 0);
		Arrays.fill(topicFrCount, 0);
		for (int i = 0; i < topicWordAssignmentCount.length; i++) {
			Arrays.fill(topicWordAssignmentCount[i], 0);
		}
		int h = 0;
		for (Document<T> d : documents) {
			if (listener != null) {
				listener.initalizing(this, h);
			}
			initializeDocument(d, h);
			h++;
		}
	}

	/**
	 * Randomly assigns topics to the words of a document, drawn from alpha.
	 *
	 * @param d the document
	 * @param index the index of the document
	 */
	protected void initializeDocument(Document<T> d, int index) {
		Arrays.fill(documentTopicAssignmentCount[index], 0);
		documentSize[index] = d.getSize();
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			for (int j = 0; j < fr; j++) {
				int topic = nextDiscrete(alpha);
				documentWordOccurrenceLastTopicAssignment[index].get(wordIndex)[j] = topic;
				documentTopicAssignmentCount[index][topic]++;
				topicWordAssignmentCount[topic][wordIndex]++;
				topicFrCount[topic]++;
			}
		}
	}

	// Compute fold in according to "Equation Methods for Topic Models" (Wallach
	// et al) equation 7
	/**
	 * Folds in an unseen document: with phi fixed to the trained estimate, the topics of the
	 * document's words are Gibbs sampled from p(z = k) proportional to phi_kw * (n_dk + alpha_k)
	 * (Wallach et al., "Evaluation Methods for Topic Models", 2009, equation 7). The returned
	 * store holds the state after the given number of sweeps and can be sampled further.
	 *
	 * @param iterations number of Gibbs sweeps over the document (burn-in of the fold-in)
	 * @param d the document; must use the vocabulary of the training data
	 * @return the sampling state of the fold-in
	 * @throws IllegalStateException if the vocabulary of the document differs from the training vocabulary
	 */
	public FoldInStore foldIn(int iterations, Document<T> d) {
		FoldInStore store = new FoldInStore();
		store.initialize(d);
		for (int i = 0; i < iterations; i++) {
			store.nextFoldInPtdSample();
		}

		return store;
	}

	/**
	 * Draws an index from the unnormalized discrete distribution.
	 */
	private int nextDiscrete(double[] probs) {
		double sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
		}
		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r) {
				return i;
			}
		}
		return probs.length - 1;
	}

	/**
	 * The count n_k of the current sampling state.
	 *
	 * @param i the topic
	 * @return the number of words currently assigned to the topic
	 */
	public int getTopicFrCount(int i) {
		return topicFrCount[i];
	}

	@Override
	public double getAlpha(int i) {
		return alpha[i];
	}

	@Override
	public double getAlphaConc() {
		return alphaSum;
	}

	@Override
	public void setAlphaConc(double alphaConc) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The count n_kw of the current sampling state.
	 *
	 * @param i the topic
	 * @param j the word
	 * @return the number of times the word is currently assigned to the topic
	 */
	public int getTopicWordAssignmentCount(int i, int j) {
		return topicWordAssignmentCount[i][j];
	}

	/**
	 * The count n_dk of the current sampling state.
	 *
	 * @param i the document
	 * @param j the topic
	 * @return the number of words of the document currently assigned to the topic
	 */
	public int getDocumentTopicAssignmentCount(int i, int j) {
		return documentTopicAssignmentCount[i][j];
	}

	/**
	 * The size of a training document.
	 *
	 * @param i the document
	 * @return the number of words in the document
	 */
	public int getDocumentSize(int i) {
		return documentSize[i];
	}

	/**
	 * The number of training documents.
	 *
	 * @return the number of training documents
	 */
	public int getNDocuments() {
		return documentSize.length;
	}

	/**
	 * Performs one fixed point step of Minka, "Estimating a Dirichlet distribution", 2000,
	 * equation 55 on alpha ("Minka's update"): each alpha_k is multiplied by
	 * (sum_d digamma(n_dk + alpha_k) - D * digamma(alpha_k)) /
	 * (sum_d digamma(n_d + sum_k alpha_k) - D * digamma(sum_k alpha_k)), the documents being
	 * the samples of the Dirichlet. Infinite, NaN or negative results are discarded.
	 */
	protected void updateAlpha() {
		double sumDigammaNSumAlpha = 0;
		for (int i = 0; i < documentSize.length; i++) {
			sumDigammaNSumAlpha += Gamma.digamma(documentSize[i] + alphaSum);
		}
		double diff = sumDigammaNSumAlpha - documentSize.length
				* Gamma.digamma(alphaSum);
		for (int k = 0; k < alpha.length; k++) {
			double sumDigammaNiAlpha = 0;
			for (int i = 0; i < documentSize.length; i++) {
				sumDigammaNiAlpha += Gamma
						.digamma(documentTopicAssignmentCount[i][k] + alpha[k]);
			}
			double h = alpha[k] * (sumDigammaNiAlpha - documentSize.length
					* Gamma.digamma(alpha[k]))
					/ diff;
			// Avoid inconsistent updates
			if (!(Double.isInfinite(h) || Double.isNaN(h) || h < 0)) {
				alpha[k] = h;
			}
		}
		alphaSum = alphaSum();
	}

	/**
	 * Performs one fixed point step of Minka, "Estimating a Dirichlet distribution", 2000,
	 * equation 55 on the symmetric beta: beta is multiplied by
	 * (sum_k sum_w digamma(n_kw + beta) - K * W * digamma(beta)) /
	 * (W * (sum_k digamma(n_k + W * beta) - K * digamma(W * beta))), the topics being the
	 * samples of the Dirichlet over the W words.
	 */
	protected void updateSymmetricBeta() {
		double sumDigammaNSumBeta = 0;
		for (int i = 0; i < topicFrCount.length; i++) {
			sumDigammaNSumBeta += Gamma.digamma(topicFrCount[i] + betaSum);
		}
		double diff = nWords
				* (sumDigammaNSumBeta - topicFrCount.length
						* Gamma.digamma(betaSum));

		double sumDigammaNKBeta = 0;
		for (int k = 0; k < nWords; k++) {
			for (int i = 0; i < topicFrCount.length; i++) {
				sumDigammaNKBeta += Gamma
						.digamma(topicWordAssignmentCount[i][k] + beta);
			}
		}
		beta *= (sumDigammaNKBeta - nWords * topicFrCount.length
				* Gamma.digamma(beta))
				/ diff;
		betaSum = nWords * beta;
	}

	/**
	 * Holds the sampling state of the fold-in of one unseen document: the topic assignments of
	 * its words and their counts, with the trained phi fixed.
	 */
	public class FoldInStore {
		private final int[] dTopicAssignmentCounts;
		private int[][] dWordOccurrenceLastTopicAssignments;
		private Document<T> d;

		/**
		 * Creates an empty store; call {@link #initialize(Document)} before sampling.
		 */
		public FoldInStore() {
			dTopicAssignmentCounts = new int[alpha.length];
		}

		/**
		 * The sampler whose phi and alpha are used.
		 *
		 * @return the enclosing sampler
		 */
		public LDAGibbsSampler<T> getSampler() {
			return LDAGibbsSampler.this;
		}

		/**
		 * Randomly assigns topics to the words of the document.
		 *
		 * @param d the document, with the vocabulary of the training data
		 * @throws IllegalStateException if the vocabularies differ
		 */
		public void initialize(Document<T> d) {
			if (!d.getProvider().getVocab().equals(getVocab())) {
				throw new IllegalStateException(
						"training and test vocab not identical");
			}

			int counter = 0;
			this.d = d;
			Arrays.fill(dTopicAssignmentCounts, 0);
			dWordOccurrenceLastTopicAssignments = new int[d.getWordIndices()
					.size()][];
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = d.getWordFrequency(wordIndex);
				dWordOccurrenceLastTopicAssignments[counter] = new int[fr];
				counter++;
			}

			// Initialize topics randomly
			counter = 0;
			it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = d.getWordFrequency(wordIndex);
				for (int j = 0; j < fr; j++) {
					int topic = nextDiscrete(alpha);
					dWordOccurrenceLastTopicAssignments[counter][j] = topic;
					dTopicAssignmentCounts[topic]++;
				}
				counter++;
			}
		}

		/**
		 * The topic counts n_dk of the folded-in document.
		 *
		 * @return the number of words of the document currently assigned to each topic (live array)
		 */
		public int[] getDTopicAssignmentCounts() {
			return dTopicAssignmentCounts;
		}

		/**
		 * Performs one Gibbs sweep over the document words.
		 *
		 * @return the number of words of the document assigned to each topic (live array)
		 */
		public int[] nextFoldInPtdSample() {
			TIntIterator it2 = d.getWordIndices().iterator();
			int counter = 0;
			while (it2.hasNext()) {
				int wordIndex = it2.next();
				int fr = d.getWordFrequency(wordIndex);

				for (int j = 0; j < fr; j++) {
					int topic = dWordOccurrenceLastTopicAssignments[counter][j];
					dTopicAssignmentCounts[topic]--;
					for (int k = 0; k < alpha.length; k++) {
						samplingRatios[k] = phi[k][wordIndex]
								* (dTopicAssignmentCounts[k] + alpha[k]);
					}
					topic = nextDiscrete(samplingRatios);
					dWordOccurrenceLastTopicAssignments[counter][j] = topic;
					dTopicAssignmentCounts[topic]++;
				}
				counter++;
			}

			return dTopicAssignmentCounts;
		}
	}
}
