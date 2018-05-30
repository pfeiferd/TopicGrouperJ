package org.hhn.topicgrouper.lda.validation;

import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.LDAPerplexityResultReporter;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;

public abstract class AbstractMultiTopicLDAGibbs<T> extends
		AbstractMulitParamLDAGibbs<T> {
	private int topics;

	public AbstractMultiTopicLDAGibbs(PrintStream pw,
			DocumentProvider<T> trainingDocumentProvider,
			DocumentProvider<T> testDocumentProvider, int iterations,
			Random random) {
		super(pw, trainingDocumentProvider, testDocumentProvider, iterations,
				random);
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
				perplexityComputed(topics, value);
			}
		};
	}

	protected abstract BasicPerplexityCalculator<T> createPerplexityCalculator();
	
	public void solve(int minTopic, int stepSize, int steps, double baseAlpha,
			double baseBeta) {
		for (int i = 0; i < steps; i++) {
			topics = minTopic + i * stepSize;
			LDAGibbsSampler<T> sampler = createSampler(baseAlpha, baseBeta,
					topics);
			sampler.solve(iterations / 4, iterations, solutionReporter);
		}
	}
}
