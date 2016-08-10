package org.hhn.topicgrouper.validation;

import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.LDAPerplexityResultReporter;
import org.hhn.topicgrouper.util.TwoParameterSearcher;

public abstract class AbstractHyperparamOptGibbsLDA<T> {
	protected final PrintStream pw;
	protected final DocumentProvider<T> trainingDocumentProvider;
	protected final DocumentProvider<T> testDocumentProvider;
	protected final LDAPerplexityResultReporter<T> solutionReporter;
	protected final TwoParameterSearcher twoParameterSearcher;
	protected final int topics;
	protected final int iterations;
	protected final double[][] xyArea;
	protected final Random random;

	public AbstractHyperparamOptGibbsLDA(PrintStream pw,
			DocumentProvider<T> trainingDocumentProvider,
			DocumentProvider<T> testDocumentProvider, int topics,
			int iterations, double[][] xyArea, Random random) {
		this.pw = pw;
		this.trainingDocumentProvider = trainingDocumentProvider;
		this.testDocumentProvider = testDocumentProvider;
		this.iterations = iterations;
		this.topics = topics;
		this.xyArea = xyArea;
		this.twoParameterSearcher = createParameterSearcher();
		this.solutionReporter = createSolutionReporter();
		this.random = random;
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
				LDAGibbsSampler<T> sampler = createSampler(
						createAlpha(x, topics), createBeta(y, topics),
						iterations);
				sampler.solve(iterations, solutionReporter);
				return perplexityStore;
			}
		};
	}

	protected void checkFor(double alpha, double beta) {
		pw.println("alpha: " + alpha);
		pw.println("beta: " + beta);
	}

	protected LDAPerplexityResultReporter<T> createSolutionReporter() {
		return new LDAPerplexityResultReporter<T>(testDocumentProvider, pw,
				iterations, createPerplexityCalculator()) {
			@Override
			public void updatedSolution(LDAGibbsSampler<T> sampler,
					int iteration) {
				// Do nothing on purpose.
			}

			@Override
			public void done(LDAGibbsSampler<T> sampler) {
				double value = computePerplexity(sampler);
				perplexityComputed(-1, value, topics);
			}

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

	protected LDAGibbsSampler<T> createSampler(double[] alpha, double beta,
			int iterations) {
		return new LDAGibbsSampler<T>(trainingDocumentProvider, alpha, beta,
				random);
	}

	protected abstract AbstractLDAPerplexityCalculator<T> createPerplexityCalculator();
}
