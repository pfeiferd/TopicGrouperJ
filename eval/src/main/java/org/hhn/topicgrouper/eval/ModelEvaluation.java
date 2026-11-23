package org.hhn.topicgrouper.eval;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.data.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.plsa.impl.PLSA;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.validation.TGLRPerplexityCalculator;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.AbstractTopicModelerWithProvider;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PerplexityCalculatorAveraging;
import org.hhn.topicgrouper.validation.PerplexityCalculatorLeftToRight;
import org.hhn.topicgrouper.validation.UnigramModeler;

/**
 * Evaluates topic models on one corpus (a training and a test part): Topic
 * Grouper, LDA, PLSA, the unigram baseline and, for the synthetic corpus, the
 * "perfect" model. It measures the hold-out perplexity and, if the corpus is
 * synthetic (a generator is given), the error rate against the true topics at
 * 4 topics. Every result is handed to a {@link Sink}; the experiments decide
 * what to write.
 */
public class ModelEvaluation {
	/** Receives one result. */
	public interface Sink {
		/**
		 * Receives one result.
		 *
		 * @param method
		 *            name of the method
		 * @param topics
		 *            number of topics
		 * @param perplexity
		 *            perplexity on the test documents
		 * @param param
		 *            a parameter of the method (the Dirichlet concentration of
		 *            Topic Grouper), NaN if none
		 * @param errorRate
		 *            error rate against the true topics (synthetic data), NaN if
		 *            not computed
		 */
		void accept(String method, int topics, double perplexity, double param, double errorRate);
	}

	/** Selects the topic numbers to evaluate. */
	public interface TopicFilter {
		/**
		 * Decides whether a number of topics is evaluated.
		 *
		 * @param topics
		 *            number of topics
		 * @return whether to evaluate it
		 */
		boolean accept(int topics);
	}

	/**
	 * How Topic Grouper solutions are evaluated. By default (the paper's "TG-LR")
	 * the left-to-right estimator is used and the Dirichlet concentration of
	 * p(t|d) is optimized on a part of the training documents for every
	 * solution; with <code>optimizeShare</code> 0 the concentration stays at
	 * <code>alpha</code>; with <code>smoothed</code> the simpler estimator with an
	 * explicit, smoothed p(t|d) is used instead.
	 */
	public static class TGConfig {
		/** Name of the method in the results. */
		public String method = "tg";
		/** Use the smoothed p(t|d) estimator instead of the left-to-right one. */
		public boolean smoothed;
		/** Initial (or, without optimization, fixed) Dirichlet concentration. */
		public double alpha = 50;
		/** Particles of the left-to-right estimator. */
		public int particles = 20;
		/** Share of the training documents the concentration is optimized on (0: none). */
		public double optimizeShare = 0.1;
		/** Refinement steps of the optimization. */
		public int optimizeSteps = 10;

		/** The paper's default configuration. */
		public TGConfig() {
		}

		/**
		 * The paper's default configuration with the sizes of the settings.
		 *
		 * @param s
		 *            the settings
		 */
		public TGConfig(Settings s) {
			particles = s.particles;
			optimizeSteps = s.alphaOptSteps;
		}
	}

	/**
	 * How LDA is trained: the prior per topic number, whether alpha and beta are
	 * re-estimated, the seed of the sampler and whether the averaging estimate of
	 * the perplexity is reported as well (as method <code>&lt;method&gt;-avg</code>).
	 */
	public static class LDAConfig {
		/** Name of the method in the results. */
		public String method = "lda";
		/** Beta (symmetric). */
		public double beta = 0.1;
		/** Re-estimate alpha during burn-in. */
		public boolean updateAlpha;
		/** Re-estimate beta during burn-in. */
		public boolean updateBeta;
		/** Seed of the sampler. */
		public int seed = 42;
		/** Report the averaging perplexity estimate as well. */
		public boolean withAveraging;

		/** The paper's default configuration: symmetric alpha with concentration 50 / topics, beta 0.1. */
		public LDAConfig() {
		}

