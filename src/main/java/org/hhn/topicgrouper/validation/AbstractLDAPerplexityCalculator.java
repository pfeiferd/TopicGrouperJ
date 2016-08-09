package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;

public abstract class AbstractLDAPerplexityCalculator<T> {
	protected final boolean bowFactor;
	protected double[] ptd;

	public AbstractLDAPerplexityCalculator(boolean bowFactor) {
		this.bowFactor = bowFactor;
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider,
			LDAGibbsSampler<T> sampler) {
		if (ptd == null || ptd.length != sampler.getNTopics()) {
			ptd = new double[sampler.getNTopics()];
		}

		DocumentProvider<T> provider = sampler.getDocumentProvider();
		double sumA = 0;
		double sumB = 0;
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		int i = 0;
		for (Document<T> d : testDocumentProvider.getDocuments()) {
			int dSize = 0;
			for (int j = 0; j < provider.getNumberOfWords(); j++) {
				T word = provider.getWord(j);
				int index = provider.getIndex(word);
				if (index >= 0) {
					dSize += d.getWordFrequency(index);
				}
			}
			sumA += computeLogProbability(sampler, d, dSize, i);
			sumB += dSize;
			i++;
		}
		return Math.exp(-sumA / sumB);
	}

	protected double computeLogProbability(LDAGibbsSampler<T> sampler,
			Document<T> d, int dSize, int dIndex) {
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		double res = bowFactor ? PerplexityCalculator.logFacN(dSize) : 0;

		// update ptd for d
		updatePtd(sampler, d, dSize, dIndex);

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int tIndex = provider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					if (bowFactor) {
						res -= PerplexityCalculator.logFacN(wordFr);
					}
					res += wordFr
							* computeWordLogProbability(sampler, tIndex, d);
				}
			}
		}
		return res;
	}

	protected abstract void updatePtd(LDAGibbsSampler<T> sampler,
			Document<T> d, int dSize, int dIndex);

	private double computeWordLogProbability(LDAGibbsSampler<T> sampler,
			int tIndex, Document<T> d) {
		double sum = 0;
		for (int i = 0; i < ptd.length; i++) {
			sum += ((double) sampler.getTopicWordAssignmentCount(i, tIndex) + 1)
					/ (sampler.getTopicFrCount(i) + ptd.length) * ptd[i];
		}
		return Math.log(sum);
	}
}
