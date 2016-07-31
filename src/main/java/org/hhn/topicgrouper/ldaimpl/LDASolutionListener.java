package org.hhn.topicgrouper.ldaimpl;


public interface LDASolutionListener<T> {
	public void beforeInitialization(LDAGibbsSampler<T> sampler);

	public void initalizing(LDAGibbsSampler<T> sampler, int document);

	public void initialized(LDAGibbsSampler<T> sampler);

	public void updatedSolution(LDAGibbsSampler<T> sampler, int iteration);
	
	public void done(LDAGibbsSampler<T> sampler);
}
