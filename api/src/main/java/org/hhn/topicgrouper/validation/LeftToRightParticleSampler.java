package org.hhn.topicgrouper.validation;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;

/**
 * Left-to-right sampler with independent particles, each keeping its own topic assignments
 * (Wallach et al., "Evaluation Methods for Topic Models", 2009, Algorithm 3).
 *
 * @param <T> the type of the words
 */
public abstract class LeftToRightParticleSampler<T> extends LeftToRightSamplerBase<T> {
	// For "left to right" with particles.
	/**
	 * Topic assignments of the words processed so far, per particle.
	 */
	protected final TIntList[] z;
	/**
	 * Topic counts per particle.
	 */
	protected final int[][] lrTopicAssignmentCounts;

	/**
	 * Creates a sampler and allocates the per-particle state.
	 *
	 * @param random source of randomness
	 * @param particles number of particles
	 * @param ntopics the number of topics
	 */
	public LeftToRightParticleSampler(Random random, int particles, int ntopics) {
		super(random, particles, ntopics);
		lrTopicAssignmentCounts = new int[particles][ntopics];

		z = new TIntList[particles];
		for (int i = 0; i < z.length; i++) {
			z[i] = new TIntArrayList();
		}
	}

	/**
	 * Estimates the log probability of a document.
	 *
	 * @param d the document
	 * @return the estimated log probability
	 */
	public double leftToRight(Document<T> d) {
		initFields();
		return leftToRightHelp(d);
	}

	/**
	 * Resets the word list and the assignments of all particles.
	 */
	protected void initFields() {
		super.initFields();
		for (int i = 0; i < z.length; i++) {
			Arrays.fill(lrTopicAssignmentCounts[i], 0);
			z[i].clear();
		}
	}
	
	@Override
	protected double leftToRightForWord(int n, int wordIndex) {
		linearWordIndices.add(wordIndex);

		double pn = 0;
		// According to Algorithm 3
		// Line 4
		for (int r = 0; r < z.length; r++) {
			for (int n2 = 0; n2 < n; n2++) {
				// Line 6
				int wordIndexN2 = linearWordIndices.get(n2);
				lrTopicAssignmentCounts[r][z[r].get(n2)]--;
				for (int k = 0; k < pzn.length; k++) {
					pzn[k] = getPhi(k, wordIndexN2)
							* (lrTopicAssignmentCounts[r][k] + getAlpha(k));
				}
				int zn2 = nextDiscrete(pzn);
				z[r].set(n2, zn2);
				lrTopicAssignmentCounts[r][zn2]++;
			}
			// Line 8
			for (int t = 0; t < pzn.length; t++) {
				pn += getPhi(t, wordIndex)
						* (lrTopicAssignmentCounts[r][t] + getAlpha(t))
						/ (n + getAlphaSum());
			}
			// Line 9
			for (int k = 0; k < pzn.length; k++) {
				pzn[k] = getPhi(k, wordIndex)
						* (lrTopicAssignmentCounts[r][k] + getAlpha(k));
			}
			int zn = nextDiscrete(pzn);
			z[r].add(zn);
			lrTopicAssignmentCounts[r][zn]++;
		}
		return pn / z.length;
	}

	/**
	 * Draws an index from the unnormalized discrete distribution.
	 *
	 * @param probs unnormalized probabilities
	 * @return the drawn index
	 */
	protected int nextDiscrete(double[] probs) {
		double sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
		}
		double r = random.nextDouble() * sum;

		sum = 0.0;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (sum > r) {
				return i;
			}
		}
		return probs.length - 1;
	}

	/**
	 * @param t the topic
	 * @return the Dirichlet prior parameter alpha of the topic
	 */
	protected abstract double getAlpha(int t);

	/**
	 * @return the sum of all alpha values
	 */
	protected abstract double getAlphaSum();

	/**
	 * @param t the topic
	 * @param wordIndex the word
	 * @return the probability of the word in the topic
	 */
	protected abstract double getPhi(int t, int wordIndex);
}