		/**
		 * A named configuration with the paper's prior that optionally re-estimates
		 * alpha and beta.
		 *
		 * @param method
		 *            name of the method
		 * @param updateAlpha
		 *            re-estimate alpha
		 * @param updateBeta
		 *            re-estimate beta
		 */
		public LDAConfig(String method, boolean updateAlpha, boolean updateBeta) {
			this.method = method;
			this.updateAlpha = updateAlpha;
			this.updateBeta = updateBeta;
		}

		/**
		 * The symmetric alpha prior with concentration 50 for a number of topics.
		 *
		 * @param topics
		 *            number of topics
		 * @return the prior
		 */
		public double[] alpha(int topics) {
			return LDAGibbsSampler.symmetricAlpha(50d / topics, topics);
		}
	}

	private final Split<DocumentProvider<String>> split;
	private final Settings settings;
	private final TWCLDAPaperDocumentGenerator generator;

	/**
	 * Evaluation on a corpus that may be synthetic.
	 *
	 * @param split
	 *            training and test documents
	 * @param settings
	 *            experiment size
	 * @param generator
	 *            the generator of the corpus if it is synthetic (for the error
	 *            rate), else null
	 */
	public ModelEvaluation(Split<DocumentProvider<String>> split, Settings settings,
			TWCLDAPaperDocumentGenerator generator) {
		this.split = split;
		this.settings = settings;
		this.generator = generator;
	}

	/**
	 * Evaluation on a real corpus (no error rate).
	 *
	 * @param split
	 *            training and test documents
	 * @param settings
	 *            experiment size
	 */
	public ModelEvaluation(Split<DocumentProvider<String>> split, Settings settings) {
		this(split, settings, null);
	}

	/**
	 * Returns the corpus the models are evaluated on.
	 *
	 * @return training and test documents
	 */
	public Split<DocumentProvider<String>> getSplit() {
		return split;
	}

	/**
	 * The unigram model (all topics identical to the word frequency distribution).
	 *
	 * @param sink
	 *            receives the result (method <code>unigram</code>, 1 topic)
	 */
	public void unigram(Sink sink) {
		UnigramModeler<String> modeler = new UnigramModeler<String>(split.train, 1);
		sink.accept("unigram", 1, perplexity(modeler), Double.NaN, Double.NaN);
	}

	/**
	 * Topic Grouper with the default configuration: the solver runs once down to
	 * a single topic, the solutions of the selected topic numbers are evaluated on
	 * the way.
	 *
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number
	 */
	public void topicGrouper(TopicFilter filter, Sink sink) {
		topicGrouper(new TGConfig(settings), filter, sink, new TopicGrouperFactory().create(split.train));
	}

	/**
	 * Topic Grouper with the given solver and configuration.
	 *
	 * @param c
	 *            how the solutions are evaluated
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number
	 * @param solver
	 *            the solver (on the training documents), run once
	 */
	public void topicGrouper(TGConfig c, TopicFilter filter, Sink sink, TGSolver<String> solver) {
		topicGrouper(new TGConfig[] { c }, filter, sink, solver);
	}

