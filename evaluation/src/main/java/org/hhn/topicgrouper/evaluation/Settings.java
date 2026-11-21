package org.hhn.topicgrouper.evaluation;

/**
 * Sizes of the experiments. {@link #full()} corresponds to the setup used for
 * the ECIR 2019 paper (long running), {@link #quick()} is a scaled down version
 * for smoke tests and first looks (minutes instead of days).
 */
public final class Settings {
	/** Largest number of topics evaluated in the perplexity experiments. */
	public final int maxTopics;
	/** Perplexity is evaluated for 1 topic and every multiple of this step. */
	public final int topicStep;
	/** Burn-in iterations of the LDA Gibbs sampler in perplexity experiments. */
	public final int ldaBurnIn;
	/** Sampling iterations of the LDA Gibbs sampler in perplexity experiments. */
	public final int ldaIterations;
	/** Particles of the left-to-right perplexity estimator. */
	public final int particles;
	/** Number of refinement steps when optimizing the alpha concentration of TG. */
	public final int alphaOptSteps;
	/** PLSA is only evaluated up to this number of topics (memory!). */
	public final int plsaMaxTopics;
	/** EM iterations of PLSA. */
	public final int plsaIterations;
	/** Number of documents of the synthetic TWC corpus. */
	public final int twcDocs;
	/** Number of steps (different corpus sizes / alpha values) in the TWC series. */
	public final int twcSteps;
	/** Topic numbers evaluated for topic grouper and the vocabulary baselines in classification. */
	public final int[] classificationTopics;
	/** Topic numbers evaluated for LDA in classification. */
	public final int[] ldaClassificationTopics;
	/** Burn-in iterations of LDA in classification. */
	public final int classLdaBurnIn;
	/** Sampling iterations of LDA in classification. */
	public final int classLdaIterations;
	/** Fold-in iterations of the LDA classifier. */
	public final int foldInIterations;
	/** Fold-in samples of the LDA classifier. */
	public final int foldInSamples;
	/** Repetitions per corpus size in the performance experiment. */
	public final int performanceTries;
	/** Number of corpus sizes (steps of 200 documents) in the performance experiment. */
	public final int performanceSteps;

	private Settings(int maxTopics, int topicStep, int ldaBurnIn, int ldaIterations,
			int particles, int alphaOptSteps, int plsaMaxTopics, int plsaIterations,
			int twcDocs, int twcSteps, int[] classificationTopics,
			int[] ldaClassificationTopics, int classLdaBurnIn,
			int classLdaIterations, int foldInIterations, int foldInSamples,
			int performanceTries, int performanceSteps) {
		this.maxTopics = maxTopics;
		this.topicStep = topicStep;
		this.ldaBurnIn = ldaBurnIn;
		this.ldaIterations = ldaIterations;
		this.particles = particles;
		this.alphaOptSteps = alphaOptSteps;
		this.plsaMaxTopics = plsaMaxTopics;
		this.plsaIterations = plsaIterations;
		this.twcDocs = twcDocs;
		this.twcSteps = twcSteps;
		this.classificationTopics = classificationTopics;
		this.ldaClassificationTopics = ldaClassificationTopics;
		this.classLdaBurnIn = classLdaBurnIn;
		this.classLdaIterations = classLdaIterations;
		this.foldInIterations = foldInIterations;
		this.foldInSamples = foldInSamples;
		this.performanceTries = performanceTries;
		this.performanceSteps = performanceSteps;
	}

	/** @return the setup of the paper. */
	public static Settings full() {
		return new Settings(200, 10, 2000, 1000, 20, 10, 30, 100, 6000, 20,
				paperTopicGrid(), new int[] { 1, 2, 5, 10, 20, 50, 100, 200 },
				1000, 1000, 200, 50, 10, 15);
	}

	/** @return a small setup for quick checks. */
	public static Settings quick() {
		return new Settings(30, 10, 20, 20, 5, 3, 10, 10, 1500, 4,
				new int[] { 1, 2, 5, 10, 20, 50 }, new int[] { 2, 10 }, 20, 20,
				20, 5, 2, 3);
	}

	/** 1..9, 10..90 (step 10), 100..900 (step 100), 1000..5000 (step 1000). */
	private static int[] paperTopicGrid() {
		int[] grid = new int[9 + 9 + 9 + 5];
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
		for (int t = 1000; t <= 5000; t += 1000) {
			grid[n++] = t;
		}
		return grid;
	}

	/**
	 * @param topics
	 *            a number of topics
	 * @return whether the perplexity experiments evaluate this number
	 */
	public boolean isPerplexityTopics(int topics) {
		return topics <= maxTopics && (topics == 1 || topics % topicStep == 0);
	}

	/**
	 * @param grid
	 *            sorted array of topic numbers
	 * @param topics
	 *            a number of topics
	 * @return whether grid contains topics
	 */
	public static boolean contains(int[] grid, int topics) {
		for (int g : grid) {
			if (g == topics) {
				return true;
			}
		}
		return false;
	}
}
