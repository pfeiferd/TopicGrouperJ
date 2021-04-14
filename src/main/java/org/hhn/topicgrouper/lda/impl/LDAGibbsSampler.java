package org.hhn.topicgrouper.lda.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.validation.AbstractTopicModelerWithProvider;

public class LDAGibbsSampler<T> extends AbstractTopicModelerWithProvider<T> implements Serializable {

	private static final long serialVersionUID = 4896261724252881347L;

	protected double[] alpha;
	protected double alphaSum;
	protected double beta;
	protected double betaSum;

	protected int[][] documentTopicAssignmentCount;
	protected int[] documentSize;
	protected int[][] topicWordAssignmentCount;

	protected int[] topicFrCount;
	protected TIntObjectMap<int[]>[] documentWordOccurrenceLastTopicAssignment;
	private List<Document<T>> documents;

	private double samplingRatios[];

	private boolean updateAlpha;
	private boolean updateBeta;
	private int alphaBetaUpdate;
	private int minkasFixPointIterations;

	private transient PrintStream log;

	protected LDAGibbsSampler() {
		//no-args -> serialization constructor
	}

	public LDAGibbsSampler(Random random, DocumentProvider<T> documentProvider,
			int topics, double alphaConc, double beta) {
		this(random, documentProvider, symmetricAlpha(alphaConc, topics), beta);
	}

	public LDAGibbsSampler(Random random, DocumentProvider<T> documentProvider,
			double[] alpha, double beta) {
		super(random, documentProvider, alpha.length);

		try {
			log = new PrintStream(new FileOutputStream(new File(
					"alphabetalog.log")));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

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

	public int getAlphaBetaUpdate() {
		return alphaBetaUpdate;
	}

	public void setAlphaBetaUpdate(int alphaBetaUpdate) {
		this.alphaBetaUpdate = alphaBetaUpdate;
	}

	public void setMinkasFixPointIterations(int minkasFixPointIterations) {
		this.minkasFixPointIterations = minkasFixPointIterations;
	}

	public int getMinkasFixPointIterations() {
		return minkasFixPointIterations;
	}

	public boolean isUpdateAlpha() {
		return updateAlpha;
	}

	public boolean isUpdateBeta() {
		return updateBeta;
	}

	public void setUpdateAlphaBeta(boolean updateAlphaBeta) {
		setUpdateAlpha(updateAlphaBeta);
		setUpdateBeta(updateAlphaBeta);
	}

	public void setUpdateAlpha(boolean updateAlpha) {
		this.updateAlpha = updateAlpha;
	}

	public void setUpdateBeta(boolean updateBeta) {
		this.updateBeta = updateBeta;
	}

	public static double[] symmetricAlpha(double alphaConc, int topics) {
		double[] v = new double[topics];
		double alphai = alphaConc / topics;
		Arrays.fill(v, alphai);
		return v;
	}

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
				// Update phi by averaging over current counts.
				for (int k = 0; k < topicWordAssignmentCount.length; k++) {
					for (int j = 0; j < topicWordAssignmentCount[k].length; j++) {
						// Check to avoid division by zero if topicFrCount[k] is
						// zero
						if (topicFrCount[k] != 0) {
							phi[k][j] += ((double) topicWordAssignmentCount[k][j])
									/ topicFrCount[k];
						} else {
							// Remember zero situations for division below (in
							// order to exclude the value from denominator.
							topicFrZeroCount[k]++;
						}
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
			for (int j = 0; j < topicWordAssignmentCount[k].length; j++) {
				phi[k][j] /= (iterations - topicFrZeroCount[k]);
			}
			topicProb[k] = topicProb[k] / iterations / provider.getSize();
		}

		if (solutionListener != null) {
			solutionListener.done(this);
		}
	}

	// protected void reportBeta() {
	// }

	protected double alphaSum() {
		double alphaSum = 0;
		for (int i = 0; i < alpha.length; i++) {
			alphaSum += alpha[i];
		}
		return alphaSum;
	}

	protected void updateBeta() {
		updateSymmetricBeta();
	}

	public double getBeta(int topicIndex, int wordIndex) {
		return beta;
	}

	public double getBetaSum(int topicIndex) {
		return betaSum;
	}

	protected void afterSampling(int i, int numberOfIterations) {
		if (i % alphaBetaUpdate == 0) {
			log.println(i);
			log.println("betaSum: " + betaSum);
			log.println("alphaSum: " + alphaSum);
		}
	}

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

	protected void initializeDocument(Document<T> d, int index) {
		Arrays.fill(documentTopicAssignmentCount[index], 0);
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			for (int j = 0; j < fr; j++) {
				int topic = nextDiscrete(alpha);
				documentWordOccurrenceLastTopicAssignment[index].get(wordIndex)[j] = topic;
				documentTopicAssignmentCount[index][topic]++;
				documentSize[index] = d.getSize();
				topicWordAssignmentCount[topic][wordIndex]++;
				topicFrCount[topic]++;
			}
		}
	}

	// Compute fold in according to "Equation Methods for Topic Models" (Wallach
	// et al) equation 7
	public FoldInStore foldIn(int iterations, Document<T> d) {
		FoldInStore store = new FoldInStore();
		store.initialize(d);
		for (int i = 0; i < iterations; i++) {
			store.nextFoldInPtdSample();
		}

		return store;
	}

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

	public int getTopicWordAssignmentCount(int i, int j) {
		return topicWordAssignmentCount[i][j];
	}

	public int getDocumentTopicAssignmentCount(int i, int j) {
		return documentTopicAssignmentCount[i][j];
	}

	public int getDocumentSize(int i) {
		return documentSize[i];
	}

	public int getNDocuments() {
		return documentSize.length;
	}
	
	/*
	 * Update on the basis of equation 55 from
	 * "Estimating a Dirichlet distribution by Thomas P. Minka" - also known as
	 * "Minka's Update"
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
			if (Double.isInfinite(h) || Double.isNaN(h) || h < 0) {
//				System.out.println("stop");				
			}
			else {
				alpha[k] = h;
			}
		}
		alphaSum = alphaSum();
	}

	/*
	 * Update on the basis of equation 55 from
	 * "Estimating a Dirichlet distribution by Thomas P. Minka" - also known as
	 * "Minka's Update"
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

	public class FoldInStore {
		private final int[] dTopicAssignmentCounts;
		private int[][] dWordOccurrenceLastTopicAssignments;
		private Document<T> d;

		public FoldInStore() {
			dTopicAssignmentCounts = new int[alpha.length];
		}

		public LDAGibbsSampler<T> getSampler() {
			return LDAGibbsSampler.this;
		}

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

		public int[] getDTopicAssignmentCounts() {
			return dTopicAssignmentCounts;
		}

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
