package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;

public class LDAPerplexityCalculatorWithFoldIn<T> implements AbstractLDAPerplexityCalculator<T> {
	private final boolean bowFactor;
	private final int foldInIterations;

	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor, int foldInIterations) {
		this.bowFactor = bowFactor;
		this.foldInIterations = foldInIterations;
	}

	public double computePerplexity(
			DocumentProvider<T> testDocumentProvider,
			LDAGibbsSampler<T> sampler) {
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		double sumA = 0;
		double sumB = 0;
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		for (Document<T> d : testDocumentProvider.getDocuments()) {
			int dSize = 0;
			for (int j = 0; j < provider.getNumberOfWords(); j++) {
				T word = provider.getWord(j);
				int index = provider.getIndex(word);
				if (index >= 0) {
					dSize += d.getWordFrequency(index);
				}
			}
			sumA += computeLogProbability(sampler, provider, d,
					dSize);
			sumB += dSize;
		}
		return Math.exp(-sumA / sumB);
	}

	public double computeLogProbability(LDAGibbsSampler<T> sampler,
			DocumentProvider<T> trainingDocumentProvider, Document<T> d,
			int dSize) {
		double res = bowFactor ? PerplexityCalculator.logFacN(dSize) : 0;

		int[] topicAssignmentCount = sampler.foldIn(foldInIterations, d);

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int tIndex = trainingDocumentProvider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					if (bowFactor) {
						res -= PerplexityCalculator.logFacN(wordFr);
					}
					res += wordFr
							* computeWordLogProbability(sampler, tIndex,
									wordFr, d, topicAssignmentCount);
				}
			}
		}
		return res;
	}

	private double computeWordLogProbability(LDAGibbsSampler<T> sampler,
			int tIndex, int fr, Document<T> d, int[] topicAssignmentCount) {
		double sum = 0;
		for (int i = 0; i < topicAssignmentCount.length; i++) {
			sum += ((double) sampler.getTopicWordAssignmentCount(i,tIndex))
					/ sampler.getTopicFrCount(i) * (topicAssignmentCount[i] + 1) / (d.getSize() + topicAssignmentCount.length);
		}
		return Math.log(sum);
	}
}
