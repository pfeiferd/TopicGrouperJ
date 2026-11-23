package org.hhn.topicgrouper.util;

import junit.framework.TestCase;

public class MathExtTest extends TestCase {
	public void testAvg() {
		assertEquals(5.0, MathExt.avg(new double[] { 2, 4, 4, 4, 5, 5, 7, 9 }), 1e-12);
		assertEquals(5.0, MathExt.avg(new long[] { 2, 4, 4, 4, 5, 5, 7, 9 }), 1e-12);
		assertEquals(5.0, MathExt.avg(new int[] { 2, 4, 4, 4, 5, 5, 7, 9 }), 1e-12);
		assertEquals(1.5, MathExt.avg(new int[] { 1, 2 }), 1e-12);
	}

	public void testSampleStdDev() {
		// Sum of squared deviations is 32, Bessel corrected: 32 / 7
		double expected = Math.sqrt(32.0 / 7);
		assertEquals(expected, MathExt.sampleStdDev(new double[] { 2, 4, 4, 4, 5, 5, 7, 9 }), 1e-12);
		assertEquals(expected, MathExt.sampleStdDev(new long[] { 2, 4, 4, 4, 5, 5, 7, 9 }), 1e-12);
		assertEquals(expected, MathExt.sampleStdDev(new int[] { 2, 4, 4, 4, 5, 5, 7, 9 }), 1e-12);
		assertEquals(expected, MathExt.sampleStdDev(5.0, new double[] { 2, 4, 4, 4, 5, 5, 7, 9 }),
				1e-12);
		assertEquals(0.0, MathExt.sampleStdDev(new double[] { 3, 3, 3 }), 1e-12);
		assertEquals(Math.sqrt(2), MathExt.sampleStdDev(new double[] { 1, 3 }), 1e-12);
		assertTrue(Double.isNaN(MathExt.sampleStdDev(new double[] { 1 })));
	}
}
