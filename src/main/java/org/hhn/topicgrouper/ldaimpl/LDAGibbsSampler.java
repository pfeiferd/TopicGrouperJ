package org.hhn.topicgrouper.ldaimpl;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;

public class LDAGibbsSampler<T> {
	private final Random random;
	private final double[] alpha;
	private final double beta;
	private final double betaSum;
	private final DocumentProvider<T> provider;

	private final int[][] documentTopicAssignmentCount;
	private final int[][] wordTopicAssignmentCount;
	private final int[] topicFrCount;
	private final int[][][] documentWordOccurrenceLastTopicAssignment;
	private final List<Document<T>> documents;

	private double samplingRatios[];

	public LDAGibbsSampler(DocumentProvider<T> documentProvider,
			double[] alpha, double beta, Random random) {
		this.alpha = alpha;
		this.beta = beta;
		this.betaSum = beta * documentProvider.getNumberOfWords();
		this.provider = documentProvider;
		this.random = random;
		documents = provider.getDocuments();
		documentTopicAssignmentCount = new int[documents.size()][alpha.length];
		wordTopicAssignmentCount = new int[provider.getNumberOfWords()][alpha.length];
		topicFrCount = new int[alpha.length];
		documentWordOccurrenceLastTopicAssignment = new int[documents.size()][][];
		int h = 0;
		for (Document<T> d : documents) {
			int h2 = 0;
			documentWordOccurrenceLastTopicAssignment[h] = new int[d
					.getWordIndices().size()][];
			TIntIterator it = d.getWordIndices().iterator();
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
	
	public void train(int iterations) {
		initialize();

		for (int i = 0; i < iterations; i++) {
			int h = 0;
			for (Document<T> d : documents) {
				int h2 = 0;
				TIntIterator it = d.getWordIndices().iterator();
				while (it.hasNext()) {
					int wordIndex = it.next();
					int fr = d.getWordFrequency(wordIndex);
					for (int j = 0; j < fr; j++) {
						int topic = documentWordOccurrenceLastTopicAssignment[h][h2][j];
						documentTopicAssignmentCount[h][topic]--;
						wordTopicAssignmentCount[wordIndex][topic]--;
						topicFrCount[topic]--;
						for (int k = 0; k < alpha.length; k++) {
							samplingRatios[k] = (documentTopicAssignmentCount[h2][k] + alpha[k])
									* (documentWordOccurrenceLastTopicAssignment[h][h2][k] + beta)
									/ (topicFrCount[k] + betaSum);
						}
						topic = nextDiscrete(samplingRatios);
						documentWordOccurrenceLastTopicAssignment[h][h2][j] = topic;
						documentTopicAssignmentCount[h][topic]++;
						wordTopicAssignmentCount[wordIndex][topic]++;
						topicFrCount[topic]++;
					}
					h2++;
				}
				h++;
			}
		}
	}

	protected void initialize() {
		// TODO
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
		// TODO: Initialize

		int[][] wordTopicAssignmentCountCopy = new int[wordTopicAssignmentCount.length][];
		for (int i = 0; i < wordTopicAssignmentCount.length; i++) {
			wordTopicAssignmentCountCopy[i] = Arrays.copyOf(
					wordTopicAssignmentCount[i], wordTopicAssignmentCount[i].length);

		}
		int[] topicFrCountCopy = Arrays.copyOf(topicFrCount, topicFrCount.length);

		for (int i = 0; i < iterations; i++) {
			TIntIterator it2 = d.getWordIndices().iterator();
			h2 = 0;
			while (it2.hasNext()) {
				int wordIndex = it2.next();
				int fr = d.getWordFrequency(wordIndex);
				for (int j = 0; j < fr; j++) {
					int topic = dWordOccurrenceLastTopicAssignment[h2][j];
					dTopicAssignmentCount[topic]--;
					wordTopicAssignmentCountCopy[wordIndex][topic]--;
					topicFrCountCopy[topic]--;
					for (int k = 0; k < alpha.length; k++) {
						samplingRatios[k] = (documentTopicAssignmentCount[h2][k] + alpha[k])
								* (dWordOccurrenceLastTopicAssignment[h2][k] + beta)
								/ (topicFrCountCopy[k] + betaSum);
					}
					topic = nextDiscrete(samplingRatios);
					dWordOccurrenceLastTopicAssignment[h2][j] = topic;
					dTopicAssignmentCount[topic]++;
					wordTopicAssignmentCountCopy[wordIndex][topic]++;
					topicFrCountCopy[topic]++;
				}
				h2++;
			}
		}
		
		return dTopicAssignmentCount;
	}

	private int nextDiscrete(double[] probs) {
		double sum = 0.0;
		for (int i = 0; i < probs.length; i++)
			sum += probs[i];

		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r)
				return i;
		}
		return probs.length - 1;
	}

}
