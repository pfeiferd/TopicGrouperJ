package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.ldaimpl.LDASolutionListener;
import org.hhn.topicgrouper.report.BasicLDAResultReporter;

public abstract class AbstractMultiTopicGibbsLDA<T> {
	private final DocumentProvider<T> trainingDocumentProvider;
	private final DocumentProvider<T> testDocumentProvider;
	private final LDASolutionListener<T> solutionReporter;

	public AbstractMultiTopicGibbsLDA(
			DocumentProvider<T> trainingDocumentProvider,
			DocumentProvider<T> testDocumentProvider) {
		this.trainingDocumentProvider = trainingDocumentProvider;
		this.testDocumentProvider = testDocumentProvider;
		solutionReporter = createSolutionReporter();
	}

	protected LDASolutionListener<T> createSolutionReporter() {
		return new BasicLDAResultReporter<T>(System.out, 10);
	}

	public void solve(int minTopic, int stepSize, int steps, int iterations)
			throws Exception {
		for (int i = 0; i < steps; i++) {
			int topics = minTopic + i * stepSize;
			LDAGibbsSampler<T> sampler = createSampler(
					topics, createAlpha(topics), createBeta(topics), iterations);
			sampler.solve(iterations, solutionReporter);
		}
	}

	protected abstract double[] createAlpha(int topics);

	protected abstract double createBeta(int topics);

	protected DocumentProvider<T> getTestDocumentProvider() {
		return testDocumentProvider;
	}

	protected DocumentProvider<T> getTrainingDocumentProvider() {
		return trainingDocumentProvider;
	}
	
	protected LDASolutionListener<T> getSolutionReporter() {
		return solutionReporter;
	}

	protected abstract LDAGibbsSampler<T> createSampler(
			int topics, double[] alpha, double beta, int iterations);
}
