package org.hhn.topicgrouper.util;

import java.util.Random;

public class RandomDirichlet1Dist {
	private final Random random;
	
	public RandomDirichlet1Dist(Random random) {
		this.random = random;
	}
	
	public double[] nextDistribution(int n) {
		double[] res = new double[n];
		double sum = 0;
		
		for (int i = 0; i < n; i++) {
			res[i] = random.nextDouble();
			sum += res[i];
		}
		for (int i = 0; i < n; i++) {
			res[i] = res[i] / sum;
		}
		return res;
	}
}
