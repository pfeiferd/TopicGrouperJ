package org.hhn.topicgrouper.lda.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

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
		long sumB = 0;
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		int i = 0;
		for (Document<T> d : testDocumentProvider.getDocuments()) {
			int dSize = 0;

			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				T word = testDocumentProvider.getWord(index);
				int sIndex = provider.getIndex(word);
				if (sIndex >= 0) {
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
		double res = bowFactor ? TGPerplexityCalculator.logFacN(dSize) : 0;

		// update ptd for d
		updatePtd(sampler, d, dSize, dIndex);

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int sIndex = provider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (sIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					if (bowFactor) {
						res -= TGPerplexityCalculator.logFacN(wordFr);
					}
					res += wordFr
							* computeWordLogProbability(sampler, sIndex, d);
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
			sum += ((double) sampler.getTopicWordAssignmentCount(i, tIndex) + 1) // + 1 --> Laplace smoothing.
					/ (sampler.getTopicFrCount(i) + ptd.length) * ptd[i]; // Laplace smoothing to avoid division by zero.
		}
		return Math.log(sum);
	}
}
