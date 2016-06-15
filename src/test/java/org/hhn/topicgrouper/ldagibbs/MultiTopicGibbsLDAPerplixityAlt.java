package org.hhn.topicgrouper.ldagibbs;

import org.hhn.topicgrouper.base.DocumentProvider;

public class MultiTopicGibbsLDAPerplixityAlt extends AbstractMultiTopicGibbsLDA {
	public MultiTopicGibbsLDAPerplixityAlt(
			DocumentProvider<String> trainingDocumentProvider,
			DocumentProvider<String> testDocumentProvider) {
		super(trainingDocumentProvider, testDocumentProvider);
	}

	@Override
	protected double[] createAlpha(int topics) {
		// Setting alpha and beta according to this forum entry:
		// http://stats.stackexchange.com/questions/59684/what-are-typical-values-to-use-for-alpha-and-beta-in-latent-dirichlet-allocation
		// More specifically, according to
		// "Finding scientific topics" by Thomas L. Griffiths and Mark Steyvers
		return AbstractGibbsSamplingLDAWithPerplexity.symmetricAlpha(
				0.001d / topics, topics);
	}

	@Override
	protected double createBeta(int topics) {
		return 0.1d; // 200d / getTrainingDocumentProvider().getNumberOfWords();
	}

	@Override
	protected AbstractGibbsSamplingLDAWithPerplexity createSampler(int topics,
			double[] alpha, double beta, int iterations) throws Exception {
		return new GibbsSamplingLDAWithPerplexityAlt(getSolutionReporter(),
				getTrainingDocumentProvider(), alpha, beta, iterations, 10,
				createFileName(topics, alpha, beta, iterations), "", 0,
				getTestDocumentProvider(), iterations);
	}

	protected String createFileName(int topics, double[] alpha, double beta,
			int iterations) {
		return "MultiTopicGibbsLDAPerplixityAlt_" + topics;
	}
}
