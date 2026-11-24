package org.hhn.topicgrouper.eval.ecir2019;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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
import org.hhn.topicgrouper.util.MathExt;

/**
 * The series on the synthetic corpus of the ECIR 2019 paper (4 topics of 100
 * words, 6000 documents of 30 words, Dirichlet prior (5, 0.5, 0.5, 0.5)): the
 * perplexity and the error rate against the true topics at 4 topics, (a) over
 * the prior LDA is given (the data are fixed; Topic Grouper, PLSA, the unigram
 * model and the "perfect" model are constant references) and (b) over the
 * number of documents. The random methods are repeated several times and
 * reported with the mean and the sample standard deviation.
 */
public class TwcSeriesExperiment {
	/** The Dirichlet concentration of Topic Grouper's estimator and of the LDA prior in the alpha series. */
	private static final double ALPHA_CONC = 6.5;
	/** Beta of LDA in the series (re-estimated during burn-in). */
	private static final double BETA = 4;
	/** Documents per step of the document series. */
	private static final int DOCS_PER_STEP = 500;
	/** Words per topic of the synthetic corpus. */
	private static final int WORDS_PER_TOPIC = 100;

	private static final TopicFilter FOUR_TOPICS = new TopicFilter() {
		public boolean accept(int topics) {
			return topics == 4;
		}
	};

	/** Collects the results of the repeated runs of one method. */
	private static final class Repeated implements Sink {
		private final double[] perplexity;
		private final double[] errorRate;
		private int n;

		Repeated(int tries) {
			perplexity = new double[tries];
			errorRate = new double[tries];
		}

		public void accept(String method, int topics, double p, double param, double err) {
			perplexity[n] = p;
			errorRate[n] = err;
			n++;
		}

		void row(Csv csv, Object x, String method) {
			double[] p = Arrays.copyOf(perplexity, n);
			double[] e = Arrays.copyOf(errorRate, n);
			csv.row(x, method, MathExt.avg(p), n > 1 ? MathExt.sampleStdDev(p) : 0,
					MathExt.avg(e), n > 1 ? MathExt.sampleStdDev(e) : 0, n);
		}
	}

	/** LDA with a fixed prior vector, beta 4 re-estimated, and the seed of the try. */
	private static final class FixedPriorLDA extends LDAConfig {
		private final double[] prior;

		FixedPriorLDA(double[] prior, int seed) {
			this.prior = prior;
			this.beta = BETA;
			this.updateBeta = true;
			this.seed = seed;
		}

		@Override
		public double[] alpha(int topics) {
			return prior.clone();
		}
	}

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
	public TwcSeriesExperiment(Datasets datasets, Settings settings, File out) {
		this.datasets = datasets;
		this.settings = settings;
		this.out = out;
	}

