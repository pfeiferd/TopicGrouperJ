package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class PerplexityCalculatorEstimatedTheta<T> extends
		BasicPerplexityCalculator<T> {
	protected final int foldInIterations;
	private LDAGibbsSampler<T>.FoldInStore foldInStore;

	public PerplexityCalculatorEstimatedTheta(boolean bowFactor,
			int foldInIterations) {
		this(bowFactor, null, foldInIterations, 100);
	}

	public PerplexityCalculatorEstimatedTheta(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int foldInIterations, int samplingMax) {
		super(bowFactor, documentSplitter, samplingMax);
		this.foldInIterations = foldInIterations;
	}

	@Override
	protected void initPtd(Document<T> d, AbstractTopicModeler<T> sampler) {
		foldInStore = ((LDAGibbsSampler<T>)sampler).foldIn(foldInIterations, d);
	}

	// Compute psi_t^s to "Equation Methods for Topic Models" (Wallach et al) equation 27
	@Override
	protected void updatePtd(Document<T> d, AbstractTopicModeler<T> sampler) {
		int[] dTopicAssignmentCounts = foldInStore.nextFoldInPtdSample();
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = (dTopicAssignmentCounts[i] + sampler.getAlpha(i)) / (d.getSize() + sampler.getAlphaConc());
		}
	}
}