	/**
	 * Topic Grouper with the given solver, every solution evaluated with each of
	 * the configurations (one solver run).
	 *
	 * @param configs
	 *            the configurations, each with its own method name
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number and configuration
	 * @param solver
	 *            the solver (on the training documents), run once
	 */
	@SuppressWarnings("unchecked")
	public void topicGrouper(final TGConfig[] configs, final TopicFilter filter, final Sink sink,
			TGSolver<String> solver) {
		final DocumentProvider<String>[] optimizeOn = new DocumentProvider[configs.length];
		final TGPerplexityCalculator<String>[] calcs = new TGPerplexityCalculator[configs.length];
		for (int i = 0; i < configs.length; i++) {
			final TGConfig c = configs[i];
			optimizeOn[i] = c.optimizeShare > 0 && !c.smoothed
					? new HoldOutSplitter<String>(new Random(10), split.train, c.optimizeShare, 0, false,
							(DefaultVocab<String>) split.train.getVocab()).getHoldOut()
					: null;
			calcs[i] = c.smoothed
					? new TGPerplexityCalculator<String>(false, new DefaultDocumentSplitter<String>(), c.alpha)
					: new TGLRPerplexityCalculator<String>(new Random(42), false,
							new DefaultDocumentSplitter<String>(), c.alpha) {
						@Override
						protected int initParticles() {
							return c.particles;
						}
					};
			calcs[i].init();
		}
		solver.solve(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size, TGSolution<String> solution) {
				int n = solution.getNumberOfTopics();
				if (!filter.accept(n)) {
					return;
				}
				for (int i = 0; i < configs.length; i++) {
					TGPerplexityCalculator<String> calc = calcs[i];
					calc.setSolution(solution);
					if (optimizeOn[i] != null) {
						System.out.println(configs[i].method + ", " + n
								+ " topics: optimizing the alpha concentration on "
								+ optimizeOn[i].getDocuments().size()
								+ " documents (steps left, interval, -perplexity):");
						calc.optimizeAlphaConc(1, 250, optimizeOn[i], configs[i].optimizeSteps);
					} else {
						System.out.println(configs[i].method + ", " + n + " topics");
					}
					double p = calc.computePerplexity(split.test);
					double err = Double.NaN;
					if (generator != null && n == 4) {
						err = errorRate(calc.getPerplexityCalculator().getTopicModeler());
					}
					sink.accept(configs[i].method, n, p, calc.getAlphaConc(), err);
				}
			}

			public void beforeInitialization(int maxTopics, int documents) {
			}

			public void initalizing(double percentage) {
			}

			public void initialized(TGSolution<String> initialSolution) {
			}

			public void done() {
			}
		});
	}

	/**
	 * LDA with the paper's prior, a new model for every selected topic number.
	 *
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number
	 */
	public void lda(TopicFilter filter, Sink sink) {
		lda(new LDAConfig(), filter, sink);
	}

	/**
	 * LDA with the given configuration, a new model for every selected topic number.
	 *
	 * @param c
	 *            how LDA is trained
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number
	 */
	public void lda(LDAConfig c, TopicFilter filter, Sink sink) {
		for (int topics = 1; topics <= Math.max(settings.maxTopics, 4); topics++) {
			if (filter.accept(topics)) {
				System.out.println(c.method + ", " + topics + " topics: training ("
						+ settings.ldaBurnIn + " burn-in + " + settings.ldaIterations + " iterations)");
				evaluate(c.method, trainLDA(c, topics), c.withAveraging, sink);
			}
		}
	}

	/**
	 * Trains one LDA model on the training documents.
	 *
	 * @param c
	 *            how LDA is trained
	 * @param topics
	 *            number of topics
	 * @return the trained sampler
	 */
	public LDAGibbsSampler<String> trainLDA(LDAConfig c, int topics) {
		LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(new Random(c.seed), split.train,
				c.alpha(topics), c.beta);
		lda.setUpdateAlpha(c.updateAlpha);
		lda.setUpdateBeta(c.updateBeta);
		if (c.updateAlpha || c.updateBeta) {
			lda.setAlphaBetaUpdate(Math.max(1, settings.ldaBurnIn / 20));
		}
		lda.solve(settings.ldaBurnIn, settings.ldaIterations, null);
		return lda;
	}

	/**
	 * PLSA (EM) with the default seed, a new model for every selected topic number.
	 *
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number
	 */
	public void plsa(TopicFilter filter, Sink sink) {
		plsa(42, filter, sink);
	}

	/**
	 * PLSA with the given seed.
	 *
	 * @param seed
	 *            random seed of the initialization
	 * @param filter
	 *            selects the topic numbers to evaluate
	 * @param sink
	 *            receives one result per selected topic number
	 */
	public void plsa(int seed, TopicFilter filter, Sink sink) {
		for (int topics = 1; topics <= Math.max(settings.plsaMaxTopics, 4); topics++) {
			if (filter.accept(topics)) {
				System.out.println("plsa, " + topics + " topics: training (" + settings.plsaIterations
						+ " iterations)");
				PLSA<String> plsa = new PLSA<String>(new Random(seed), split.train, topics, 50);
				plsa.train(settings.plsaIterations);
				evaluate("plsa", plsa, false, sink);
			}
		}
	}

	/**
	 * The "perfect" model of the synthetic corpus: the true topic of every word
	 * (word index / words per topic), p(w|t) from the training frequencies and
	 * the topic prior from the topic masses with concentration 6.5.
	 *
	 * @param wordsPerTopic
	 *            number of words per topic of the generator
	 * @param sink
	 *            receives the result (method <code>perfect</code>)
	 */
	public void perfect(int wordsPerTopic, Sink sink) {
		evaluate("perfect", new PerfectTWCModeler(split.train, 4, wordsPerTopic), false, sink);
	}

	/**
	 * Perplexity (left-to-right) and, on synthetic data at 4 topics, the error
	 * rate of a trained model; optionally also the averaging estimate (method
	 * <code>&lt;method&gt;-avg</code>).
	 *
	 * @param method
	 *            name of the method in the results
	 * @param modeler
	 *            the trained model
	 * @param withAveraging
	 *            whether the averaging perplexity estimate is reported as well
	 * @param sink
	 *            receives the result(s)
	 */
	public void evaluate(String method, AbstractTopicModeler<String> modeler, boolean withAveraging, Sink sink) {
		int topics = modeler.getNTopics();
		double err = generator != null && topics == 4 ? errorRate(modeler) : Double.NaN;
		sink.accept(method, topics, perplexity(modeler), Double.NaN, err);
		if (withAveraging) {
			BasicPerplexityCalculator<String> calc = new PerplexityCalculatorAveraging<String>(false,
					new DefaultDocumentSplitter<String>());
			calc.setTopicModeler(modeler);
			sink.accept(method + "-avg", topics, calc.computePerplexity(split.test), Double.NaN, err);
		}
	}

	/**
	 * Left-to-right perplexity of a model on the test documents.
	 *
	 * @param modeler
	 *            the trained model
	 * @return the perplexity
	 */
	public double perplexity(AbstractTopicModeler<String> modeler) {
		BasicPerplexityCalculator<String> calc = new PerplexityCalculatorLeftToRight<String>(
				new Random(42), false, new DefaultDocumentSplitter<String>(), settings.particles);
		calc.setTopicModeler(modeler);
		return calc.computePerplexity(split.test);
	}

	/**
	 * Error rate of a model's topic-word distributions against the true ones of
	 * the generator (best assignment of the 4 topics).
	 *
	 * @param modeler
	 *            the trained model with 4 topics
	 * @return the error rate, 1 minus the accuracy
	 */
	public double errorRate(final AbstractTopicModeler<String> modeler) {
		PwtProvider<String> pwt = new PwtProvider<String>() {
			public double getPwtFromModel(int topic, int wordIndex) {
				return modeler.getPhi(topic, wordIndex);
			}

			public double getCorrectPwt(int topic, String word) {
				return generator.getPwt(topic, word);
			}
		};
		return 1 - new TrueTopicAccuracyCalculator<String>().computeAccuracy(split.train, 4, pwt);
	}

	private static final class PerfectTWCModeler extends AbstractTopicModelerWithProvider<String> {
		private final double[] alphaBase;

		PerfectTWCModeler(DocumentProvider<String> provider, int nTopics, int wordsPerTopic) {
			super(null, provider, nTopics);
			alphaBase = new double[nTopics];
			double sumAll = 0;
			for (int i = 0; i < nTopics; i++) {
				double sum = 0;
				for (int j = 0; j < nWords; j++) {
					int word = Integer.parseInt(provider.getVocab().getWord(j));
					if (word / wordsPerTopic == i) {
						phi[i][j] = provider.getWordFrequency(j);
						sum += phi[i][j];
					}
				}
				alphaBase[i] = sum;
				sumAll += sum;
				for (int j = 0; j < nWords; j++) {
					phi[i][j] = phi[i][j] / sum;
				}
			}
			for (int i = 0; i < nTopics; i++) {
				alphaBase[i] = alphaBase[i] / sumAll;
			}
		}

		@Override
		public double getAlpha(int i) {
			return getAlphaConc() * alphaBase[i];
		}

		@Override
		public double getAlphaConc() {
			return 6.5;
		}

		@Override
		public void setAlphaConc(double alphaConc) {
		}
	}
}
