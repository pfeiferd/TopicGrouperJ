package org.hhn.topicgrouper.lda.impl;

import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;

public class SimpleEMAlphaBetaOptimizer<T> {
	protected final Random random;

	public SimpleEMAlphaBetaOptimizer(Random random) {
		this.random = random;
	}

	public void run(DocumentProvider<T> documentProvider, double concAlpha,
			double concBeta, int topics, double stopEpsilon, int gibbsIterations) {
		double[] alpha = initialAlpha(concAlpha, topics);
		double[] newAlpha = new double[topics];
		
		double[][] fullBeta = initialBeta(concBeta, topics, documentProvider);
		double[] newBetaI = new double[fullBeta[0].length];
		
		double alphaErr = Double.POSITIVE_INFINITY;
		double betaErr = Double.POSITIVE_INFINITY;
		
		while (alphaErr >= stopEpsilon || betaErr >= stopEpsilon) {
			LDAFullBetaGibbsSampler<T> gibbsSampler = createGibbsSampler(
					documentProvider, alpha, fullBeta);
			gibbsSampler.solve(gibbsIterations, null);
			
			alphaErr = newAlpha(alpha, newAlpha, concAlpha, gibbsSampler);
			double[] oldAlpha = alpha;
			alpha = newAlpha;
			newAlpha = oldAlpha;
			
			betaErr = newBeta(fullBeta, newBetaI, concBeta, gibbsSampler);
			
			System.out.println(Arrays.toString(alpha));
			System.out.println(alphaErr);
		}
	}

	protected double[] initialAlpha(double concAlpha, int topics) {
		return LDAGibbsSampler.symmetricAlpha(concAlpha / topics, topics);
	}

	protected double[][] initialBeta(double concBeta, int topics,
			DocumentProvider<T> documentProvider) {
		double[][] res = new double[topics][documentProvider.getNumberOfWords()];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[i].length; j++) {
				res[i][j] = concBeta / res[i].length;
			}
		}
		return res;
	}

	protected LDAFullBetaGibbsSampler<T> createGibbsSampler(
			DocumentProvider<T> documentProvider, double[] alpha,
			double[][] beta) {
		return new LDAFullBetaGibbsSampler<T>(documentProvider, alpha, beta,
				random);
	}

	protected double newAlpha(double[] alpha, double[] newAlpha, double concentration,
			LDAGibbsSampler<T> gibbsSampler) {
		int sum = 0;
		for (int i = 0; i < alpha.length; i++) {
			newAlpha[i] = gibbsSampler.getTopicFrCount(i);
			sum += alpha[i];
		}
		for (int i = 0; i < alpha.length; i++) {
			newAlpha[i] = newAlpha[i] / sum;
		}
		
		double newConc = newConc(alpha, newAlpha, concentration);
		
		for (int i = 0; i < alpha.length; i++) {
			newAlpha[i] = newConc * newAlpha[i];
		}
		
		return distance(alpha, newAlpha);
	}

	protected double newBeta(double[][] fullBeta, double[] newBetaI, double concentration,
			LDAGibbsSampler<T> gibbsSampler) {
		double distanceSum = 0;
		for (int i = 0; i < fullBeta.length; i++) {
			distanceSum += newAlpha(fullBeta[i], newBetaI, concentration, gibbsSampler);
			double[] oldBetaI = fullBeta[i];
			fullBeta[i] = newBetaI;
			newBetaI = oldBetaI;
		}
		return distanceSum;
	}

	protected double distance(double[] alpha1, double[] alpha2) {
		double var = 0;
		for (int i = 0; i < alpha1.length; i++) {
			double diff = alpha1[i] - alpha2[i];
			var += diff * diff;
		}
		return Math.sqrt(var);
	}
	
	// TODO: Compute newConc accoring to
	// http://jonathan-huang.org/research/dirichlet/dirichlet.pdf
	// Section 3.4.2.
	protected double newConc(double[] a, double[] b, double concentration) {
		return concentration;
	}
}