	/**
	 * (a) LDA's prior: alpha_1 = 6.5 i / (3 + i), the other three 6.5 / (3 + i),
	 * for i = 1 .. steps (so the concentration is 6.5 throughout); writes
	 * <code>twc-alpha.csv</code> with the columns
	 * <code>alpha1;method;perplexity;perplexityStdDev;errorRate;errorRateStdDev;tries</code>.
	 * The constant references are repeated for every alpha_1.
	 */
	public void runAlpha() {
		File alphaFile = new File(out, "twc-alpha.csv");
		if (Csv.skipIfDone(alphaFile)) {
			return;
		}
		Csv csv = new Csv(alphaFile, "alpha1", "method", "perplexity",
				"perplexityStdDev", "errorRate", "errorRateStdDev", "tries");
		try {
			Datasets.Twc twc = datasets.twc(settings.twcDocs);
			ModelEvaluation evaluation = new ModelEvaluation(twc.split, settings, twc.generator);
			Repeated tg = new Repeated(1);
			evaluation.topicGrouper(fixedAlphaTG(), FOUR_TOPICS, tg, new EHACTopicGrouper<String>(1, twc.split.train, 1));
			Repeated tgSense = new Repeated(1);
			evaluation.topicGrouper(fixedAlphaTGSense(), FOUR_TOPICS, tgSense,
					new SenseSplittingTopicGrouper<String>(1, twc.split.train, 1));
			Repeated unigram = new Repeated(1);
			evaluation.unigram(unigram);
			Repeated perfect = new Repeated(1);
			evaluation.perfect(WORDS_PER_TOPIC, perfect);
			Repeated plsa = new Repeated(settings.twcTries);
			for (int t = 0; t < settings.twcTries; t++) {
				evaluation.plsa(t, FOUR_TOPICS, plsa);
			}
			for (int i = 1; i <= settings.twcAlphaSteps; i++) {
				double sum = 3 + i;
				double alpha1 = ALPHA_CONC * i / sum;
				double[] prior = new double[4];
				Arrays.fill(prior, ALPHA_CONC / sum);
				prior[0] = alpha1;
				Repeated lda = new Repeated(settings.twcTries);
				for (int t = 0; t < settings.twcTries; t++) {
					evaluation.lda(new FixedPriorLDA(prior, t), FOUR_TOPICS, lda);
				}
				lda.row(csv, alpha1, "lda");
				tg.row(csv, alpha1, "tg");
				tgSense.row(csv, alpha1, "tg-sense");
				plsa.row(csv, alpha1, "plsa");
				unigram.row(csv, alpha1, "unigram");
				perfect.row(csv, alpha1, "perfect");
			}
		} catch (RuntimeException | Error e) {
			csv.discard();
			throw e;
		}
		csv.close();
		plot(alphaFile);
	}

	/**
	 * (b) The number of documents, 500 per step: Topic Grouper and LDA with the
	 * true prior (5, 0.5, 0.5, 0.5) and beta 4 re-estimated; writes
	 * <code>twc-docs.csv</code> with the columns
	 * <code>docs;method;perplexity;perplexityStdDev;errorRate;errorRateStdDev;tries</code>.
	 */
	public void runDocs() {
		File docsFile = new File(out, "twc-docs.csv");
		if (Csv.skipIfDone(docsFile)) {
			return;
		}
		Csv csv = new Csv(docsFile, "docs", "method", "perplexity",
				"perplexityStdDev", "errorRate", "errorRateStdDev", "tries");
		try {
			for (int step = 0; step < settings.twcDocsSteps; step++) {
				int docs = (step + 1) * DOCS_PER_STEP;
				Datasets.Twc twc = datasets.twc(docs);
				ModelEvaluation evaluation = new ModelEvaluation(twc.split, settings, twc.generator);
				Repeated tg = new Repeated(1);
				evaluation.topicGrouper(fixedAlphaTG(), FOUR_TOPICS, tg, new EHACTopicGrouper<String>(1, twc.split.train, 1));
				tg.row(csv, docs, "tg");
				Repeated tgSense = new Repeated(1);
				evaluation.topicGrouper(fixedAlphaTGSense(), FOUR_TOPICS, tgSense,
						new SenseSplittingTopicGrouper<String>(1, twc.split.train, 1));
				tgSense.row(csv, docs, "tg-sense");
				Repeated lda = new Repeated(settings.twcTries);
				for (int t = 0; t < settings.twcTries; t++) {
					evaluation.lda(new FixedPriorLDA(new double[] { 5, 0.5, 0.5, 0.5 }, t), FOUR_TOPICS, lda);
				}
				lda.row(csv, docs, "lda");
				Repeated unigram = new Repeated(1);
				evaluation.unigram(unigram);
				unigram.row(csv, docs, "unigram");
			}
		} catch (RuntimeException | Error e) {
			csv.discard();
			throw e;
		}
		csv.close();
		plot(docsFile);
	}

	/** Topic Grouper at the fixed concentration 6.5 (no optimization). */
	private TGConfig fixedAlphaTG() {
		TGConfig c = new TGConfig(settings);
		c.alpha = ALPHA_CONC;
		c.optimizeShare = 0;
		return c;
	}

	/** Topic Grouper with word senses at the fixed concentration 6.5 (left-to-right sampler). */
	private TGConfig fixedAlphaTGSense() {
		TGConfig c = fixedAlphaTG();
		c.method = "tg-sense";
		c.leftToRight = true;
		return c;
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
