package org.hhn.topicgrouper.lda.impl;


/**
 * Listener for the progress of {@link LDAGibbsSampler#solve(int, int, LDASolutionListener)}.
 *
 * @param <T> the type of the words
 */
public interface LDASolutionListener<T> {
	/**
	 * Called before the topic assignments are initialized.
	 *
	 * @param sampler the sampler
	 */
	public void beforeInitialization(LDAGibbsSampler<T> sampler);

	/**
	 * Called before the given document is initialized.
	 *
	 * @param sampler the sampler
	 * @param document the index of the document
	 */
	public void initalizing(LDAGibbsSampler<T> sampler, int document);

	/**
	 * Called after initialization, before the first iteration.
	 *
	 * @param sampler the sampler
	 */
	public void initialized(LDAGibbsSampler<T> sampler);

	/**
	 * Called after each sampling iteration.
	 *
	 * @param sampler the sampler
	 * @param iteration the zero-based iteration number
	 */
	public void updatedSolution(LDAGibbsSampler<T> sampler, int iteration);
	
	/**
	 * Called after the last iteration and the computation of the averaged estimates.
	 *
	 * @param sampler the sampler
	 */
	public void done(LDAGibbsSampler<T> sampler);
}
