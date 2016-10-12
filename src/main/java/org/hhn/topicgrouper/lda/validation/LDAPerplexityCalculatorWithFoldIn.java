package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

public class LDAPerplexityCalculatorWithFoldIn<T> extends
		AbstractLDAPerplexityCalculator<T> {
	protected final int foldInIterations;
	private LDAGibbsSampler<T>.FoldInStore foldInStore;

	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor,
			int foldInIterations) {
		this(bowFactor, null, foldInIterations);
	}

	public LDAPerplexityCalculatorWithFoldIn(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int foldInIterations) {
		super(bowFactor, documentSplitter, 100);
		this.foldInIterations = foldInIterations;
	}

	@Override
	protected void initPtd(Document<T> d, LDAGibbsSampler<T> sampler) {
		foldInStore = sampler.foldIn(foldInIterations, d, foldInStore);
	}

	// Compute psi_t^s to "Equation Methods for Topic Models" (Wallach et al) equation 27
	@Override
	protected void updatePtd(Document<T> d, LDAGibbsSampler<T> sampler) {
		int[] dTopicAssignmentCounts = foldInStore.nextFoldInPtdSample();
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = (dTopicAssignmentCounts[i] + sampler.getAlpha(i)) / (d.getSize() + sampler.getAlphaSum());
		}
	}
}
