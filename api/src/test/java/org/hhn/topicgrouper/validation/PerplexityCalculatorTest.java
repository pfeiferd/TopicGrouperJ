package org.hhn.topicgrouper.validation;

import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider.Vocab;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.EachWordDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.FiftyFiftyDocumentSplitter;

public class PerplexityCalculatorTest extends TestCase {
	private DefaultDocumentProvider<String> train;
	private DefaultDocumentProvider<String> test;
	private double closedForm;

	@Override
	protected void setUp() {
		// p(a) = 0.6, p(b) = 0.3, p(c) = 0.1
		train = new DefaultDocumentProvider<String>();
		DefaultDocumentProvider<String>.DefaultDocument d = train.newDocument();
		d.addWord("a", 4);
		d.addWord("b", 1);
		d = train.newDocument();
		d.addWord("a", 2);
		d.addWord("b", 2);
		d.addWord("c", 1);

		// Test document: a a b c
		test = new DefaultDocumentProvider<String>((DefaultVocab<String>) train.getVocab());
		d = test.newDocument();
		d.addWord("a", 2);
		d.addWord("b", 1);
		d.addWord("c", 1);
		closedForm = Math.exp(-(2 * Math.log(0.6) + Math.log(0.3) + Math.log(0.1)) / 4);
	}

	public void testUnigramModelerIsConsistent() {
		UnigramModeler<String> m = new UnigramModeler<String>(train, 3);
		for (int k = 0; k < 3; k++) {
			double sum = 0;
			for (int w = 0; w < m.getNWords(); w++) {
				sum += m.getPhi(k, w);
			}
			assertEquals(1.0, sum, 1e-12);
			assertEquals(1.0 / 3, m.getTopicProb(k), 1e-12);
			assertEquals(m.getTopicProb(k) * m.getAlphaConc(), m.getAlpha(k), 1e-12);
		}
		assertEquals(0.6, m.getPhi(0, train.getVocab().getIndex("a")), 1e-12);
	}

	public void testBasicPerplexityEqualsClosedForm() {
		for (int topics = 1; topics <= 3; topics++) {
			BasicPerplexityCalculator<String> calc = new BasicPerplexityCalculator<String>(false);
			calc.setTopicModeler(new UnigramModeler<String>(train, topics));
			assertEquals(closedForm, calc.computePerplexity(test), 1e-9);
		}
	}

	public void testBowFactor() {
		BasicPerplexityCalculator<String> calc = new BasicPerplexityCalculator<String>(true);
		calc.setTopicModeler(new UnigramModeler<String>(train, 1));
		// Multinomial coefficient 4! / (2! 1! 1!) = 12
		assertEquals(closedForm * Math.pow(12, -0.25), calc.computePerplexity(test), 1e-9);
		assertEquals(Math.log(24), BasicPerplexityCalculator.logFacN(4), 1e-12);
		assertEquals(0, BasicPerplexityCalculator.logFacN(0), 1e-12);
	}

	public void testLeftToRightEqualsUnigramPerplexity() {
		for (int topics = 1; topics <= 3; topics++) {
			PerplexityCalculatorLeftToRight<String> calc = new PerplexityCalculatorLeftToRight<String>(
					new Random(1), false, 5);
			calc.setTopicModeler(new UnigramModeler<String>(train, topics));
			assertEquals(closedForm, calc.computePerplexity(test), 1e-9);

			// Particle variant
			calc = new PerplexityCalculatorLeftToRight<String>(new Random(1), false, 5) {
				@Override
				protected boolean useUnbiased() {
					return false;
				}
			};
			calc.setTopicModeler(new UnigramModeler<String>(train, topics));
			assertEquals(closedForm, calc.computePerplexity(test), 1e-9);
		}
		// With document completion splitters.
		PerplexityCalculatorLeftToRight<String> calc = new PerplexityCalculatorLeftToRight<String>(
				new Random(1), false, new EachWordDocumentSplitter<String>(true), 3);
		calc.setTopicModeler(new UnigramModeler<String>(train, 2));
		assertEquals(closedForm, calc.computePerplexity(test), 1e-9);
		calc = new PerplexityCalculatorLeftToRight<String>(new Random(1), false,
				new FiftyFiftyDocumentSplitter<String>(new Random(2)), 3);
		calc.setTopicModeler(new UnigramModeler<String>(train, 2));
		assertEquals(closedForm, calc.computePerplexity(test), 1e-9);
	}

	public void testLeftToRightAppliesBowFactor() {
		PerplexityCalculatorLeftToRight<String> calc = new PerplexityCalculatorLeftToRight<String>(
				new Random(1), true, 5);
		calc.setTopicModeler(new UnigramModeler<String>(train, 1));
		assertEquals(closedForm * Math.pow(12, -0.25), calc.computePerplexity(test), 1e-9);
	}

	public void testAveragingEqualsUnigramPerplexity() {
		for (int topics = 1; topics <= 3; topics++) {
			PerplexityCalculatorAveraging<String> calc = new PerplexityCalculatorAveraging<String>(
					false, new DefaultDocumentSplitter<String>(false));
			calc.setTopicModeler(new UnigramModeler<String>(train, topics));
			assertEquals(closedForm, calc.computePerplexity(test), 1e-9);
			calc = new PerplexityCalculatorAveraging<String>(false);
			calc.setTopicModeler(new UnigramModeler<String>(train, topics));
			assertEquals(closedForm, calc.computePerplexity(test), 1e-9);
		}
	}

