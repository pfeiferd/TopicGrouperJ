package org.hhn.topicgrouper.util;

import java.util.Random;

import junit.framework.TestCase;

public class RandomDirichlet1DistTest extends TestCase {
	public void testDistributionsAreNormalized() {
		RandomDirichlet1Dist dist = new RandomDirichlet1Dist(new Random(1));
		for (int i = 0; i < 100; i++) {
			double[] v = dist.nextDistribution(5);
			assertEquals(5, v.length);
			double sum = 0;
			for (double x : v) {
				assertTrue(x > 0 && x < 1);
				sum += x;
			}
			assertEquals(1.0, sum, 1e-12);
		}
	}

	/**
	 * Dir(1, 1) is the uniform distribution on [0, 1] for the first component:
	 * P(x < 0.25) = 0.25 and E[x] = 0.5. (Normalized uniform random numbers would
	 * give P(x < 0.25) = 1/6.)
	 */
	public void testTwoComponentsAreUniform() {
		RandomDirichlet1Dist dist = new RandomDirichlet1Dist(new Random(7));
		int n = 40000;
		int below = 0;
		double sum = 0;
		for (int i = 0; i < n; i++) {
			double x = dist.nextDistribution(2)[0];
			if (x < 0.25) {
				below++;
			}
			sum += x;
		}
		assertEquals(0.25, ((double) below) / n, 0.01);
		assertEquals(0.5, sum / n, 0.01);
	}

	/**
	 * For Dir(1, 1, 1) each component is Beta(1, 2): E[x] = 1/3, P(x < 0.5) = 3/4.
	 */
	public void testThreeComponentsMarginal() {
		RandomDirichlet1Dist dist = new RandomDirichlet1Dist(new Random(11));
		int n = 40000;
		int below = 0;
		double sum = 0;
		for (int i = 0; i < n; i++) {
			double x = dist.nextDistribution(3)[2];
			if (x < 0.5) {
				below++;
			}
			sum += x;
		}
		assertEquals(0.75, ((double) below) / n, 0.01);
		assertEquals(1.0 / 3, sum / n, 0.01);
	}
}
