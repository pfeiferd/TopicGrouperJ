package org.hhn.topicgrouper.eval.ecir2019;

import java.io.File;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.eval.Csv;
import org.hhn.topicgrouper.eval.Figures;
import org.hhn.topicgrouper.eval.ModelEvaluation;
import org.hhn.topicgrouper.eval.ModelEvaluation.LDAConfig;
import org.hhn.topicgrouper.eval.ModelEvaluation.Sink;
import org.hhn.topicgrouper.eval.ModelEvaluation.TGConfig;
import org.hhn.topicgrouper.eval.ModelEvaluation.TopicFilter;
import org.hhn.topicgrouper.eval.Settings;
import org.hhn.topicgrouper.eval.Split;
import org.hhn.topicgrouper.eval.TopicGrouperFactory;

/**
 * Hold-out perplexity over the number of topics (ECIR 2019 paper, the figures
 * of Section 5): Topic Grouper with the left-to-right estimator and an
 * optimized Dirichlet concentration (<code>tg</code>), Topic Grouper with the
 * simple smoothed estimator (<code>tg-smoothed</code>), LDA with the fixed
 * prior (<code>lda</code>, left-to-right estimate; <code>lda-avg</code>,
 * averaging estimate) and with re-estimated alpha and beta
 * (<code>lda-opt</code>), PLSA (<code>plsa</code>) on the smaller corpora and
 * the unigram model (<code>unigram</code>). The parameters per corpus are those
 * of the paper, see {@link Corpus}.
 */
public class PerplexityExperiment {
	/** The parameters of one corpus. */
	public static class Corpus {
		/** Name of the corpus (used in the file name). */
		public final String name;
		/** Training and test documents. */
		public final Split<DocumentProvider<String>> split;
		/** Initial Dirichlet concentration of the left-to-right estimator for Topic Grouper. */
		public double tgAlpha = 50;
		/** Share of the training documents the concentration is optimized on. */
		public double optimizeShare = 0.1;
		/** Topic numbers evaluated: 1 and the multiples of this step. */
		public int topicStep = 10;
		/** Particles of the left-to-right estimator (0: the setting's default). */
		public int particles;
		/** Also evaluate the smoothed estimator for Topic Grouper (fixed concentration 50). */
		public boolean withSmoothedTG = true;
		/** Also evaluate PLSA (feasible for small vocabularies only). */
		public boolean withPlsa;

		/**
		 * A corpus with the default parameters.
		 *
		 * @param name
		 *            name of the corpus
		 * @param split
		 *            training and test documents
		 */
		public Corpus(String name, Split<DocumentProvider<String>> split) {
			this.name = name;
			this.split = split;
		}

		/**
		 * Sets {@link #tgAlpha}.
		 *
		 * @param alpha
		 *            initial Dirichlet concentration
		 * @return this corpus with another initial concentration of Topic Grouper
		 */
		public Corpus tgAlpha(double alpha) {
			tgAlpha = alpha;
			return this;
		}

		/**
		 * Sets {@link #optimizeShare}.
		 *
		 * @param share
		 *            share of the training documents (0..1)
		 * @return this corpus with another share for the optimization
		 */
		public Corpus optimizeShare(double share) {
			optimizeShare = share;
			return this;
		}

		/**
		 * Sets {@link #topicStep}.
		 *
		 * @param step
		 *            the step between evaluated topic numbers
		 * @return this corpus with another topic step
		 */
		public Corpus topicStep(int step) {
			topicStep = step;
			return this;
		}

		/**
		 * Sets {@link #particles}.
		 *
		 * @param particles
		 *            particles of the left-to-right estimator
		 * @return this corpus with another number of particles
		 */
		public Corpus particles(int particles) {
			this.particles = particles;
			return this;
		}

		/**
		 * Sets {@link #withPlsa}.
		 *
		 * @return this corpus with PLSA included
		 */
		public Corpus withPlsa() {
			withPlsa = true;
			return this;
		}
	}

	private final Settings settings;
	private final File out;

	/**
	 * Creates the experiment.
	 *
	 * @param settings
	 *            experiment size
	 * @param out
	 *            output directory
	 */
	public PerplexityExperiment(Settings settings, File out) {
		this.settings = settings;
		this.out = out;
	}

	/**
	 * Runs all methods for one corpus and writes <code>perplexity-&lt;name&gt;.csv</code>
	 * with the columns <code>method;topics;perplexity;alphaConc</code>.
	 *
	 * @param c
	 *            the corpus and its parameters
	 */
	public void run(final Corpus c) {
		File file = new File(out, "perplexity-" + c.name + ".csv");
		if (Csv.skipIfDone(file)) {
			return;
		}
		final Csv csv = new Csv(file, "method", "topics", "perplexity", "alphaConc");
		try {
			Sink sink = new Sink() {
				public void accept(String method, int topics, double perplexity, double param, double errorRate) {
					csv.row(method, topics, perplexity, param);
				}
			};
			TopicFilter grid = new TopicFilter() {
				public boolean accept(int topics) {
					return topics <= settings.maxTopics && (topics == 1 || topics % c.topicStep == 0);
				}
			};
			ModelEvaluation evaluation = new ModelEvaluation(c.split, settings);
			evaluation.unigram(sink);

			TGConfig lr = new TGConfig(settings);
			lr.alpha = c.tgAlpha;
			lr.optimizeShare = c.optimizeShare;
			if (c.particles > 0) {
				lr.particles = c.particles;
			}
			TGConfig[] configs;
			if (c.withSmoothedTG) {
				TGConfig smoothed = new TGConfig();
				smoothed.method = "tg-smoothed";
				smoothed.smoothed = true;
				configs = new TGConfig[] { lr, smoothed };
			} else {
				configs = new TGConfig[] { lr };
			}
			evaluation.topicGrouper(configs, grid, sink, new TopicGrouperFactory().create(c.split.train));

			LDAConfig lda = new LDAConfig();
			lda.withAveraging = true;
			evaluation.lda(lda, grid, sink);
			LDAConfig opt = new LDAConfig("lda-opt", true, true);
			opt.withAveraging = true;
			evaluation.lda(opt, grid, sink);

			if (c.withPlsa) {
				evaluation.plsa(new TopicFilter() {
					public boolean accept(int topics) {
						return topics <= settings.plsaMaxTopics && (topics == 1 || topics % c.topicStep == 0);
					}
				}, sink);
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
