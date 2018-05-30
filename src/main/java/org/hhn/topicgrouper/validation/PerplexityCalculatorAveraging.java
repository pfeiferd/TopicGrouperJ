package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;

public class PerplexityCalculatorAveraging<T> extends
		BasicPerplexityCalculator<T> {
	public PerplexityCalculatorAveraging(boolean bowFactor) {
		super(bowFactor);
	}

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
