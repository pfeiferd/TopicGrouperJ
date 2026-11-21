package org.hhn.topicgrouper.util;

import java.util.Random;

/**
 * Generates random distributions by normalizing independent uniform random numbers.
 */
public class RandomDirichlet1Dist {
	private final Random random;
	
	/**
	 * @param random source of randomness
	 */
	public RandomDirichlet1Dist(Random random) {
		this.random = random;
	}
	
	/**
	 * @param n the number of outcomes
	 * @return a random probability distribution over n outcomes
	 */
	public double[] nextDistribution(int n) {
		double[] res = new double[n];
		double sum = 0;
		
		for (int i = 0; i < n; i++) {
			res[i] = random.nextDouble();
			sum += res[i];
		}
		for (int i = 0; i < n; i++) {
			res[i] = res[i] / sum;
		}
		return res;
	}
}
