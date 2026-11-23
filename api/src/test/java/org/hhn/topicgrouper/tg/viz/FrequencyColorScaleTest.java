package org.hhn.topicgrouper.tg.viz;

import java.awt.Color;

import junit.framework.TestCase;

/**
 * The frequency scale must be logarithmic, monotonic and keep the colors light.
 */
public class FrequencyColorScaleTest extends TestCase {
	public void testPositions() {
		FrequencyColorScale scale = new FrequencyColorScale(1000000);
		assertEquals(0.0, scale.position(1), 1e-12);
		assertEquals(0.0, scale.position(0), 1e-12);
		assertEquals(0.5, scale.position(1000), 1e-9);
		assertEquals(1.0, scale.position(1000000), 1e-12);
		assertEquals(1.0, scale.position(5000000), 1e-12);
	}

	public void testColorsRunFromBlueToRedAndStayLight() {
		FrequencyColorScale scale = new FrequencyColorScale(100000);
		int lastRed = -1;
		int lastBlue = 256;
		for (int fr : new int[] { 1, 10, 100, 1000, 10000, 100000 }) {
			Color c = scale.colorOf(fr);
			assertTrue("red grows", c.getRed() > lastRed);
			assertTrue("blue falls", c.getBlue() < lastBlue);
			// Light enough for black text.
			float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
			assertTrue(hsb[2] >= 0.85f);
			lastRed = c.getRed();
			lastBlue = c.getBlue();
		}
		assertEquals(FrequencyColorScale.LOW, scale.colorOf(1));
		assertEquals(FrequencyColorScale.HIGH, scale.colorOf(100000));
		Color low = scale.colorOf(1);
		assertTrue("bluish", low.getBlue() > low.getRed());
		Color high = scale.colorOf(100000);
		assertTrue("reddish", high.getRed() > high.getBlue());
	}

	public void testDegenerateMaximum() {
		FrequencyColorScale scale = new FrequencyColorScale(1);
		assertEquals(0.0, scale.position(1), 1e-12);
		assertEquals(1.0, scale.position(2), 1e-12);
	}
}
