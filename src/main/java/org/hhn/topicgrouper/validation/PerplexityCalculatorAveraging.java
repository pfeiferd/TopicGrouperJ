package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;

/**
 * Perplexity calculator that estimates p(t|d) from the reference document by averaging the
 * posterior topic probabilities of its words.
 *
 * @param <T> the type of the words
 */
public class PerplexityCalculatorAveraging<T> extends
		BasicPerplexityCalculator<T> {
	/**
	 * @param bowFactor whether to include the bag-of-words factor
	 */
	public PerplexityCalculatorAveraging(boolean bowFactor) {
		super(bowFactor);
	}

	/**
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
				for (int i = 0; i < ptd.length; i++) {
					ptd[i] += (((double) fr) / d.getSize())
							* sampler.getPhi(i, index)
							* sampler.getTopicProb(i) / sampler.getWordProb(index);
				}
			}
		}
	}
}
