package org.hhn.topicgrouper.eval;

/**
 * Sizes of the experiments. {@link #full()} corresponds to the setup of the
 * papers, {@link #quick()} is a small setup for checking that everything runs.
 * The fields are deliberately public and mutable: an experiment may need a
 * variant (see {@link #copy()}).
 */
public final class Settings {
	/** Largest number of topics evaluated in the perplexity experiments. */
	public int maxTopics;
	/** Topic numbers evaluated: 1 and the multiples of this step. */
	public int topicStep;
	/** Burn-in iterations of LDA. */
	public int ldaBurnIn;
	/** Sampling iterations of LDA (after burn-in). */
	public int ldaIterations;
	/** Particles of the left-to-right perplexity estimator (LDA and Topic Grouper with senses; plain Topic Grouper is evaluated in closed form). */
	public int particles;
	/** Number of refinement steps when optimizing the alpha concentration of TG. */
	public int alphaOptSteps;
	/** Threads for independent model runs (the LDA models of the perplexity experiment). */
	public int threads;
	/**
	 * Largest vocabulary for which the perplexity experiment also runs the sense splitting
	 * solver (<code>tg-sense</code>; its memory is quadratic in the vocabulary).
	 */
	public int senseMaxWords;
	/** Largest number of topics evaluated with PLSA. */
	public int plsaMaxTopics;
	/** EM iterations of PLSA. */
	public int plsaIterations;
	/** Documents of the synthetic corpus. */
	public int twcDocs;
	/** Runs of the random models (LDA, PLSA) per point of the synthetic series. */
	public int twcTries;
	/** Steps of the alpha series on the synthetic corpus. */
	public int twcAlphaSteps;
	/** Steps of the document series on the synthetic corpus (500 documents per step). */
	public int twcDocsSteps;
	/** Numbers of homonyms of the homonym experiment. */
	public int[] homonymCounts;
	/** Topic numbers at which the classifiers on Topic Grouper topics (and the word baselines) are evaluated. */
	public int[] classificationTopics;
	/** Topic numbers at which the classifiers on LDA topics are evaluated. */
	public int[] ldaClassificationTopics;
	/** Burn-in iterations of LDA in the classification experiment. */
	public int classLdaBurnIn;
	/** Sampling iterations of LDA in the classification experiment. */
	public int classLdaIterations;
	/** Fold-in iterations of the LDA classifier per document. */
	public int foldInIterations;
	/** Fold-in samples of the LDA classifier per document. */
	public int foldInSamples;
	/** Repetitions per point of the performance experiment. */
	public int performanceTries;
	/** Steps of the performance experiment (200 documents per step). */
	public int performanceSteps;
	/** Documents of the full AP corpus used for perplexity. */
	public int apFullDocs;
	/** Number of topics at which the topic examples are listed. */
	public int exampleTopics;
	/** Real corpora are reduced to a random subset of this many documents (0: all documents). */
	public int maxDocuments;

	private Settings() {
	}

	/**
	 * The full setup of the papers (e.g. 200 topics, 6000 synthetic documents, 10 refinement steps for
	 * the Dirichlet concentration of Topic Grouper, 20 particles of the left-to-right estimator), except
	 * for the LDA iterations, which dominate the run time on the text corpora: 500 burn-in and 200
	 * sampling iterations (the papers used 2000 and 1000; both can be set on the command line, see
	 * {@link Main}). Independent model runs use one thread per available processor.
	 *
	 * @return the sizes used for the papers
	 */
	public static Settings full() {
		Settings s = new Settings();
		s.maxTopics = 200;
		s.topicStep = 10;
		s.ldaBurnIn = 500;
		s.ldaIterations = 200;
		s.particles = 20;
		s.alphaOptSteps = 10;
		s.threads = Runtime.getRuntime().availableProcessors();
		s.senseMaxWords = 12000;
		s.plsaMaxTopics = 200;
		s.plsaIterations = 150;
		s.twcDocs = 6000;
		s.twcTries = 50;
		s.twcAlphaSteps = 10;
		s.twcDocsSteps = 20;
		s.homonymCounts = new int[] { 0, 10, 20, 40, 80 };
		s.classificationTopics = paperTopicGrid(9000);
		// LDA stops at 1000 topics: the dense Gibbs sampler is linear in the
		// topics (about 5 hours per 1000-topic model of 2000 iterations on
		// Reuters, 15 hours at 2000), and the accuracy is flat from a few
		// hundred topics on.
		s.ldaClassificationTopics = paperTopicGrid(1000);
		s.classLdaBurnIn = 1000;
		s.classLdaIterations = 1000;
		s.foldInIterations = 200;
		s.foldInSamples = 50;
		s.performanceTries = 10;
		s.performanceSteps = 15;
		s.apFullDocs = 25000;
		s.exampleTopics = 40;
		s.maxDocuments = 0;
		return s;
	}

