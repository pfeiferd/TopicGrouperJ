package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;

/**
 * Perplexity calculator that estimates p(t|d) from the reference document by averaging the
 * posterior topic probabilities p(t|w) = phi(t,w) p(t) / sum_t' phi(t',w) p(t') of its words.
 *
 * @param <T> the type of the words
 */
public class PerplexityCalculatorAveraging<T> extends
		BasicPerplexityCalculator<T> {
	/**
	 * Uses the default document splitter.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 */
	public PerplexityCalculatorAveraging(boolean bowFactor) {
		super(bowFactor);
	}

	/**
	 * Creates a calculator; since the estimate of p(t|d) is deterministic, a single sample is used.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param documentSplitter splits test documents into reference and test part
	 */
	public PerplexityCalculatorAveraging(boolean bowFactor,
			DocumentSplitter<T> documentSplitter) {
		super(bowFactor, documentSplitter, 1);
	}

	@Override
	protected void updatePtd(Document<T> d, AbstractTopicModeler<T> sampler) {
		if (d == null) {
			super.updatePtd(d, sampler);
		} else {
			Arrays.fill(ptd, 0);
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int fr = d.getWordFrequency(index);
				// Marginal probability of the word under the model, so that
				// the posteriors over the topics sum to one.
				double pw = 0;
				for (int i = 0; i < ptd.length; i++) {
					pw += sampler.getPhi(i, index) * sampler.getTopicProb(i);
				}
				if (pw > 0) {
					for (int i = 0; i < ptd.length; i++) {
						ptd[i] += (((double) fr) / d.getSize())
								* sampler.getPhi(i, index)
								* sampler.getTopicProb(i) / pw;
					}
				}
			}
		}
	}
}
