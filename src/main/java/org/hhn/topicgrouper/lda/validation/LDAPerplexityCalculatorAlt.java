package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;


public class LDAPerplexityCalculatorAlt<T> extends
		AbstractLDAPerplexityCalculator<T> {
	public LDAPerplexityCalculatorAlt(boolean bowFactor) {
		super(bowFactor);
	}
	
	public LDAPerplexityCalculatorAlt(boolean bowFactor,
			DocumentSplitter<T> documentSplitter) {
		super(bowFactor, documentSplitter);
	}

	@Override
	protected void updatePtd(Document<T> d, LDAGibbsSampler<T> sampler) {
		if (d == null) {
			super.updatePtd(d, sampler);
		}
		else {
			DocumentProvider<T> provider = sampler.getDocumentProvider();
			for (int i = 0; i < ptd.length; i++) {
				ptd[i] = 0;
				for (int j = 0; j < provider.getNumberOfWords(); j++) {
					T word = provider.getWord(j);
					int index = d.getProvider().getIndex(word);
					if (index >= 0) {
						ptd[i] += ((double) d.getWordFrequency(index))
								* (sampler.getTopicWordAssignmentCount(i, j) + 1) // + 1 --> Laplace smoothing.
								/ (provider.getWordFrequency(j) + ptd.length); // Laplace smoothing to avoid division by zero.
					}
				}
				ptd[i] /= d.getSize();
			}
		}
	}	
}
