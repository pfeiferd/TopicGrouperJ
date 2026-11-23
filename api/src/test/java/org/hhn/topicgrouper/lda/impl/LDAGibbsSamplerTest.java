package org.hhn.topicgrouper.lda.impl;

import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;

public class LDAGibbsSamplerTest extends TestCase {
	private static final String[] A = { "a0", "a1", "a2", "a3", "a4" };
	private static final String[] B = { "b0", "b1", "b2", "b3", "b4" };

	private static void addWords(DefaultDocumentProvider<String>.DefaultDocument d,
			Random random, String[] words, int n) {
		for (int i = 0; i < n; i++) {
			d.addWord(words[random.nextInt(words.length)]);
		}
	}

	/**
	 * 12 documents of topic A, 12 of topic B and 6 mixed ones; the two topics have
	 * disjoint vocabularies.
	 */
	private DefaultDocumentProvider<String> createSeparatedCorpus() {
		Random random = new Random(1);
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int i = 0; i < 12; i++) {
			addWords(p.newDocument(), random, A, 40);
			addWords(p.newDocument(), random, B, 40);
		}
		for (int i = 0; i < 6; i++) {
			DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
			addWords(d, random, A, 20);
			addWords(d, random, B, 20);
		}
		return p;
	}

	private double sumPhi(LDAGibbsSampler<String> lda, int topic, String[] words) {
		double sum = 0;
		for (String w : words) {
			sum += lda.getPhi(topic, lda.getVocab().getIndex(w));
		}
		return sum;
	}

	public void testRecoversTwoSeparatedTopics() {
		DefaultDocumentProvider<String> p = createSeparatedCorpus();
		LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(new Random(42), p, 2, 1.0, 0.1);
		assertEquals(0.5, lda.getAlpha(0), 1e-12);
		assertEquals(1.0, lda.getAlphaConc(), 1e-12);
		lda.solve(100, 20, null);

		// Rows of phi are distributions.
		for (int k = 0; k < 2; k++) {
			double sum = 0;
			for (int w = 0; w < lda.getNWords(); w++) {
				double phi = lda.getPhi(k, w);
				assertTrue(phi > 0 && phi <= 1);
				sum += phi;
			}
			assertEquals(1.0, sum, 1e-9);
		}
		// Topic probabilities sum to 1 and are balanced.
		assertEquals(1.0, lda.getTopicProb(0) + lda.getTopicProb(1), 1e-9);
		assertEquals(0.5, lda.getTopicProb(0), 0.1);

		// One topic is A, the other B.
		int kA = sumPhi(lda, 0, A) > sumPhi(lda, 1, A) ? 0 : 1;
		assertTrue(sumPhi(lda, kA, A) > 0.95);
		assertTrue(sumPhi(lda, 1 - kA, B) > 0.95);

		// Fold-in of a pure A document assigns nearly all words to topic A.
		DefaultDocumentProvider<String> test = new DefaultDocumentProvider<String>(
				(DefaultVocab<String>) p.getVocab());
		DefaultDocumentProvider<String>.DefaultDocument d = test.newDocument();
		addWords(d, new Random(7), A, 30);
		LDAGibbsSampler<String>.FoldInStore store = lda.foldIn(50, d);
		int[] counts = store.getDTopicAssignmentCounts();
		assertEquals(30, counts[0] + counts[1]);
		assertTrue(counts[kA] >= 27);
	}

	/**
	 * Documents are generated from LDA with alpha = (1, 1) (theta uniform on the
	 * simplex) and disjoint topic vocabularies, so the topic assignments are
	 * effectively observed. Minka's update started at alpha = (5, 5) must move
	 * towards (1, 1).
	 */
	public void testAlphaUpdateApproachesTrueAlpha() {
		Random random = new Random(3);
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int i = 0; i < 300; i++) {
			double thetaA = random.nextDouble();
			DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
			for (int j = 0; j < 40; j++) {
				String[] topic = random.nextDouble() < thetaA ? A : B;
				d.addWord(topic[random.nextInt(topic.length)]);
			}
		}
		LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(new Random(42), p, 2, 10.0, 0.1);
		lda.setUpdateAlpha(true);
		assertTrue(lda.isUpdateAlpha());
		assertFalse(lda.isUpdateBeta());
		lda.setAlphaBetaUpdate(5);
		lda.solve(60, 5, null);

		double a0 = lda.getAlpha(0);
		double a1 = lda.getAlpha(1);
		assertEquals(a0 + a1, lda.getAlphaConc(), 1e-9);
		assertTrue("alpha0 = " + a0, a0 > 0.5 && a0 < 2.0);
		assertTrue("alpha1 = " + a1, a1 > 0.5 && a1 < 2.0);
	}

	public void testBetaUpdateStaysPositiveAndFinite() {
		DefaultDocumentProvider<String> p = createSeparatedCorpus();
		LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(new Random(42), p, 2, 1.0, 1.0);
		lda.setUpdateBeta(true);
		lda.setAlphaBetaUpdate(5);
		lda.solve(40, 5, null);
		for (int k = 0; k < 2; k++) {
			double sum = 0;
			for (int w = 0; w < lda.getNWords(); w++) {
				sum += lda.getPhi(k, w);
			}
			assertEquals(1.0, sum, 1e-9);
		}
		double beta = lda.getBeta(0, 0);
		assertTrue(beta > 0 && !Double.isInfinite(beta) && !Double.isNaN(beta));
		// With disjoint topics the data favour a sparse (small) beta.
		assertTrue("beta = " + beta, beta < 1.0);
		assertEquals(lda.getNWords() * beta, lda.getBetaSum(0), 1e-9);
	}

	public void testSymmetricAlpha() {
		double[] alpha = LDAGibbsSampler.symmetricAlpha(2.0, 4);
		assertEquals(4, alpha.length);
		for (double a : alpha) {
			assertEquals(0.5, a, 1e-12);
		}
	}
}
