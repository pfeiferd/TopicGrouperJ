package org.hhn.topicgrouper.lda.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

/**
 * Perplexity calculator that estimates the topic distribution of the reference document by
 * Gibbs-sampling fold-in with the trained LDA sampler (Wallach et al., "Evaluation Methods for
 * Topic Models", 2009, equations 7 and 27).
 *
 * @param <T> the type of the words
 */
public class PerplexityCalculatorEstimatedTheta<T> extends
		BasicPerplexityCalculator<T> {
	/**
	 * Number of fold-in iterations per document.
	 */
	protected final int foldInIterations;
	private LDAGibbsSampler<T>.FoldInStore foldInStore;

	/**
	 * Uses 100 samples and folds in the whole test document before scoring it.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param foldInIterations number of fold-in iterations
	 */
	public PerplexityCalculatorEstimatedTheta(boolean bowFactor,
			int foldInIterations) {
		this(bowFactor, new DefaultDocumentSplitter<T>(false), foldInIterations, 100);
	}

	/**
	 * Creates a calculator. For each reference document the fold-in is run for the given number
	 * of iterations as burn-in, then <code>samplingMax</code> further Gibbs sweeps each yield one
	 * sample of p(t|d) = (n_dt + alpha_t) / (n_d + sum_t alpha_t) over which the test document
	 * probability is averaged. The model set with {@link #setTopicModeler(AbstractTopicModeler)}
	 * must be an {@link LDAGibbsSampler}.
	 *
	 * @param bowFactor whether to include the bag-of-words factor
	 * @param documentSplitter splits test documents into reference and test part
	 * @param foldInIterations number of fold-in iterations
	 * @param samplingMax number of samples of p(t|d)
	 */
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
