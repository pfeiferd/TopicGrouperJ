package org.hhn.topicgrouper.plsa.impl;

import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

public class PLSATest extends TestCase {
	private DefaultDocumentProvider<String> createProvider() {
		Random random = new Random(1);
		String[] a = { "a0", "a1", "a2", "a3" };
		String[] b = { "b0", "b1", "b2", "b3" };
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int i = 0; i < 8; i++) {
			String[] words = i % 2 == 0 ? a : b;
			DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
			for (int j = 0; j < 20; j++) {
				d.addWord(words[random.nextInt(words.length)]);
			}
			// A few words of the other topic.
			String[] other = i % 2 == 0 ? b : a;
			d.addWord(other[random.nextInt(other.length)]);
		}
		return p;
	}

	public void testLikelihoodNonDecreasingAndDistributionsNormalized() {
		DefaultDocumentProvider<String> p = createProvider();
		final double[] ll = new double[30];
		PLSA<String> plsa = new PLSA<String>(new Random(5), p, 2, 1.0) {
			@Override
			protected void afterIteration(int iteration) {
				ll[iteration] = computeLogLikelihood();
			}
		};
		plsa.train(ll.length);

		for (int i = 1; i < ll.length; i++) {
			assertFalse(Double.isNaN(ll[i]));
			assertTrue("iteration " + i + ": " + ll[i - 1] + " -> " + ll[i],
					ll[i] >= ll[i - 1] - 1e-9 * Math.abs(ll[i - 1]));
		}
		// EM actually improves the random start.
		assertTrue(ll[ll.length - 1] > ll[0]);
		assertEquals(ll[ll.length - 1], plsa.computeLogLikelihood(), 1e-9);

		for (int k = 0; k < plsa.getNTopics(); k++) {
			double sum = 0;
			for (int w = 0; w < plsa.getNWords(); w++) {
				assertTrue(plsa.getPhi(k, w) >= 0);
				sum += plsa.getPhi(k, w);
			}
			assertEquals(1.0, sum, 1e-9);
		}
		double[][] theta = plsa.getDocTopics();
		assertEquals(p.getDocuments().size(), theta.length);
		for (double[] row : theta) {
			double sum = 0;
			for (double v : row) {
				assertTrue(v >= 0);
				sum += v;
			}
			assertEquals(1.0, sum, 1e-9);
		}
		double topicSum = 0;
		for (int k = 0; k < plsa.getNTopics(); k++) {
			topicSum += plsa.getTopicProb(k);
			assertEquals(plsa.getTopicProb(k) * plsa.getAlphaConc(), plsa.getAlpha(k), 1e-12);
		}
		assertEquals(1.0, topicSum, 1e-9);
	}

	public void testRandomProbabilities() {
		PLSA<String> plsa = new PLSA<String>(new Random(1), createProvider(), 2, 1.0);
		double[] v = plsa.randomProbabilities(new double[5]);
		double sum = 0;
		for (double x : v) {
			assertTrue(x > 0);
			sum += x;
		}
		assertEquals(1.0, sum, 1e-12);
	}
}
