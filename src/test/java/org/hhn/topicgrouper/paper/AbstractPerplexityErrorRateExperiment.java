package org.hhn.topicgrouper.paper;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorAlt;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public abstract class AbstractPerplexityErrorRateExperiment<T> {
	protected final Random random;
	protected final AbstractLDAPerplexityCalculator<T> calc1;
	protected final TGPerplexityCalculator<T> perplexityCalculator;

	public AbstractPerplexityErrorRateExperiment(Random random) {
		this.random = random;
		calc1 = new LDAPerplexityCalculatorAlt<T>(false);
		perplexityCalculator = new TGPerplexityCalculator<T>(false);
	}

	public void run(int gibbsIterations, int steps, int avgC)
			throws IOException {
		PrintStream pw = prepareLDAPrintStream();
		PrintStream pw2 = prepareTGPrintStream();
		PrintStream pw3 = prepareTGLikelihoodPrintStream();

		double[] perplexity1 = new double[avgC];
		double[] perplexity2 = new double[avgC];
		double[] acc = new double[avgC];

		double[] tgAcc = new double[avgC];
		double[] tgPerplexity = new double[avgC];

		for (int i = 0; i < steps; i++) {
			System.out.print("Step: ");
			System.out.println(i);
			for (int j = 0; j < avgC; j++) {
				System.out.print("Repeat: ");
				System.out.println(j);
				DocumentProvider<T> documentProvider = createDocumentProvider(i);
				HoldOutSplitter<T> holdOutSplitter = createHoldoutSplitter(i,
						documentProvider);

				runTopicGrouper(pw3, i, j, holdOutSplitter.getRest(),
						holdOutSplitter.getHoldOut(), tgPerplexity, tgAcc);
				
				runLDAGibbsSampler(i, j, gibbsIterations,
						holdOutSplitter.getRest(),
						holdOutSplitter.getHoldOut(), perplexity1, perplexity2,
						acc);
			}
			aggregateLDAResults(pw, i, perplexity1, perplexity2, acc);
			aggregateTGResults(pw2, i, tgPerplexity, tgAcc);
		}
		if (pw != null) {
			pw.close();
		}
		if (pw2 != null) {
			pw2.close();
		}
		if (pw3 != null) {
			pw3.close();
		}
	}

	protected HoldOutSplitter<T> createHoldoutSplitter(int step,
			DocumentProvider<T> documentProvider) {
		return new HoldOutSplitter<T>(random, documentProvider, 0.3333, 1);
	}

	protected abstract DocumentProvider<T> createDocumentProvider(int step);

	protected abstract LDAGibbsSampler<T> createGibbsSampler(int step,
			DocumentProvider<T> documentProvider);

	protected abstract void runLDAGibbsSampler(int step, int repeat,
			int gibbsIterations, final DocumentProvider<T> documentProvider,
			final DocumentProvider<T> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] acc);

	protected abstract PrintStream prepareLDAPrintStream() throws IOException;

	protected abstract PrintStream prepareTGPrintStream() throws IOException;

	protected abstract PrintStream prepareTGLikelihoodPrintStream()
			throws IOException;

	protected abstract void runTopicGrouper(PrintStream pw3, int step,
			int repeat, DocumentProvider<T> documentProvider,
			DocumentProvider<T> testDocumentProvider, double[] tgPerplexity,
			double[] tgAcc);

	protected abstract void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] acc);

	protected abstract void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc);
}
