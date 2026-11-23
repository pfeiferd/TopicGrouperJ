package org.hhn.topicgrouper.eval.classify.impl;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;

public class AbstractTopicBasedBasicProbClassifierTest extends TestCase {
	/**
	 * Every selected word is its own topic; other words have no topic.
	 */
	private static class WordBasicProbClassifier extends AbstractTopicBasedBasicProbClassifier<String, String> {
		private final int[] topics;
		private final TIntSet topicSet;
		private final double[] pt;

		WordBasicProbClassifier(double lambda, int[] topics, double[] pt) {
			super(lambda);
			this.topics = topics;
			this.topicSet = new TIntHashSet(topics);
			this.pt = pt;
		}

		@Override
		protected int[] getTopicIndices() {
			return topics;
		}

		@Override
		protected int getTopicIndex(int wordIndex) {
			return topicSet.contains(wordIndex) ? wordIndex : Integer.MIN_VALUE;
		}

		@Override
		protected double[] getPt() {
			return pt;
		}

		double logPtc(int label, int topic) {
			return getLogPtc(label, topic);
		}
	}

	private DefaultLabelingDocumentProvider<String, String> train;
	private DefaultLabelingDocumentProvider<String, String> test;
	private int w0, w1, w2, w3;

	@Override
	protected void setUp() {
		train = new DefaultLabelingDocumentProvider<String, String>();
		DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = train
				.newLabeledDocument("A");
		d.addWord("w0", 3);
		d.addWord("w1", 1);
		d.addWord("w3", 2);
		d = train.newLabeledDocument("A");
		d.addWord("w0", 2);
		d.addWord("w2", 1);
		d = train.newLabeledDocument("B");
		d.addWord("w1", 2);
		d.addWord("w2", 2);
		d.addWord("w3", 1);
		w0 = train.getVocab().getIndex("w0");
		w1 = train.getVocab().getIndex("w1");
		w2 = train.getVocab().getIndex("w2");
		w3 = train.getVocab().getIndex("w3");

		test = new DefaultLabelingDocumentProvider<String, String>(
				(DefaultVocab<String>) train.getVocab());
		// Classified as A (0.08 vs. 0.0204): hit
		d = test.newLabeledDocument("A");
		d.addWord("w0", 1);
		d.addWord("w1", 1);
		d.addWord("w3", 5);
		// Classified as B (0.00533 vs. 0.02624): miss
		d = test.newLabeledDocument("A");
		d.addWord("w1", 2);
		d.addWord("w2", 1);
		// Classified as B: hit
		d = test.newLabeledDocument("B");
		d.addWord("w1", 2);
		d.addWord("w2", 1);
	}

	/**
	 * Topics are w0, w1, w2 (w3 has no topic). Counts per label: A: w0=5, w1=1,
	 * w2=1 (total 7), B: w0=0, w1=2, w2=2 (total 4). Laplace smoothing with 3
	 * topics.
	 */
	public void testLaplaceSmoothedPosteriors() {
		WordBasicProbClassifier c = new WordBasicProbClassifier(0, new int[] { w0, w1, w2 }, null);
		c.train(train);
		assertEquals(2, c.labels.size());
		int a = c.labels.indexOf("A");
		int b = c.labels.indexOf("B");
		assertEquals(Math.log(2.0 / 3), c.logpc.get(a), 1e-12);
		assertEquals(Math.log(1.0 / 3), c.logpc.get(b), 1e-12);

		assertEquals(Math.log(6.0 / 10), c.logPtc(a, 0), 1e-12);
		assertEquals(Math.log(2.0 / 10), c.logPtc(a, 1), 1e-12);
		assertEquals(Math.log(2.0 / 10), c.logPtc(a, 2), 1e-12);
		assertEquals(Math.log(1.0 / 7), c.logPtc(b, 0), 1e-12);
		assertEquals(Math.log(3.0 / 7), c.logPtc(b, 1), 1e-12);
		assertEquals(Math.log(3.0 / 7), c.logPtc(b, 2), 1e-12);
		// The smoothed topic distributions of a label sum to one.
		for (int l = 0; l < 2; l++) {
			double sum = 0;
			for (int t = 0; t < 3; t++) {
				sum += Math.exp(c.logPtc(l, t));
			}
			assertEquals(1.0, sum, 1e-12);
		}

		assertEquals("A", c.classify(test.getDocuments().get(0)));
		assertEquals("B", c.classify(test.getDocuments().get(1)));
		assertEquals("B", c.classify(test.getDocuments().get(2)));
	}

	public void testMicroAndMacroAccuracy() {
		WordBasicProbClassifier c = new WordBasicProbClassifier(0, new int[] { w0, w1, w2 }, null);
		c.train(train);
		double[] res = c.test(test, null);
		assertEquals(2, res.length);
		// 2 of 3 documents correct
		assertEquals(2.0 / 3, res[0], 1e-12);
		// A: 1 of 2, B: 1 of 1
		assertEquals((0.5 + 1.0) / 2, res[1], 1e-12);
		double[] res2 = new double[2];
		assertSame(res2, c.test(test, res2));
		assertEquals(res[0], res2[0], 1e-12);
	}

	public void testPriorInterpolationSmoothing() {
		double[] pt = { 0.5, 0.25, 0.25 };
		WordBasicProbClassifier c = new WordBasicProbClassifier(0.4, new int[] { w0, w1, w2 }, pt);
		c.train(train);
		int a = c.labels.indexOf("A");
		int b = c.labels.indexOf("B");
		assertEquals(0.4, c.getSmoothingLambda(), 1e-12);
		assertEquals(Math.log(0.6 * 5.0 / 7 + 0.4 * 0.5), c.logPtc(a, 0), 1e-12);
		assertEquals(Math.log(0.6 * 1.0 / 7 + 0.4 * 0.25), c.logPtc(a, 1), 1e-12);
		assertEquals(Math.log(0.6 * 0.0 / 4 + 0.4 * 0.5), c.logPtc(b, 0), 1e-12);
		assertEquals(Math.log(0.6 * 2.0 / 4 + 0.4 * 0.25), c.logPtc(b, 2), 1e-12);
	}

	public void testOptimizeLambda() {
		double[] pt = { 0.5, 0.25, 0.25 };
		WordBasicProbClassifier c = new WordBasicProbClassifier(0.4, new int[] { w0, w1, w2 }, pt);
		c.train(train);
		c.optimizeLambda(0.05, 0.95, test, 3, true);
		double lambda = c.getSmoothingLambda();
		assertTrue(lambda >= 0.05 && lambda <= 0.95);
	}

	public void testUntrainedClassifierReturnsNull() {
		WordBasicProbClassifier c = new WordBasicProbClassifier(0, new int[] { w0, w1, w2 }, null);
		c.updateTopicIndices();
		assertNull(c.classify(test.getDocuments().get(0)));
	}
}
