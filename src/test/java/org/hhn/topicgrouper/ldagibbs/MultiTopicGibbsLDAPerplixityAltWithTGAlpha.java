package org.hhn.topicgrouper.ldagibbs;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.SolutionListenerMultiplexer;
import org.hhn.topicgrouper.report.BasicSolutionReporter;
import org.hhn.topicgrouper.report.TopicFrCollectSolutionReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;

public class MultiTopicGibbsLDAPerplixityAltWithTGAlpha extends
		MultiTopicGibbsLDAPerplixityAlt {
	public MultiTopicGibbsLDAPerplixityAltWithTGAlpha(
			DocumentProvider<String> trainingDocumentProvider,
			DocumentProvider<String> testDocumentProvider) {
		super(trainingDocumentProvider, testDocumentProvider);
	}

	private int[][] frequenciesPerNTopicsStore;

	@Override
	public void inference(int minTopic, int stepSize, int steps, int iterations)
			throws Exception {
		OptimizedTopicGrouper<String> tg = new OptimizedTopicGrouper<String>(1,
				0, getTrainingDocumentProvider(), 1);
		SolutionListenerMultiplexer<String> mp = new SolutionListenerMultiplexer<String>();
		TopicFrCollectSolutionReporter<String> r = new TopicFrCollectSolutionReporter<String>();
		mp.addSolutionListener(r);
		mp.addSolutionListener(new BasicSolutionReporter<String>(System.out, 100, true));
		tg.solve(mp);

		frequenciesPerNTopicsStore = r.getFrequenciesPerNTopics();

		super.inference(minTopic, stepSize, steps, iterations);
	}

	@Override
	protected double[] createAlpha(int topics) {
		double[] normalized = new double[topics];
		int sum = 0;
		for (int i = 0; i < normalized.length; i++) {
			sum += frequenciesPerNTopicsStore[topics - 1][i];
		}
		for (int i = 0; i < normalized.length; ++i) {
			normalized[i] = ((double) frequenciesPerNTopicsStore[topics - 1][i])
					/ sum;
		}
		return reworkAlpha(normalized);
	}

	protected double[] reworkAlpha(double[] alpha) {
		for (int i = 0; i < alpha.length; i++) {
			alpha[i] *= 50;
		}
		return alpha;
	}
	
	protected String createFileName(int topics, double[] alpha, double beta,
			int iterations) {
		return "MultiTopicGibbsLDAPerplixityAltWithAlpha_" + topics;
	}

}
