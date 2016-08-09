package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;

public class LDAPerplexityCalculatorWithFoldIn<T> extends
		AbstractLDAPerplexityCalculator<T> {
	private final int foldInIterations;

	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor,
			int foldInIterations) {
		super(bowFactor);
		this.foldInIterations = foldInIterations;
	}

	@Override
	protected void updatePtd(LDAGibbsSampler<T> sampler, Document<T> d,
			int dSize, int dIndex) {
		int[] topicAssignmentCount = sampler.foldIn(foldInIterations, d);

		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = ((double) (topicAssignmentCount[i] + 1))
					/ (d.getSize() + topicAssignmentCount.length);
		}
	}
}