	/**
	 * A small setup for checking that everything runs (e.g. 30 topics, 20 LDA iterations, 1500 synthetic documents).
	 *
	 * @return a small setup for quick checks
	 */
	public static Settings quick() {
		Settings s = new Settings();
		s.maxTopics = 30;
		s.topicStep = 10;
		s.ldaBurnIn = 20;
		s.ldaIterations = 20;
		s.particles = 5;
		s.alphaOptSteps = 3;
		s.threads = Runtime.getRuntime().availableProcessors();
		s.senseMaxWords = 12000;
		s.plsaMaxTopics = 10;
		s.plsaIterations = 10;
		s.twcDocs = 1500;
		s.twcTries = 2;
		s.twcAlphaSteps = 3;
		s.twcDocsSteps = 3;
		s.homonymCounts = new int[] { 0, 20 };
		s.classificationTopics = new int[] { 1, 2, 5, 10, 20, 50 };
		s.ldaClassificationTopics = new int[] { 2, 10 };
		s.classLdaBurnIn = 20;
		s.classLdaIterations = 20;
		s.foldInIterations = 20;
		s.foldInSamples = 5;
		s.performanceTries = 2;
		s.performanceSteps = 3;
		s.apFullDocs = 2000;
		s.exampleTopics = 10;
		s.maxDocuments = 2000;
		return s;
	}

	/**
	 * Copies these settings (arrays are cloned) so that a variant can be derived.
	 *
	 * @return a copy of these settings
	 */
	public Settings copy() {
		Settings s = new Settings();
		s.maxTopics = maxTopics;
		s.topicStep = topicStep;
		s.ldaBurnIn = ldaBurnIn;
		s.ldaIterations = ldaIterations;
		s.particles = particles;
		s.alphaOptSteps = alphaOptSteps;
		s.threads = threads;
		s.senseMaxWords = senseMaxWords;
		s.plsaMaxTopics = plsaMaxTopics;
		s.plsaIterations = plsaIterations;
		s.twcDocs = twcDocs;
		s.twcTries = twcTries;
		s.twcAlphaSteps = twcAlphaSteps;
		s.twcDocsSteps = twcDocsSteps;
		s.homonymCounts = homonymCounts.clone();
		s.classificationTopics = classificationTopics.clone();
		s.ldaClassificationTopics = ldaClassificationTopics.clone();
		s.classLdaBurnIn = classLdaBurnIn;
		s.classLdaIterations = classLdaIterations;
		s.foldInIterations = foldInIterations;
		s.foldInSamples = foldInSamples;
		s.performanceTries = performanceTries;
		s.performanceSteps = performanceSteps;
		s.apFullDocs = apFullDocs;
		s.exampleTopics = exampleTopics;
		s.maxDocuments = maxDocuments;
		return s;
	}

	/**
	 * Derives settings with other LDA iterations.
	 *
	 * @param burnIn
	 *            LDA burn-in iterations
	 * @param iterations
	 *            LDA sampling iterations
	 * @return a copy of these settings with other LDA iterations (for large corpora)
	 */
	public Settings withLda(int burnIn, int iterations) {
		Settings s = copy();
		s.ldaBurnIn = burnIn;
		s.ldaIterations = iterations;
		return s;
	}

	/**
	 * The topic grid of the feature reduction paper: 1 to 9, 10 to 90, 100 to
	 * 900, 1000 to max in steps of 1000.
	 */
	private static int[] paperTopicGrid(int max) {
		int[] grid = new int[27 + max / 1000];
		int n = 0;
		for (int t = 1; t <= 9; t++) {
			grid[n++] = t;
		}
		for (int t = 10; t <= 90; t += 10) {
			grid[n++] = t;
		}
		for (int t = 100; t <= 900; t += 100) {
			grid[n++] = t;
		}
		for (int t = 1000; t <= max; t += 1000) {
			grid[n++] = t;
		}
		return grid;
	}

	/**
	 * Decides whether a number of topics is evaluated in the perplexity experiments.
	 *
	 * @param topics
	 *            a number of topics
	 * @return whether it is on the perplexity grid (1 and the multiples of the step up to the maximum)
	 */
	public boolean isPerplexityTopics(int topics) {
		return topics <= maxTopics && (topics == 1 || topics % topicStep == 0);
	}

	/**
	 * Decides whether a number of topics is in {@link #classificationTopics}.
	 *
	 * @param topics
	 *            a number of topics
	 * @return whether the classifiers on Topic Grouper topics are evaluated at this number of topics
	 */
	public boolean isClassificationTopics(int topics) {
		return contains(classificationTopics, topics);
	}

	private static boolean contains(int[] grid, int topics) {
		for (int g : grid) {
			if (g == topics) {
				return true;
			}
		}
		return false;
	}
}
