package org.hhn.topicgrouper.validation;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.LDAPerplexityResultReporter;
import org.hhn.topicgrouper.util.TwoParameterSearcher;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;

public abstract class AbstractHyperparamOptGibbsLDA<T> {
	private final DocumentProvider<T> trainingDocumentProvider;
	private final DocumentProvider<T> testDocumentProvider;
	private final LDAPerplexityResultReporter<T> solutionReporter;
	private final TwoParameterSearcher twoParameterSearcher;
	private final int topics;
	private final int iterations;
	private final double[][] xyArea;

	public AbstractHyperparamOptGibbsLDA(
			DocumentProvider<T> trainingDocumentProvider,
			DocumentProvider<T> testDocumentProvider, int topics,
			int iterations, double[][] xyArea) {
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
					LDAGibbsSampler<T> sampler = createSampler(topics,
							createAlpha(x, topics), createBeta(y, topics),
							iterations);
					sampler.solve(iterations, solutionReporter);
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

	protected LDAPerplexityResultReporter<T> createSolutionReporter() {
		return new LDAPerplexityResultReporter<T>(trainingDocumentProvider,
				System.out, iterations, createPerplexityCalculator()) {
			@Override
			protected void perplexityComputed(int step, double value, int topics) {
				super.perplexityComputed(step, value, topics);
				perplexityStore = value;
			}
		};
	}

	protected double[] createAlpha(double base, int topics) {
		return LDAGibbsSampler.symmetricAlpha(base / topics, topics);
	}

	protected double createBeta(double base, int topics) {
		return base;
	}

	protected DocumentProvider<T> getTestDocumentProvider() {
		return testDocumentProvider;
	}

	protected DocumentProvider<T> getTrainingDocumentProvider() {
		return trainingDocumentProvider;
	}

	public LDAPerplexityResultReporter<T> getSolutionReporter() {
		return solutionReporter;
	}

	protected abstract LDAGibbsSampler<T> createSampler(int topics,
			double[] alpha, double beta, int iterations);

	protected abstract AbstractLDAPerplexityCalculator<T> createPerplexityCalculator();
}
