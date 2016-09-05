package org.hhn.topicgrouper.validation;

import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.LDAPerplexityResultReporter;
import org.hhn.topicgrouper.util.TwoParameterSearcher;

public abstract class AbstractHyperparamOptLDAGibbs<T> extends
		AbstractMulitParamLDAGibbs<T> {
	protected final TwoParameterSearcher twoParameterSearcher;
	private double perplexityStore;

	public AbstractHyperparamOptLDAGibbs(PrintStream pw,
			DocumentProvider<T> trainingDocumentProvider,
			DocumentProvider<T> testDocumentProvider, int iterations,
			Random random, int topics, double[][] xyArea) {
		super(pw, trainingDocumentProvider, testDocumentProvider, iterations,
				random);
		this.twoParameterSearcher = createParameterSearcher(xyArea, topics);
	}

	public void run() {
		twoParameterSearcher.search();
	}

	protected TwoParameterSearcher createParameterSearcher(double[][] xyArea,
			final int topics) {
		return new TwoParameterSearcher(xyArea, 10) {
			@Override
			protected double optFunction(double x, double y) {
				checkFor(x, y);
				LDAGibbsSampler<T> sampler = createSampler(x, y, topics);
				sampler.solve(iterations, solutionReporter);
				return perplexityStore;
			}
		};
	}

	protected void checkFor(double alpha, double beta) {
		pw.println("alpha: " + alpha);
		pw.println("beta: " + beta);
	}

	protected abstract AbstractLDAPerplexityCalculator<T> createPerplexityCalculator();

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
				perplexityComputed(-1, value);
			}

			@Override
			protected void perplexityComputed(int step, double value) {
				super.perplexityComputed(step, value);
				perplexityStore = value;
			}
		};
	}
}
