package org.hhn.topicgrouper.lda.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class LDAGibbsSampler<T> {
	private final Random random;
	private final double[] alpha;
	private final double alphaSum;
	private final double beta;
	private final double betaSum;
	private final DocumentProvider<T> provider;

	private final int[][] documentTopicAssignmentCount;
	private final int[] documentSize;
	private final int[][] topicWordAssignmentCount;

	private final int[] topicFrCount;
	private final int[][][] documentWordOccurrenceLastTopicAssignment;
	private final List<Document<T>> documents;

	private double samplingRatios[];

	private double psi[][];

	// For "left to right".
	private final TIntList z;
	private final double[] pzn2;
	
	public LDAGibbsSampler(DocumentProvider<T> documentProvider, int topics,
			double alpha, double beta, Random random) {
		this(documentProvider, symmetricAlpha(alpha, topics), beta, random);
	}

	public LDAGibbsSampler(DocumentProvider<T> documentProvider,
			double[] alpha, double beta, Random random) {
		this.alpha = alpha;
		double aSum = 0;
		for (int i = 0; i < alpha.length; i++) {
			aSum += alpha[i];
		}
		this.alphaSum = aSum;
		this.beta = beta;
		this.betaSum = beta * documentProvider.getNumberOfWords();
		this.provider = documentProvider;
		this.random = random;
		documents = provider.getDocuments();
		documentTopicAssignmentCount = new int[documents.size()][alpha.length];
		documentSize = new int[documents.size()];
		topicWordAssignmentCount = new int[alpha.length][provider
				.getNumberOfWords()];
		psi = new double[alpha.length][provider.getNumberOfWords()];
		topicFrCount = new int[alpha.length];
		documentWordOccurrenceLastTopicAssignment = new int[documents.size()][][];
		int h = 0;
		for (Document<T> d : documents) {
			documentWordOccurrenceLastTopicAssignment[h] = new int[d.getWords()][];
			TIntIterator it = d.getWordIndices().iterator();
			int h2 = 0;
			while (it.hasNext()) {
				int wordIndex = it.next();
				int fr = d.getWordFrequency(wordIndex);
				documentWordOccurrenceLastTopicAssignment[h][h2] = new int[fr];
				h2++;
			}
			h++;
		}
		samplingRatios = new double[alpha.length];
		
		z = new TIntArrayList();
		pzn2 = new double[alpha.length];
	}

	public static double[] symmetricAlpha(double alpha, int topics) {
		double[] v = new double[topics];
		Arrays.fill(v, alpha);
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

		int all = burnIn + iterations;
		for (int i = 0; i < all; i++) {
			int h = 0;
			for (Document<T> d : documents) {
				TIntIterator it = d.getWordIndices().iterator();
				int h2 = 0;
				while (it.hasNext()) {
					int wordIndex = it.next();
					int fr = d.getWordFrequency(wordIndex);
					for (int j = 0; j < fr; j++) {
						int topic = documentWordOccurrenceLastTopicAssignment[h][h2][j];
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
						documentWordOccurrenceLastTopicAssignment[h][h2][j] = topic;
						documentTopicAssignmentCount[h][topic]++;
						topicWordAssignmentCount[topic][wordIndex]++;
						topicFrCount[topic]++;
					}
					h2++;
				}
				h++;
			}

			if (i > burnIn) {
				// Update psi by averaging over current counts.
				for (int k = 0; k < topicWordAssignmentCount.length; k++) {
					for (int j = 0; j < topicWordAssignmentCount[k].length; j++) {
						psi[k][j] += ((double) topicWordAssignmentCount[k][j])
								/ topicFrCount[k];
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
				psi[k][j] /= iterations;
			}
		}

		if (solutionListener != null) {
			solutionListener.done(this);
		}
	}

	public double getBeta(int topicIndex, int wordIndex) {
		return beta;
	}

	public double getBetaSum(int topicIndex) {
		return betaSum;
	}

	protected void afterSampling(int i, int numberOfIterations) {
	}

	protected void initialize(LDASolutionListener<T> listener) {
		for (int i = 0; i < psi.length; i++) {
			Arrays.fill(psi[i], 0);
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
		TIntIterator it = d.getWordIndices().iterator();
		int h2 = 0;
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			for (int j = 0; j < fr; j++) {
				int topic = nextDiscrete(alpha);
				documentWordOccurrenceLastTopicAssignment[index][h2][j] = topic;
				documentTopicAssignmentCount[index][topic]++;
				documentSize[index] = d.getSize();
				topicWordAssignmentCount[topic][wordIndex]++;
				topicFrCount[topic]++;
			}
			h2++;
		}
	}

	// Compute fold in according to "Equation Methods for Topic Models" (Wallach et al) equation 7
	public FoldInStore foldIn(int iterations, Document<T> d, FoldInStore store) {
		if (store == null) {
			store = new FoldInStore();
		}
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

	public double getAlpha(int i) {
		return alpha[i];
	}

	public double getAlphaSum() {
		return alphaSum;
	}

	public int getTopicWordAssignmentCount(int i, int j) {
		return topicWordAssignmentCount[i][j];
	}

	public DocumentProvider<T> getDocumentProvider() {
		return provider;
	}

	public int getDocumentTopicAssignmentCount(int i, int j) {
		return documentTopicAssignmentCount[i][j];
	}

	public int getNTopics() {
		return topicFrCount.length;
	}

	public int getDocumentSize(int i) {
		return documentSize[i];
	}

	public int getNDocuments() {
		return documentSize.length;
	}

	public int getNWords() {
		return provider.getNumberOfWords();
	}

	public class FoldInStore {
		private final int[] dTopicAssignmentCounts;
		private int[][] dWordOccurrenceLastTopicAssignments;
		private Document<T> d;

		public FoldInStore() {
			dTopicAssignmentCounts = new int[alpha.length];
		}

		public void initialize(Document<T> d) {
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

				T word = d.getProvider().getWord(wordIndex);
				int tIndex = provider.getIndex(word);
				// Ensure the word is in the training vocabulary.
				if (tIndex >= 0) {
					for (int j = 0; j < fr; j++) {
						int topic = nextDiscrete(alpha);
						dWordOccurrenceLastTopicAssignments[counter][j] = topic;
						dTopicAssignmentCounts[topic]++;
					}
				}
				counter++;
			}
		}

		public int[] getDTopicAssignmentCounts() {
			return dTopicAssignmentCounts;
		}

		public int[] nextFoldInPtdSample() {
			TIntIterator it2 = d.getWordIndices().iterator();
			int dSize = d.getSize();
			int counter = 0;
			while (it2.hasNext()) {
				int wordIndex = it2.next();
				int fr = d.getWordFrequency(wordIndex);

				T word = d.getProvider().getWord(wordIndex);
				int tIndex = provider.getIndex(word);
				// Ensure the word is in the training vocabulary.
				if (tIndex >= 0) {
					for (int j = 0; j < fr; j++) {
						int topic = dWordOccurrenceLastTopicAssignments[counter][j];
						dTopicAssignmentCounts[topic]--;
						for (int k = 0; k < alpha.length; k++) {
							samplingRatios[k] = psi[k][tIndex]
									* (dTopicAssignmentCounts[k] + alpha[k])
									/ (dSize + alphaSum);
						}
						topic = nextDiscrete(samplingRatios);
						dWordOccurrenceLastTopicAssignments[counter][j] = topic;
						dTopicAssignmentCounts[topic]++;
					}
				}
				counter++;
			}

			return dTopicAssignmentCounts;
		}
	}
	
	
	// According to Algorithm 3 from "Evaluation Methods for Topic Models" by Wallach et al.
	public double leftToRight(Document<T> d, int nParticles) {
		double l = 0;
		int n = 0;
		z.clear();
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			for (int i = 0; i < fr; i++) {
				double pn = 0;
				for (int r = 0; r < nParticles; r++) {
					for (int n2 = 0; n2 < n; n2++) {
						double[] pzn2 = computePzn2(wordIndex, n, n2, z);
						z.set(n2, nextDiscrete(pzn2)); // Line 6
					}
					double[] pzn = computePzn2(-1, n, -1, z);
					for (int t = 0; t < alpha.length; t++) {
						pn += psi[t][wordIndex] * pzn[t]; // Line 8 right (addends)
					}
					pzn = computePzn2(wordIndex, n, -1, z); // Line 9
					z.set(n, nextDiscrete(pzn));
				}
				pn = pn / nParticles;
				l += Math.log(pn);
				n++;
			}
		}

		return l;
	}
	
	// TODO: Line 6
	protected double[] computePzn2(int wordIndex, int n, int n2, TIntList z) {
		return null;
	}
}
