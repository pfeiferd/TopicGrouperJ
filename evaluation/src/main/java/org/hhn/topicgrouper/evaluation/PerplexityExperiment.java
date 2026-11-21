package org.hhn.topicgrouper.evaluation;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.evaluation.data.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.plsa.impl.PLSA;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.LowMemTopicGrouper;
import org.hhn.topicgrouper.tg.validation.TGLRPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;
import org.hhn.topicgrouper.validation.BasicPerplexityCalculator;
import org.hhn.topicgrouper.validation.PerplexityCalculatorLeftToRight;
import org.hhn.topicgrouper.validation.UnigramModeler;

/**
 * Held out perplexity (document completion, estimated with the left-to-right
 * method) of Topic Grouper, LDA, PLSA and a unigram baseline for varying
 * numbers of topics. This is the main experiment of the paper's evaluation.
 */
public final class PerplexityExperiment {
	/** Vocabularies up to this size are processed by the faster but memory hungry EHAC solver. */
	static final int EHAC_MAX_WORDS = 2500;

	/** Receives one result. */
	public interface Sink {
		/**
		 * @param method
		 *            "tg", "lda", "plsa" or "unigram"
		 * @param topics
		 *            number of topics of the model
		 * @param perplexity
		 *            held out perplexity
		 * @param param
		 *            the optimized alpha concentration (tg only, NaN otherwise)
		 * @param errorRate
		 *            fraction of words assigned to a wrong true topic (only for
		 *            synthetic data and 4 topics, NaN otherwise)
		 */
		void accept(String method, int topics, double perplexity, double param, double errorRate);
	}

	/** Decides which numbers of topics are evaluated. */
	public interface TopicFilter {
		/**
		 * @param topics
		 *            number of topics
		 * @return whether to evaluate it
		 */
		boolean accept(int topics);
	}

	private PerplexityExperiment() {
	}

	/**
	 * Runs all methods for the standard grid of topic numbers and writes
	 * <code>perplexity-&lt;name&gt;.csv</code> with the columns
	 * <code>method;topics;perplexity;alphaConc</code>.
	 * 
	 * @param name
	 *            name of the data set
	 * @param split
	 *            training and test documents
	 * @param s
	 *            experiment size
	 * @param out
	 *            result file
	 * @param withPlsa
	 *            whether PLSA is evaluated (only feasible for small vocabularies)
	 */
	public static void run(String name, Split<DocumentProvider<String>> split,
			final Settings s, java.io.File out, boolean withPlsa) {
		Csv csv = new Csv(out, "method", "topics", "perplexity", "alphaConc");
		try {
			final Csv c = csv;
			Sink sink = new Sink() {
				public void accept(String method, int topics, double perplexity,
						double param, double errorRate) {
					c.row(method, topics, perplexity, param);
				}
			};
			TopicFilter grid = new TopicFilter() {
				public boolean accept(int topics) {
					return s.isPerplexityTopics(topics);
				}
			};
			runUnigram(split, s, sink);
			runTG(split, s, grid, null, sink);
			runLDA(split, s, grid, null, sink);
			if (withPlsa) {
				runPlsa(split, s, new TopicFilter() {
					public boolean accept(int topics) {
						return topics <= s.plsaMaxTopics && s.isPerplexityTopics(topics);
					}
				}, sink);
			}
		} finally {
			csv.close();
		}
	}

	/**
	 * Perplexity of the unigram model (all topics identical to the word frequency distribution).
	 * 
	 * @param split
	 *            training and test documents
	 * @param s
	 *            experiment size
	 * @param sink
	 *            receives the result
	 */
	public static void runUnigram(Split<DocumentProvider<String>> split, Settings s, Sink sink) {
		UnigramModeler<String> modeler = new UnigramModeler<String>(split.train, 1);
		sink.accept("unigram", 1, perplexity(modeler, split, s), Double.NaN, Double.NaN);
	}

