package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class LDAPerplexityCalculatorWithFoldIn<T> extends
		AbstractLDAPerplexityCalculator<T> {
	private final int foldInIterations;
	private final double lidstoneLambda;

	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor,
			int foldInIterations) {
		this(bowFactor, foldInIterations, 0.00000000000001);
	}
	
	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor,
			int foldInIterations, double lidstoneLambda) {
		super(bowFactor);
		this.lidstoneLambda = lidstoneLambda;
		this.foldInIterations = foldInIterations;
	}

	@Override
	protected void updatePtd(LDAGibbsSampler<T> sampler, Document<T> d,
			int dSize, int dIndex) {
		int[] topicAssignmentCount = sampler.foldIn(foldInIterations, d);

		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = ((double) topicAssignmentCount[i] + lidstoneLambda)
					/ (dSize + lidstoneLambda * ptd.length); // Lidstone smoothing.
		}
	}
}
