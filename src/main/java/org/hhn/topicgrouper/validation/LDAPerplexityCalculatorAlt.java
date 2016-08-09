package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;


public class LDAPerplexityCalculatorAlt<T> extends
		AbstractLDAPerplexityCalculator<T> {
	public LDAPerplexityCalculatorAlt(boolean bowFactor) {
		super(bowFactor);
	}
	
	@Override
	protected void updatePtd(LDAGibbsSampler<T> sampler, Document<T> d, int dSize, int dIndex) {
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = 0;
			for (int j = 0; j < provider.getNumberOfWords(); j++) {
				T word = provider.getWord(j);
				int index = d.getProvider().getIndex(word);
				if (index >= 0) {
					ptd[i] += ((double) d.getWordFrequency(index))
							* sampler.getTopicWordAssignmentCount(i, j)
							/ provider.getWordFrequency(j);
				}
			}
			ptd[i] /= dSize;
		}		
	}	
}
