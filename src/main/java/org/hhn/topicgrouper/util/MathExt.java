package org.hhn.topicgrouper.util;

public class MathExt {
	public static double avg(double[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}

	public static double avg(long[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}
	
	public static double avg(int[] values) {
		double sum1 = 0;
		for (int j = 0; j < values.length; j++) {
			sum1 += values[j];
		}

		return sum1 / values.length;
	}
	
	public static double sampleStdDev(double avg, double[] values) {		
		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}
	
	public static double sampleStdDev(double avg, long[] values) {		
		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}
	
	public static double sampleStdDev(double avg, int[] values) {		
		double sum = 0;
		for (int j = 0; j < values.length; j++) {
			sum += (values[j] - avg) * (values[j] - avg);
		}
		return Math.sqrt(sum / (values.length - 1));
	}
	
	public static double sampleStdDev(double[] values) {
		double avg = avg(values);
		return sampleStdDev(avg, values);
	}
	
	public static double sampleStdDev(long[] values) {
		double avg = avg(values);
		return sampleStdDev(avg, values);
	}
	
	public static double sampleStdDev(int[] values) {
		double avg = avg(values);
		return sampleStdDev(avg, values);
	}
}
