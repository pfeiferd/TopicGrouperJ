package org.hhn.topicgrouper.util;

import java.util.Random;

import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.commons.math3.random.RandomGenerator;

public class DirichletSampler {
	private final RandomGenerator random;

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
