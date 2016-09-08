package org.hhn.topicgrouper.util;

public class MathExt {
	public static double avg(double[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}

	public static double sampleStdDev(double[] values) {
		double avg = avg(values);

		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}

	
}
