package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;

public class LDAPerplexityCalculatorInDoc<T> implements
		AbstractLDAPerplexityCalculator<T> {
	private final boolean bowFactor;

	public LDAPerplexityCalculatorInDoc(boolean bowFactor) {
		this.bowFactor = bowFactor;
	}

	@Override
	public double computePerplexity(DocumentProvider<T> testDocumentProvider,
			LDAGibbsSampler<T> sampler) {
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		double sumA = 0;
		double sumB = 0;
		int[] dSize = new int[1];
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		int i = 0;
		for (Document<T> d : testDocumentProvider.getDocuments()) {
			sumA += computeLogProbability(sampler, provider, d, dSize, i);
			sumB += dSize[0];
			i++;
		}
		return Math.exp(-sumA / sumB);
	}

	public double computeLogProbability(LDAGibbsSampler<T> sampler,
			DocumentProvider<T> trainingDocumentProvider, Document<T> d,
			int[] dSize, int dIndex) {
		double res = 0;
		dSize[0] = 0;
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int tIndex = trainingDocumentProvider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					double pw = computeWordProbability(sampler, tIndex, wordFr,
							d, dIndex);
					if (pw > 0) {
						if (bowFactor) {
							res -= PerplexityCalculator.logFacN(wordFr);
						}
						res += wordFr * Math.log(pw);
					} else {
						// Do nothing: should not happen, but it does.
						// We simply ignore the probability which is in favour
						// of LDA.
					}
					dSize[0] += wordFr;
				}
			}
		}
		res += bowFactor ? PerplexityCalculator.logFacN(dSize[0]) : 0;
		return res;
	}

	private double computeWordProbability(LDAGibbsSampler<T> sampler,
			int tIndex, int fr, Document<T> d, int dIndex) {
		double sum = 0;
		for (int i = 0; i < sampler.getNTopics(); i++) {
			sum += ((double) sampler.getTopicWordAssignmentCount(i, tIndex) / sampler
					.getTopicFrCount(i))
					* (((double) sampler.getDocumentTopicAssignmentCount(
							dIndex, i) / sampler.getDocumentSize(dIndex)));
		}
		return sum;
	}
}
