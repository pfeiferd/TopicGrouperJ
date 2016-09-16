package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class LDAPerplexityCalculatorWithFoldIn<T> extends
		AbstractLDAPerplexityCalculator<T> {
	// Small value in order to avoid distortion caused by smoothing.
	public static final double DEFAULT_LIDSTONE_LAMDA = 0.00000000000001d;
	
	private final int foldInIterations;
	private final double lidstoneLambda;

	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor,
			int foldInIterations) {
		this(bowFactor, foldInIterations, DEFAULT_LIDSTONE_LAMDA);
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
			// Smoothing is necessary because no ptd element must be zero.
			// Otherwise sum in computeWordLogProbability() may become zero which leads
			// to negative infinity for Math.log(sum) ...
			ptd[i] = ((double) topicAssignmentCount[i] + lidstoneLambda)
					/ (dSize + lidstoneLambda * ptd.length); // Lidstone smoothing.
		}
	}
}
