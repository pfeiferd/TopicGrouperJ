package org.hhn.topicgrouper.lda.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class LDAGibbsSampler<T> {
	private final Random random;
	private final double[] alpha;
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

	public LDAGibbsSampler(DocumentProvider<T> documentProvider, int topics,
			double alpha, double beta, Random random) {
		this(documentProvider, symmetricAlpha(alpha, topics), beta, random);
	}

	public LDAGibbsSampler(DocumentProvider<T> documentProvider,
			double[] alpha, double beta, Random random) {
		this.alpha = alpha;
		this.beta = beta;
		this.betaSum = beta * documentProvider.getNumberOfWords();
		this.provider = documentProvider;
		this.random = random;
		documents = provider.getDocuments();
		documentTopicAssignmentCount = new int[documents.size()][alpha.length];
		documentSize = new int[documents.size()];
		topicWordAssignmentCount = new int[alpha.length][provider
				.getNumberOfWords()];
		topicFrCount = new int[alpha.length];
		documentWordOccurrenceLastTopicAssignment = new int[documents.size()][][];
		int h = 0;
		for (Document<T> d : documents) {
			documentWordOccurrenceLastTopicAssignment[h] = new int[d
					.getWordIndices().size()][];
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
	}

	public static double[] symmetricAlpha(double alpha, int topics) {
		double[] v = new double[topics];
		Arrays.fill(v, alpha);
		return v;
	}

	public void solve(int iterations, LDASolutionListener<T> solutionListener) {
		if (solutionListener != null) {
			solutionListener.beforeInitialization(this);
		}
		initialize(solutionListener);
		if (solutionListener != null) {
			solutionListener.initialized(this);
		}

		for (int i = 0; i < iterations; i++) {
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
									* (topicWordAssignmentCount[k][wordIndex] + getBeta(k, wordIndex))
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
			afterSampling(i, iterations);
			if (solutionListener != null) {
				if (solutionListener != null) {
					solutionListener.updatedSolution(this, i);
				}
			}
		}
		if (solutionListener != null) {
			solutionListener.done(this);
		}
	}
	
	protected double getBeta(int topicIndex, int wordIndex) {
		return beta;
	}
	
	protected double getBetaSum(int topicIndex) {
		return betaSum;
	}

	protected void afterSampling(int i, int numberOfIterations) {
	}

	protected void initialize(LDASolutionListener<T> listener) {
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
				int topic = random.nextInt(alpha.length);
				documentWordOccurrenceLastTopicAssignment[index][h2][j] = topic;
				documentTopicAssignmentCount[index][topic]++;
				documentSize[index] = d.getSize();
				topicWordAssignmentCount[topic][wordIndex]++;
				topicFrCount[topic]++;
			}
			h2++;
		}
	}

	public int[] foldIn(int iterations, Document<T> d) {
		int h2 = 0;
		int[][] dWordOccurrenceLastTopicAssignment = new int[d.getWordIndices()
				.size()][];
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);
			dWordOccurrenceLastTopicAssignment[h2] = new int[fr];
			h2++;
		}
		int[] dTopicAssignmentCount = new int[alpha.length];

		int[][] topicWordAssignmentCountCopy = new int[topicWordAssignmentCount.length][];
		for (int i = 0; i < topicWordAssignmentCount.length; i++) {
			topicWordAssignmentCountCopy[i] = Arrays.copyOf(
					topicWordAssignmentCount[i],
					topicWordAssignmentCount[i].length);

		}
		int[] topicFrCountCopy = Arrays.copyOf(topicFrCount,
				topicFrCount.length);

		// Initialize topics randomly
		h2 = 0;
		it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int wordIndex = it.next();
			int fr = d.getWordFrequency(wordIndex);

			T word = d.getProvider().getWord(wordIndex);
			int tIndex = provider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				for (int j = 0; j < fr; j++) {
					int topic = random.nextInt(alpha.length);
					dWordOccurrenceLastTopicAssignment[h2][j] = topic;
					dTopicAssignmentCount[topic]++;
					topicWordAssignmentCountCopy[topic][tIndex]++;
					topicFrCountCopy[topic]++;
				}
			}
			h2++;
		}

		for (int i = 0; i < iterations; i++) {
			TIntIterator it2 = d.getWordIndices().iterator();
			h2 = 0;
			while (it2.hasNext()) {
				int wordIndex = it2.next();
				int fr = d.getWordFrequency(wordIndex);

				T word = d.getProvider().getWord(wordIndex);
				int tIndex = provider.getIndex(word);
				// Ensure the word is in the training vocabulary.
				if (tIndex >= 0) {
					for (int j = 0; j < fr; j++) {
						int topic = dWordOccurrenceLastTopicAssignment[h2][j];
						dTopicAssignmentCount[topic]--;
						topicWordAssignmentCountCopy[topic][tIndex]--;
						topicFrCountCopy[topic]--;
						for (int k = 0; k < alpha.length; k++) {
							samplingRatios[k] = (dTopicAssignmentCount[k] + alpha[k])
									* (topicWordAssignmentCountCopy[k][tIndex] + getBeta(k, wordIndex))
									/ (topicFrCountCopy[k] + getBetaSum(k));
						}
						topic = nextDiscrete(samplingRatios);
						dWordOccurrenceLastTopicAssignment[h2][j] = topic;
						dTopicAssignmentCount[topic]++;
						topicWordAssignmentCountCopy[topic][tIndex]++;
						topicFrCountCopy[topic]++;
					}
				}
				h2++;
			}
		}

		return dTopicAssignmentCount;
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
}