	/**
	 * Two topics with different word distributions: the averaged p(t|d) must be a
	 * distribution and the resulting perplexity must be the one of the mixture.
	 */
	public void testAveragingPtdIsNormalized() {
		final double[][] phi = { { 0.8, 0.1, 0.1 }, { 0.2, 0.3, 0.5 } };
		AbstractTopicModeler<String> m = new TwoTopicModeler(train.getVocab(), phi, 0.5);
		// Reference document a a b c: p(t|w) = phi(t,w) p(t) / p(w)
		// p(a) = 0.5, p(b) = 0.2, p(c) = 0.3
		double ptd0 = 0.5 * (0.8 * 0.5 / 0.5) + 0.25 * (0.1 * 0.5 / 0.2) + 0.25 * (0.1 * 0.5 / 0.3);
		double ptd1 = 1 - ptd0;
		double logp = 2 * Math.log(0.8 * ptd0 + 0.2 * ptd1) + Math.log(0.1 * ptd0 + 0.3 * ptd1)
				+ Math.log(0.1 * ptd0 + 0.5 * ptd1);
		PerplexityCalculatorAveraging<String> calc = new PerplexityCalculatorAveraging<String>(
				false, new DefaultDocumentSplitter<String>(false));
		calc.setTopicModeler(m);
		assertEquals(Math.exp(-logp / 4), calc.computePerplexity(test), 1e-9);
	}

	public void testVocabMismatchIsRejected() {
		DefaultDocumentProvider<String> other = new DefaultDocumentProvider<String>();
		other.newDocument().addWord("x");
		BasicPerplexityCalculator<String> calc = new BasicPerplexityCalculator<String>(false);
		calc.setTopicModeler(new UnigramModeler<String>(train, 1));
		try {
			calc.computePerplexity(other);
			fail();
		} catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * The optimizer must leave the model with the optimized concentration, not with
	 * the last tested one.
	 */
	public void testOptimizeAlphaConcSetsOptimum() {
		final double[][] phi = { { 0.9, 0.1, 0.0 }, { 0.1, 0.9, 0.0 } };
		// alpha(0) = 1 is fixed, alpha(1) = alphaConc - 1, so p(t|d) depends on alphaConc.
		final TwoTopicModeler m = new TwoTopicModeler(train.getVocab(), phi, 0.5) {
			@Override
			public double getAlpha(int i) {
				return i == 0 ? 1 : getAlphaConc() - 1;
			}
		};
		BasicPerplexityCalculator<String> calc = new BasicPerplexityCalculator<String>(false) {
			@Override
			protected void updatePtd(Document<String> d,
					AbstractTopicModeler<String> sampler) {
				for (int i = 0; i < ptd.length; i++) {
					ptd[i] = sampler.getAlpha(i) / sampler.getAlphaConc();
				}
			}
		};
		calc.setTopicModeler(m);
		// Test document b b b b b: the perplexity decreases with alphaConc.
		DefaultDocumentProvider<String> bOnly = new DefaultDocumentProvider<String>(
				(DefaultVocab<String>) train.getVocab());
		bOnly.newDocument().addWord("b", 5);
		m.setAlphaConc(2);
		double p2 = calc.computePerplexity(bOnly);
		m.setAlphaConc(8);
		assertTrue(calc.computePerplexity(bOnly) < p2);

		calc.optimizeAlphaConc(1, 10, bOnly, 5);
		// Interval halving towards 10: [1,10] -> [5.5,10] -> ... -> [9.71875,10], midpoint:
		assertEquals(9.859375, m.getAlphaConc(), 1e-9);
	}

	public void testPeakValueOptimizer() {
		PeakValueOptimizer opt = new PeakValueOptimizer() {
			@Override
			public double test(double value) {
				return -(value - 3) * (value - 3);
			}
		};
		assertEquals(3, opt.optimizeLambda(0, 10, 20), 1e-4);
	}

	private static class TwoTopicModeler extends AbstractTopicModeler<String> {
		private static final long serialVersionUID = 1L;
		private double alphaConc = 1;

		TwoTopicModeler(Vocab<String> vocab, double[][] phi, double p0) {
			super(vocab, 2);
			for (int k = 0; k < 2; k++) {
				for (int w = 0; w < phi[k].length; w++) {
					this.phi[k][vocab.getIndex(w == 0 ? "a" : w == 1 ? "b" : "c")] = phi[k][w];
				}
			}
			topicProb[0] = p0;
			topicProb[1] = 1 - p0;
		}

		@Override
		public double getWordProb(int wordIndex) {
			throw new UnsupportedOperationException();
		}

		@Override
		public double getAlpha(int i) {
			return topicProb[i] * alphaConc;
		}

		@Override
		public double getAlphaConc() {
			return alphaConc;
		}

		@Override
		public void setAlphaConc(double alphaConc) {
			this.alphaConc = alphaConc;
		}
	}
}
