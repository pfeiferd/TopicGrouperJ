package org.hhn.topicgrouper.ldagibbs;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.util.TwoParameterSearcher;

public abstract class AbstractHyperparamOptGibbsLDA {
	private final DocumentProvider<String> trainingDocumentProvider;
	private final DocumentProvider<String> testDocumentProvider;
	private final BasicGibbsSolutionReporter solutionReporter;
	private final TwoParameterSearcher twoParameterSearcher;
	private final int topics;
	private final int iterations;
	private final double[][] xyArea;

	public AbstractHyperparamOptGibbsLDA(
			DocumentProvider<String> trainingDocumentProvider,
			DocumentProvider<String> testDocumentProvider, int topics,
			int iterations,
			double[][] xyArea) {
		this.trainingDocumentProvider = trainingDocumentProvider;
		this.testDocumentProvider = testDocumentProvider;
		this.iterations = iterations;
		this.topics = topics;
		this.xyArea = xyArea;
		this.twoParameterSearcher = createParameterSearcher();
		this.solutionReporter = createSolutionReporter();
	}
	
	public void run() {
		twoParameterSearcher.search();
	}

	private double perplexityStore;

	protected TwoParameterSearcher createParameterSearcher() {
		return new TwoParameterSearcher(xyArea, 10) {
			@Override
			protected double optFunction(double x, double y) {
				checkFor(x, y);
				try {
					AbstractGibbsSamplingLDAWithPerplexity sampler = createSampler(
							topics, createAlpha(x, topics),
							createBeta(y, topics), iterations);
					sampler.folderPath = "target/";
					sampler.inference();
					return perplexityStore;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
	
	protected void checkFor(double alpha, double beta) {
		System.out.println("alpha: " + alpha);
		System.out.println("beta: " + beta);
	}

	protected BasicGibbsSolutionReporter createSolutionReporter() {
		return new BasicGibbsSolutionReporter(System.out) {
			@Override
			public void perplexityComputed(int step, double value, int topics) {
				super.perplexityComputed(step, value, topics);
				perplexityStore = value;
			}
		};
	}

	protected double[] createAlpha(double base, int topics) {
		return AbstractGibbsSamplingLDAWithPerplexity.symmetricAlpha(
				base / topics, topics);		
	}

	protected double createBeta(double base, int topics) {
		return base;
	}

	protected DocumentProvider<String> getTestDocumentProvider() {
		return testDocumentProvider;
	}

	protected DocumentProvider<String> getTrainingDocumentProvider() {
		return trainingDocumentProvider;
	}

	public BasicGibbsSolutionReporter getSolutionReporter() {
		return solutionReporter;
	}

	protected String createFileName(int topics, double[] alpha, double beta,
			int iterations) {
		return "HyperparamOptGibbsLDA_";
	}
	
	protected abstract AbstractGibbsSamplingLDAWithPerplexity createSampler(
			int topics, double[] alpha, double beta, int iterations)
			throws Exception;
}
