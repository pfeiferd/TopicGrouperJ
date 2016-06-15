package org.hhn.topicgrouper.ldagibbs;

import org.hhn.topicgrouper.base.DocumentProvider;

public abstract class AbstractGibbsSamplingLDAWithPerplexity extends
		GibbsSamplingLDAAdapt {
	protected final boolean bowFactor;
	private final int perplexitySteps;
	private final DocumentProvider<String> trainingDocumentProvider;
	private final DocumentProvider<String> testDocumentProvider;
	private final BasicGibbsSolutionReporter solutionReporter;

	public AbstractGibbsSamplingLDAWithPerplexity(
			BasicGibbsSolutionReporter reporter,
			DocumentProvider<String> documentProvider, double[] inAlpha,
			double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider, int ppSteps)
			throws Exception {
		this(reporter, documentProvider, inAlpha, inBeta, inNumIterations,
				inTopWords, inExpName, pathToTAfile, inSaveStep,
				testDocumentProvider, ppSteps, true);
	}

	public AbstractGibbsSamplingLDAWithPerplexity(
			BasicGibbsSolutionReporter reporter,
			DocumentProvider<String> documentProvider, double[] inAlpha,
			double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider, int ppSteps,
			boolean bowFactor) throws Exception {
		super(documentProvider, inAlpha, inBeta, inNumIterations, inTopWords,
				inExpName, pathToTAfile, inSaveStep);
		this.solutionReporter = reporter;
		this.perplexitySteps = ppSteps;
		this.bowFactor = bowFactor;
		this.trainingDocumentProvider = documentProvider;
		this.testDocumentProvider = testDocumentProvider;
	}

	protected DocumentProvider<String> getTrainingDocumentProvider() {
		return trainingDocumentProvider;
	}

	@Override
	protected void afterSampling(int i, int numberOfIterations) {
		if (i > 0 && i % perplexitySteps == 0) {
			double d = computePerplexity(testDocumentProvider);
			perplexityComputed(i, d);
		}
	}

	protected abstract double computePerplexity(
			DocumentProvider<String> provider);

	protected void perplexityComputed(int step, double value) {
		solutionReporter.perplexityComputed(step, value, numTopics);
	}

	public static double logFakN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}
}