	/**
	 * Topic Grouper: the solver runs once down to a single topic, the solutions of
	 * the selected topic numbers are evaluated on the way. The Dirichlet
	 * concentration of the left-to-right estimator is optimized on 10% of the
	 * training documents for every solution.
	 * 
	 * @param split
	 *            training and test documents
	 * @param s
	 *            experiment size
	 * @param filter
	 *            topic numbers to evaluate
	 * @param gen
	 *            generator of synthetic data to compute the error rate, may be null
	 * @param sink
	 *            receives the results
	 */
	public static void runTG(final Split<DocumentProvider<String>> split, final Settings s,
			final TopicFilter filter, final TWCLDAPaperDocumentGenerator gen, final Sink sink) {
		final HoldOutSplitter<String> holdOut = new HoldOutSplitter<String>(
				new Random(10), split.train, 0.1, 0, false,
				(DefaultVocab<String>) split.train.getVocab());
		final TGLRPerplexityCalculator<String> calc = new TGLRPerplexityCalculator<String>(
				new Random(42), false, new DefaultDocumentSplitter<String>(), 50) {
			@Override
			protected int initParticles() {
				return s.particles;
			}
		};
		calc.init();
		createSolver(split.train).solve(new TGSolutionListener<String>() {
			public void updatedSolution(int newTopicIndex, int oldTopicIndex,
					double improvement, int t1Size, int t2Size, TGSolution<String> solution) {
				int n = solution.getNumberOfTopics();
				if (filter.accept(n)) {
					calc.setSolution(solution);
					calc.optimizeAlphaConc(1, 250, holdOut.getHoldOut(), s.alphaOptSteps);
					double p = calc.computePerplexity(split.test);
					double err = Double.NaN;
					if (gen != null && n == 4) {
						err = errorRate(split.train, gen, calc.getPerplexityCalculator().getTopicModeler());
					}
					sink.accept("tg", n, p, calc.getAlphaConc(), err);
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
	 * @param train
	 *            training documents
	 * @return the EHAC solver for small and the low memory solver for large vocabularies
	 */
	static TGSolver<String> createSolver(DocumentProvider<String> train) {
		if (train.getVocab().getNumberOfWords() <= EHAC_MAX_WORDS) {
			return new EHACTopicGrouper<String>(1, train, 1);
		}
		return new LowMemTopicGrouper<String>(1, train, 1);
	}

	/**
	 * LDA with collapsed Gibbs sampling, a symmetric Dirichlet prior with
	 * concentration 50/topics for the topic distribution (as in the original
	 * experiments of the paper) and beta = 0.1.
	 * 
	 * @param split
	 *            training and test documents
	 * @param s
	 *            experiment size
	 * @param filter
	 *            topic numbers to evaluate
	 * @param gen
	 *            generator of synthetic data to compute the error rate, may be null
	 * @param sink
	 *            receives the results
	 */
	public static void runLDA(Split<DocumentProvider<String>> split, Settings s,
			TopicFilter filter, TWCLDAPaperDocumentGenerator gen, Sink sink) {
		for (int topics = 1; topics <= Math.max(s.maxTopics, 4); topics++) {
			if (filter.accept(topics)) {
				LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(
						new Random(42), split.train,
						LDAGibbsSampler.symmetricAlpha(50d / topics, topics), 0.1);
				lda.solve(s.ldaBurnIn, s.ldaIterations, null);
				double err = gen != null && topics == 4 ? errorRate(split.train, gen, lda) : Double.NaN;
				sink.accept("lda", topics, perplexity(lda, split, s), Double.NaN, err);
			}
		}
	}

	/**
	 * Probabilistic latent semantic analysis (EM).
	 * 
	 * @param split
	 *            training and test documents
	 * @param s
	 *            experiment size
	 * @param filter
	 *            topic numbers to evaluate
	 * @param sink
	 *            receives the results
	 */
	public static void runPlsa(Split<DocumentProvider<String>> split, Settings s,
			TopicFilter filter, Sink sink) {
		for (int topics = 1; topics <= s.plsaMaxTopics; topics++) {
			if (filter.accept(topics)) {
				PLSA<String> plsa = new PLSA<String>(new Random(42), split.train, topics, 50);
				plsa.train(s.plsaIterations);
				sink.accept("plsa", topics, perplexity(plsa, split, s), Double.NaN, Double.NaN);
			}
		}
	}

	private static double perplexity(AbstractTopicModeler<String> modeler,
			Split<DocumentProvider<String>> split, Settings s) {
		BasicPerplexityCalculator<String> calc = new PerplexityCalculatorLeftToRight<String>(
				new Random(42), false, new DefaultDocumentSplitter<String>(), s.particles);
		calc.setTopicModeler(modeler);
		return calc.computePerplexity(split.test);
	}

	private static double errorRate(DocumentProvider<String> train,
			final TWCLDAPaperDocumentGenerator gen, final AbstractTopicModeler<String> modeler) {
		PwtProvider<String> pwt = new PwtProvider<String>() {
			public double getPwtFromModel(int topic, int wordIndex) {
				return modeler.getPhi(topic, wordIndex);
			}

			public double getCorrectPwt(int topic, String word) {
				return gen.getPwt(topic, word);
			}
		};
		return 1 - new TrueTopicAccuracyCalculator<String>().computeAccuracy(train, 4, pwt);
	}
}
