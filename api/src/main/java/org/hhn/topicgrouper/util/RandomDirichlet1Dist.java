package org.hhn.topicgrouper.util;

import java.util.Random;

/**
 * Draws random distributions from the flat Dirichlet distribution Dir(1, ..., 1), i.e.
 * uniformly from the probability simplex, by normalizing independent standard
 * exponential random numbers (Gamma(1, 1) variates).
 */
public class RandomDirichlet1Dist {
	private final Random random;
	
	/**
	 * Creates a generator.
	 *
	 * @param random source of randomness
	 */
	public RandomDirichlet1Dist(Random random) {
		this.random = random;
	}

	/**
	 * Draws a distribution from Dir(1, ..., 1).
	 *
	 * @param n the number of outcomes
	 * @return a random probability distribution over n outcomes, uniformly distributed on the simplex
	 */
	public double[] nextDistribution(int n) {
		double[] res = new double[n];
		double sum = 0;

		for (int i = 0; i < n; i++) {
			// Standard exponential variate; 1 - nextDouble() is in (0, 1].
			res[i] = -Math.log(1 - random.nextDouble());
			sum += res[i];
		}
		for (int i = 0; i < n; i++) {
			res[i] = res[i] / sum;
		}
		return res;
	}
}
