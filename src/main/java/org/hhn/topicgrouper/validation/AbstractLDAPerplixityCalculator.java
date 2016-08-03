package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;

public interface AbstractLDAPerplixityCalculator<T> {
	public double computePerplexity(
			DocumentProvider<T> trainingDocumentProvider,
			LDAGibbsSampler<T> sampler);

}
