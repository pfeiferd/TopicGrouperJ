package org.hhn.topicgrouper.doc.impl;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.MeasuringMethod;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;

public class TrueTopicAccuracyCalculatorTest extends TestCase {
	private static final double[][] TRUE_PWT = { { 0.5, 0.5, 0, 0 }, { 0, 0, 0.5, 0.5 } };

	private DefaultDocumentProvider<String> provider;

	@Override
	protected void setUp() {
		provider = new DefaultDocumentProvider<String>();
		DefaultDocumentProvider<String>.DefaultDocument d = provider.newDocument();
		for (int i = 0; i < 4; i++) {
			d.addWord("w" + i);
		}
	}

	private PwtProvider<String> pwt(final double[][] model) {
		return new PwtProvider<String>() {
			@Override
			public double getPwtFromModel(int topic, int wordIndex) {
				return model[topic][wordIndex];
			}

			@Override
			public double getCorrectPwt(int topic, String word) {
				return TRUE_PWT[topic][Integer.parseInt(word.substring(1))];
			}
		};
	}

	public void testTrueAndPermutedModel() {
		double[][] permuted = { TRUE_PWT[1], TRUE_PWT[0] };
		for (MeasuringMethod m : MeasuringMethod.values()) {
			TrueTopicAccuracyCalculator<String> calc = new TrueTopicAccuracyCalculator<String>();
			calc.setMeasuringMethod(m);
			assertEquals(m, calc.getMeasuringMethod());
			assertEquals(1.0, calc.computeAccuracy(provider, 2, pwt(TRUE_PWT)), 1e-12);
			assertEquals(1.0, calc.computeAccuracy(provider, 2, pwt(permuted)), 1e-12);
		}
	}

	public void testPartiallyWrongModel() {
		// Topic 0 is uniform instead of (0.5, 0.5, 0, 0), topic 1 is correct but
		// given in the other position.
		double[][] model = { TRUE_PWT[1], { 0.25, 0.25, 0.25, 0.25 } };
		TrueTopicAccuracyCalculator<String> calc = new TrueTopicAccuracyCalculator<String>();
		// ABS: topic 0 differs by 4 * 0.25 = 1 (of at most 2), topic 1 by 0.
		assertEquals(1 - 1.0 / 2 / 2, calc.computeAccuracy(provider, 2, pwt(model)), 1e-12);
		calc.setMeasuringMethod(MeasuringMethod.SQUARE);
		// 4 * 0.0625 = 0.25
		assertEquals(1 - 0.25 / 2 / 2, calc.computeAccuracy(provider, 2, pwt(model)), 1e-12);
		calc.setMeasuringMethod(MeasuringMethod.MIN);
		// Overlap 0.5 for topic 0 and 1 for topic 1.
		assertEquals((0.5 + 1) / 2, calc.computeAccuracy(provider, 2, pwt(model)), 1e-12);
	}

	public void testCompletelyWrongModelHasAccuracyZero() {
		double[][] model = { { 0, 0, 0, 1 }, { 1, 0, 0, 0 } };
		double[][] trueModel = { { 1, 0, 0, 0 }, { 0, 1, 0, 0 } };
		final double[][] t = trueModel;
		PwtProvider<String> p = new PwtProvider<String>() {
			@Override
			public double getPwtFromModel(int topic, int wordIndex) {
				return model[topic][wordIndex];
			}

			@Override
			public double getCorrectPwt(int topic, String word) {
				return t[topic][Integer.parseInt(word.substring(1))];
			}
		};
		// Best assignment: model 1 -> true 0 (identical), model 0 -> true 1 (disjoint)
		TrueTopicAccuracyCalculator<String> calc = new TrueTopicAccuracyCalculator<String>();
		assertEquals(0.5, calc.computeAccuracy(provider, 2, p), 1e-12);
		calc.setMeasuringMethod(MeasuringMethod.MIN);
		assertEquals(0.5, calc.computeAccuracy(provider, 2, p), 1e-12);
		// Disjoint in both topics:
		double[][] model2 = { { 0, 0, 0, 1 }, { 0, 0, 1, 0 } };
		final double[][] m2 = model2;
		p = new PwtProvider<String>() {
			@Override
			public double getPwtFromModel(int topic, int wordIndex) {
				return m2[topic][wordIndex];
			}

			@Override
			public double getCorrectPwt(int topic, String word) {
				return t[topic][Integer.parseInt(word.substring(1))];
			}
		};
		for (MeasuringMethod m : MeasuringMethod.values()) {
			calc.setMeasuringMethod(m);
			assertEquals(0.0, calc.computeAccuracy(provider, 2, p), 1e-12);
		}
	}

	public void testInconsistentModelIsRejected() {
		double[][] model = { { 0.5, 0.5, 0.5, 0 }, TRUE_PWT[1] };
		try {
			new TrueTopicAccuracyCalculator<String>().computeAccuracy(provider, 2, pwt(model));
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
}
