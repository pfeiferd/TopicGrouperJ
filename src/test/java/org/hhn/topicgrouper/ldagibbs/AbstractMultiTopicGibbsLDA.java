package org.hhn.topicgrouper.ldagibbs;

import org.hhn.topicgrouper.base.DocumentProvider;

public abstract class AbstractMultiTopicGibbsLDA {
	private final DocumentProvider<String> trainingDocumentProvider;
	private final DocumentProvider<String> testDocumentProvider;
	private final BasicGibbsSolutionReporter solutionReporter;

	public AbstractMultiTopicGibbsLDA(
			DocumentProvider<String> trainingDocumentProvider,
			DocumentProvider<String> testDocumentProvider) {
		this.trainingDocumentProvider = trainingDocumentProvider;
		this.testDocumentProvider = testDocumentProvider;
		solutionReporter = createSolutionReporter();
	}

	protected BasicGibbsSolutionReporter createSolutionReporter() {
		return new BasicGibbsSolutionReporter(System.out);
	}

	public void inference(int minTopic, int stepSize, int steps, int iterations)
			throws Exception {
		for (int i = 0; i < steps; i++) {
			int topics = minTopic + i * stepSize;
			AbstractGibbsSamplingLDAWithPerplexity sampler = createSampler(
					topics, createAlpha(topics), createBeta(topics), iterations);
			sampler.folderPath = "target/";
			sampler.inference();
		}
	}

	protected abstract double[] createAlpha(int topics);

	protected abstract double createBeta(int topics);

	protected DocumentProvider<String> getTestDocumentProvider() {
		return testDocumentProvider;
	}

	protected DocumentProvider<String> getTrainingDocumentProvider() {
		return trainingDocumentProvider;
	}
	
	public BasicGibbsSolutionReporter getSolutionReporter() {
		return solutionReporter;
	}

	protected abstract AbstractGibbsSamplingLDAWithPerplexity createSampler(
			int topics, double[] alpha, double beta, int iterations)
			throws Exception;
}
