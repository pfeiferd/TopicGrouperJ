package org.hhn.topicgrouper.tg.viz;

import java.awt.Color;

/**
 * Maps topic frequencies to background colors for the hierarchy browser: a
 * gradual scale from a light blue (a single occurrence) over lavender to a
 * light red (the frequency of the whole corpus). The scale is logarithmic,
 * because topic frequencies span several orders of magnitude, and all colors
 * stay light enough for dark text.
 */
public class FrequencyColorScale {
	/** The color of the lowest frequency (light blue). */
	public static final Color LOW = new Color(190, 215, 255);
	/** The color of the highest frequency (light red). */
	public static final Color HIGH = new Color(255, 180, 170);

	private final double logMax;

	/**
	 * Creates the scale.
	 *
	 * @param maxFrequency
	 *            the frequency that gets the strongest color (the root of the
	 *            hierarchy, i.e. the number of occurrences in the corpus)
	 */
	public FrequencyColorScale(int maxFrequency) {
		logMax = Math.log(Math.max(2, maxFrequency));
	}

	/**
	 * The position of a frequency on the scale.
	 *
	 * @param frequency
	 *            a topic frequency
	 * @return <code>log(frequency) / log(maxFrequency)</code> clamped to 0..1
	 */
	public double position(int frequency) {
		double t = Math.log(Math.max(1, frequency)) / logMax;
		return t < 0 ? 0 : (t > 1 ? 1 : t);
	}

	/**
	 * The color of a frequency.
	 *
	 * @param frequency
	 *            a topic frequency
	 * @return the background color
	 */
	public Color colorOf(int frequency) {
		double t = position(frequency);
		return new Color(mix(LOW.getRed(), HIGH.getRed(), t), mix(LOW.getGreen(), HIGH.getGreen(), t), mix(
				LOW.getBlue(), HIGH.getBlue(), t));
	}

	private static int mix(int a, int b, double t) {
		return (int) Math.round(a + (b - a) * t);
	}
}
