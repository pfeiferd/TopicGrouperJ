package org.hhn.topicgrouper.lda.validation;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class LDAPerplexityCalculatorAveraging<T> extends
		AbstractLDAPerplexityCalculator<T> {
	public LDAPerplexityCalculatorAveraging(boolean bowFactor) {
		super(bowFactor);
	}

	public LDAPerplexityCalculatorAveraging(boolean bowFactor,
			DocumentSplitter<T> documentSplitter) {
		super(bowFactor, documentSplitter, 1);
	}

	@Override
	protected void updatePtd(Document<T> d, LDAGibbsSampler<T> sampler) {
		if (d == null) {
			super.updatePtd(d, sampler);
		} else {
			DocumentProvider<T> provider = sampler.getDocumentProvider();
			Arrays.fill(ptd, 0);
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int fr = d.getWordFrequency(index);
				for (int i = 0; i < ptd.length; i++) {
					ptd[i] += (((double) fr) / d.getSize())
							* sampler.getPhi(i, index)
							* sampler.getTopicProb(i) * provider.getSize()
							/ provider.getWordFrequency(index);
				}
			}
		}
	}
}
