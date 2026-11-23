package org.hhn.topicgrouper.util;

/**
 * Simple statistics helpers for arrays.
 */
public class MathExt {
	/**
	 * Creates an instance; all methods are static, so none is needed.
	 */
	public MathExt() {
	}

	/**
	 * The arithmetic mean of the values.
	 *
	 * @param values the values
	 * @return their arithmetic mean
	 */
	public static double avg(double[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}

	/**
	 * The arithmetic mean of the values.
	 *
	 * @param values the values
	 * @return their arithmetic mean
	 */
	public static double avg(long[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}

	/**
	 * The arithmetic mean of the values.
	 *
	 * @param values the values
	 * @return their arithmetic mean
	 */
	public static double avg(int[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}
	
	/**
	 * The sample standard deviation (with Bessel correction). NaN for fewer than two values.
	 *
	 * @param avg the mean of the values
	 * @param values the values
	 * @return the standard deviation
	 */
	public static double sampleStdDev(double avg, double[] values) {
		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}
	
	/**
	 * The sample standard deviation (with Bessel correction).
	 *
	 * @param avg the mean of the values
	 * @param values the values
	 * @return the standard deviation
	 */
	public static double sampleStdDev(double avg, long[] values) {		
		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}
	
	/**
	 * The sample standard deviation (with Bessel correction).
	 *
	 * @param avg the mean of the values
	 * @param values the values
	 * @return the standard deviation
	 */
	public static double sampleStdDev(double avg, int[] values) {		
		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}
	
	/**
	 * The sample standard deviation, computing the mean first.
	 *
	 * @param values the values
	 * @return the standard deviation
	 */
	public static double sampleStdDev(double[] values) {
		double avg = avg(values);
		return sampleStdDev(avg, values);
	}
	
	/**
	 * The sample standard deviation, computing the mean first.
	 *
	 * @param values the values
	 * @return the standard deviation
	 */
	public static double sampleStdDev(long[] values) {
		double avg = avg(values);
		return sampleStdDev(avg, values);
	}
	
	/**
	 * The sample standard deviation, computing the mean first.
	 *
	 * @param values the values
	 * @return the standard deviation
	 */
	public static double sampleStdDev(int[] values) {
		double avg = avg(values);
		return sampleStdDev(avg, values);
	}
}
