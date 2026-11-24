package org.hhn.topicgrouper.eval.homonyms;

import java.io.File;
import java.io.IOException;

import org.hhn.topicgrouper.eval.Csv;
import org.hhn.topicgrouper.eval.Figures;
import org.hhn.topicgrouper.eval.Datasets;
import org.hhn.topicgrouper.eval.ModelEvaluation;
import org.hhn.topicgrouper.eval.ModelEvaluation.LDAConfig;
import org.hhn.topicgrouper.eval.ModelEvaluation.Sink;
import org.hhn.topicgrouper.eval.ModelEvaluation.TGConfig;
import org.hhn.topicgrouper.eval.ModelEvaluation.TopicFilter;
import org.hhn.topicgrouper.eval.Settings;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.SenseSplittingTopicGrouper;

/**
 * Does handling of homonyms help? Compares plain Topic Grouper with Topic
 * Grouper that can give words several senses (see
 * {@link SenseSplittingTopicGrouper} and <code>doc/topic-grouper.md</code>,
 * Section 6) and LDA on synthetic corpora with an increasing number of
 * homonyms of 2 to 4 senses. All are evaluated at the true number of topics
 * (4); the error rate compares the learned with the true word distributions
 * of the topics, including the homonym tokens.
 */
public class HomonymExperiment {
	private static final TopicFilter FOUR_TOPICS = new TopicFilter() {
		public boolean accept(int topics) {
			return topics == 4;
		}
	};

	private final Datasets datasets;
	private final Settings settings;
	private final File out;

	/**
	 * Creates the experiment.
	 *
	 * @param datasets
	 *            the data sets (for the synthetic corpus)
	 * @param settings
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public HomonymExperiment(Datasets datasets, Settings settings, File out) {
		this.datasets = datasets;
		this.settings = settings;
		this.out = out;
	}

	/**
	 * Writes <code>homonyms.csv</code> with the columns
	 * <code>homonyms;method;perplexity;errorRate;splits</code> (methods:
	 * <code>tg</code>, <code>tg-sense</code> with the defaults of
	 * {@link SenseSplittingTopicGrouper}, <code>lda</code> and
	 * <code>lda-alpha</code>, LDA with an optimized alpha vector).
	 */
	public void run() {
		File file = new File(out, "homonyms.csv");
		if (Csv.skipIfDone(file)) {
			return;
		}
		final Csv csv = new Csv(file, "homonyms", "method", "perplexity", "errorRate", "splits");
		try {
			for (final int homonyms : settings.homonymCounts) {
				Datasets.Twc twc = datasets.twcWithHomonyms(settings.twcDocs, homonyms, 42);
				ModelEvaluation evaluation = new ModelEvaluation(twc.split, settings, twc.generator);
				Sink sink = new Sink() {
					public void accept(String method, int topics, double perplexity, double param, double errorRate) {
						csv.row(homonyms, method, perplexity, errorRate, 0);
					}
				};
				evaluation.topicGrouper(new TGConfig(settings), FOUR_TOPICS, sink,
						new EHACTopicGrouper<String>(1, twc.split.train, 1));
				final SenseSplittingTopicGrouper<String> sense = new SenseSplittingTopicGrouper<String>(1,
						twc.split.train, 1);
				TGConfig senseConfig = new TGConfig(settings);
				senseConfig.method = "tg-sense";
				// A word may have several senses and thus topics: the closed form does not apply.
				senseConfig.leftToRight = true;
				evaluation.topicGrouper(senseConfig, FOUR_TOPICS, new Sink() {
					public void accept(String method, int topics, double perplexity, double param, double errorRate) {
						csv.row(homonyms, method, perplexity, errorRate, sense.getNumberOfSplits());
					}
				}, sense);
				evaluation.lda(FOUR_TOPICS, sink);
				evaluation.lda(new LDAConfig("lda-alpha", true, false), FOUR_TOPICS, sink);
			}
		} catch (RuntimeException | Error e) {
			csv.discard();
			throw e;
		}
		csv.close();
		plot(file);
	}

	/**
	 * Draws the figures of a finished CSV file into <code>figs</code>.
	 *
	 * @param csv
	 *            the CSV file
	 */
	private void plot(File csv) {
		try {
			new Figures(out).plot(csv);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
