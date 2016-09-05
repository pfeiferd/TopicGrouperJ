package org.hhn.topicgrouper.lda.validation;

import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.LDAPerplexityResultReporter;

public abstract class AbstractMulitParamLDAGibbs<T> {
	protected final PrintStream pw;
	protected final DocumentProvider<T> trainingDocumentProvider;
	protected final DocumentProvider<T> testDocumentProvider;
	protected final LDAPerplexityResultReporter<T> solutionReporter;
	protected final int iterations;
	protected final Random random;

	public AbstractMulitParamLDAGibbs(PrintStream pw,
			DocumentProvider<T> trainingDocumentProvider,
			DocumentProvider<T> testDocumentProvider, int iterations,
			Random random) {
		this.pw = pw;
		this.trainingDocumentProvider = trainingDocumentProvider;
		this.testDocumentProvider = testDocumentProvider;
		this.iterations = iterations;
		this.solutionReporter = createSolutionReporter();
		this.random = random;
	}

	protected abstract LDAPerplexityResultReporter<T> createSolutionReporter();

	protected double[] createAlpha(double base, int topics) {
		return LDAGibbsSampler.symmetricAlpha(base / topics, topics);
	}

	protected double createBeta(double base, int topics) {
		return base;
	}

	protected LDAGibbsSampler<T> createSampler(double baseAlpha,
			double baseBeta, int topics) {
		return new LDAGibbsSampler<T>(trainingDocumentProvider, createAlpha(
				baseAlpha, topics), createBeta(baseBeta, topics), random);
	}
}
