package org.hhn.topicgrouper.lda.impl;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;

public class SimpleEMAlphaOptimizer<T> {
	protected final Random random;
	
	public SimpleEMAlphaOptimizer(Random random) {
		this.random = random;
	}
	
	public void run(DocumentProvider<T> documentProvider, double concentration, int topics, double beta, double stopEpsilon, int gibbsIterations) {
		double[] oldAlpha = null;
		double[] alpha = initialAlpha(topics);
		while (oldAlpha == null || alphaErr(oldAlpha, alpha) >= stopEpsilon) {
			LDAGibbsSampler<T> gibbsSampler = createGibbsSampler(documentProvider, alpha, beta);
			gibbsSampler.solve(gibbsIterations, null);
			oldAlpha = alpha;
			alpha = newAlpha(concentration, gibbsSampler);
			System.out.println(Arrays.toString(alpha));
			System.out.println(alphaErr(oldAlpha, alpha));
		}
	}
	
	protected double[] initialAlpha(int topics) {
		return LDAGibbsSampler.symmetricAlpha(50.d / topics, topics);
	}
	
	protected LDAGibbsSampler<T> createGibbsSampler(DocumentProvider<T> documentProvider, double[] alpha, double beta) {
		return new LDAGibbsSampler<T>(documentProvider, alpha, beta, random);
	}
		
	protected double[] newAlpha(double concentration, LDAGibbsSampler<T> gibbsSampler) {
		double[] alpha = new double[gibbsSampler.getNTopics()];
		int sum = 0;
		for (int i = 0; i < alpha.length; i++) {
			alpha[i] = gibbsSampler.getTopicFrCount(i);
			sum += alpha[i];
		}
		for (int i = 0; i < alpha.length; i++) {
			alpha[i] = concentration * alpha[i] / sum;
		}
		return alpha;
	}
	
	protected double alphaErr(double[] alpha1, double[] alpha2) {
		double var = 0;
		for (int i = 0; i < alpha1.length; i++) {
			double diff = alpha1[i] - alpha2[i];
			var += diff * diff;
		}
		return Math.sqrt(var);
	}
}
