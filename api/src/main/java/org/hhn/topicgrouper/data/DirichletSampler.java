package org.hhn.topicgrouper.data;

import java.util.Random;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * Samples from Dirichlet distributions (via normalized gamma variates).
 */
public class DirichletSampler {
	private final RandomGenerator random;

	/**
	 * Creates a sampler that draws its random numbers from the given generator.
	 *
	 * @param random the source of randomness
	 */
	public DirichletSampler(final Random random) {
		this.random = new RandomGenerator() {

			@Override
			public void setSeed(long seed) {
				random.setSeed(seed);
			}

			@Override
			public void setSeed(int[] seed) {
				throw new IllegalStateException();
			}

			@Override
			public void setSeed(int seed) {
				random.setSeed(seed);
			}

			@Override
			public long nextLong() {
				return random.nextLong();
			}

			@Override
			public int nextInt(int n) {
				return random.nextInt(n);
			}

			@Override
			public int nextInt() {
				return random.nextInt();
			}

			@Override
			public double nextGaussian() {
				return random.nextGaussian();
			}

			@Override
			public float nextFloat() {
				return random.nextFloat();
			}

			@Override
			public double nextDouble() {
				return random.nextDouble();
			}

			@Override
			public void nextBytes(byte[] bytes) {
				random.nextBytes(bytes);
			}

			@Override
			public boolean nextBoolean() {
				return random.nextBoolean();
			}
		};
	}

	/**
	 * Samples from the Dirichlet distribution with the given parameters: gamma
	 * variates with shape <code>params[i]</code> and scale 1, normalized to sum 1.
	 *
	 * @param params the Dirichlet parameters
	 * @return a sample, a probability vector of the same length as params
	 */
	public double[] sample(double[] params) {
		double[] p = new double[params.length];
		double sum = 0;
		for (int i = 0; i < params.length; i++) {
			p[i] = new GammaDistribution(random, params[i], 1.00d).sample();
			sum = sum + p[i];
		}
		for (int i = 0; i < params.length; i++) {
			p[i] = p[i] / sum;
		}
		return p;
	}
	
	/**
	 * Samples from the symmetric Dirichlet distribution with all parameters 1
	 * (uniform random numbers normalized to sum 1).
	 *
	 * @param n the dimension of the sample
	 * @return a sample of size n from the symmetric Dirichlet distribution with all parameters 1.
	 */
	public double[] dirichlet1Sample(int n) {
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
