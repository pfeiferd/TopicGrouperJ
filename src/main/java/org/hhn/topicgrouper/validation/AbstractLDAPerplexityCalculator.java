package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;

public interface AbstractLDAPerplexityCalculator<T> {
	public double computePerplexity(
			DocumentProvider<T> testDocumentProvider,
			LDAGibbsSampler<T> sampler);

}
