package org.hhn.topicgrouper.evaluation;

import java.io.File;

import org.hhn.topicgrouper.evaluation.PerplexityExperiment.Sink;
import org.hhn.topicgrouper.evaluation.PerplexityExperiment.TopicFilter;

/**
 * Perplexity and topic error rate on the synthetic corpus (which has exactly 4
 * true topics) when the corpus size or the skew of the topic distribution
 * changes.
 */
public final class TwcSeriesExperiment {
	private TwcSeriesExperiment() {
	}

	private static final TopicFilter FOUR_TOPICS = new TopicFilter() {
		public boolean accept(int topics) {
			return topics == 4;
		}
	};

	/**
	 * Varies the number of documents from 1/steps of the full corpus to the
	 * full corpus. Writes <code>twc-docs.csv</code> with the columns
	 * <code>docs;method;perplexity;errorRate</code>.
	 * 
	 * @param s
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public static void runDocs(final Settings s, File out) {
		final Csv csv = new Csv(new File(out, "twc-docs.csv"), "docs", "method", "perplexity", "errorRate");
		try {
			for (int step = 1; step <= s.twcSteps; step++) {
				final int docs = s.twcDocs * step / s.twcSteps;
				runModels(Datasets.twc(docs), s, new Sink() {
					public void accept(String method, int topics, double perplexity, double param, double errorRate) {
						csv.row(docs, method, perplexity, errorRate);
					}
				});
			}
		} finally {
			csv.close();
		}
	}

	/**
	 * Varies the Dirichlet parameter alpha1 of the first topic (the other three stay
	 * 0.5), i.e. how unbalanced the topics are. Writes <code>twc-alpha.csv</code>
	 * with the columns <code>alpha1;method;perplexity;errorRate</code>.
	 * 
	 * @param s
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public static void runAlpha(final Settings s, File out) {
		final Csv csv = new Csv(new File(out, "twc-alpha.csv"), "alpha1", "method", "perplexity", "errorRate");
		try {
			for (int step = 0; step < s.twcSteps; step++) {
				// 0.5, 1.5, 2.5, ... up to roughly 10 for the full setup.
				final double alpha1 = 0.5 + step * (s.twcSteps >= 20 ? 0.5 : 2.0);
				runModels(Datasets.twc(s.twcDocs, alpha1, 42), s, new Sink() {
					public void accept(String method, int topics, double perplexity, double param, double errorRate) {
						csv.row(alpha1, method, perplexity, errorRate);
					}
				});
			}
		} finally {
			csv.close();
		}
	}

	private static void runModels(Datasets.Twc twc, Settings s, Sink sink) {
		PerplexityExperiment.runUnigram(twc.split, s, sink);
		PerplexityExperiment.runTG(twc.split, s, FOUR_TOPICS, twc.generator, sink);
		PerplexityExperiment.runLDA(twc.split, s, FOUR_TOPICS, twc.generator, sink);
		PerplexityExperiment.runPlsa(twc.split, s, FOUR_TOPICS, sink);
	}
}
