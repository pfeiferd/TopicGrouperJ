package org.hhn.topicgrouper.bin;

import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;

// Doesn't work well at all (may be of some use in the future?)
public class SimpleEMAlphaBetaOptimizer<T> {
	protected final Random random;

	public SimpleEMAlphaBetaOptimizer(Random random) {
		this.random = random;
	}

	public void run(DocumentProvider<T> documentProvider, double concAlpha,
			double concBeta, int topics, double stopEpsilon, int gibbsIterations) {
		double[] alpha = initialAlpha(concAlpha, topics);
		double[] newAlpha = new double[topics];

		// double[][] fullBeta = initialBeta(concBeta, topics,
		// documentProvider);
		// double[] newBetaI = new double[fullBeta[0].length];

		double alphaErr = Double.POSITIVE_INFINITY;
		double betaErr = Double.POSITIVE_INFINITY;

		while (alphaErr >= stopEpsilon || betaErr >= stopEpsilon) {
			LDAGibbsSampler<T> gibbsSampler = createGibbsSampler(
					documentProvider, alpha /* , fullBeta */);
			gibbsSampler.solve(gibbsIterations / 4, gibbsIterations, null);

			alphaErr = newAlpha(alpha, newAlpha, concAlpha, gibbsSampler);
			double[] oldAlpha = alpha;
			alpha = newAlpha;
			newAlpha = oldAlpha;

			// betaErr = newBeta(fullBeta, newBetaI, concBeta, gibbsSampler);

			System.out.println(Arrays.toString(alpha));
			System.out.println(alphaErr);
		}
		System.out.println("done");
	}

	protected double[] initialAlpha(double concAlpha, int topics) {
		return LDAGibbsSampler.symmetricAlpha(concAlpha, topics);
	}

	protected double[][] initialBeta(double concBeta, int topics,
			DocumentProvider<T> documentProvider) {
		double[][] res = new double[topics][documentProvider.getVocab().getNumberOfWords()];
		for (int i = 0; i < res.length; i++) {
			for (int j = 0; j < res[i].length; j++) {
				res[i][j] = concBeta / res[i].length;
			}
		}
		return res;
	}

	protected LDAGibbsSampler<T> createGibbsSampler(
			DocumentProvider<T> documentProvider, double[] alpha
	/* , double[][] beta */) {
		return new LDAGibbsSampler<T>(random, documentProvider, alpha, 1);
	}

	// protected double newBetaI(int topic, double[] alpha, double[] newAlpha,
	// double concentration, LDAGibbsSampler<T> gibbsSampler) {
	// int sum = 0;
	// for (int i = 0; i < alpha.length; i++) {
	// newAlpha[i] = gibbsSampler.getTopicWordAssignmentCount(topic, i) + 0.001;
	// // smoothing
	// sum += newAlpha[i];
	// }
	// for (int i = 0; i < alpha.length; i++) {
	// newAlpha[i] = newAlpha[i] / sum;
	// }
	// double res = distance(alpha, newAlpha);
	//
	// double newConc = newConc(alpha, newAlpha, concentration);
	// for (int i = 0; i < alpha.length; i++) {
	// newAlpha[i] = newConc * newAlpha[i];
	// }
	//
	// return res;
	// }

	protected double newAlpha(double[] alpha, double[] newAlpha,
			double concentration, LDAGibbsSampler<T> gibbsSampler) {
		int sum = 0;
		for (int i = 0; i < alpha.length; i++) {
			newAlpha[i] = gibbsSampler.getTopicFrCount(i) + 0.001; // Smoothing
			sum += newAlpha[i];
		}
		for (int i = 0; i < alpha.length; i++) {
			newAlpha[i] = newAlpha[i] / sum;
		}
		double res = distance(alpha, newAlpha);

		double newConc = newConc(alpha, newAlpha, concentration);
		for (int i = 0; i < alpha.length; i++) {
			newAlpha[i] = newConc * newAlpha[i];
		}

		return res;
	}

	// protected double newBeta(double[][] fullBeta, double[] newBetaI,
	// double concentration, LDAGibbsSampler<T> gibbsSampler) {
	// double distanceSum = 0;
	// for (int i = 0; i < fullBeta.length; i++) {
	// distanceSum += newBetaI(i, fullBeta[i], newBetaI, concentration,
	// gibbsSampler);
	// double[] oldBetaI = fullBeta[i];
	// fullBeta[i] = newBetaI;
	// newBetaI = oldBetaI;
	// }
	// return distanceSum;
	// }

	// Kullback–Leibler divergence
	protected double distance(double[] alpha1, double[] alpha2) {
		double sum = 0;
		for (int i = 0; i < alpha1.length; i++) {
			sum += alpha1[i];
		}

		double kld = 0;
		for (int i = 0; i < alpha1.length; i++) {
			if (alpha2[i] != 0 && alpha1[i] != 0) {
				kld += alpha2[i] * Math.log(alpha2[i] / (alpha1[i] / sum));
			}
		}
		return kld;

		// double var = 0;
		// for (int i = 0; i < alpha1.length; i++) {
		// double diff = alpha1[i] - alpha2[i];
		// var += diff * diff;
		// }
		// return Math.sqrt(var);
	}

	// Compute newConc according to
	// http://jonathan-huang.org/research/dirichlet/dirichlet.pdf
	// Section 3.4.2.
	// This doesn't work at all but returns negative results --> Because
	// the formula is wrong (minus exchanged for plus).
	// 
	// Now using using THIS instead:
	// http://www.msr-waypoint.com/en-us/um/people/minka/papers/dirichlet/minka-dirichlet.pdf
	// Section 2.1 Formula (31), (32) and (40)
	// TODO: Use Formula (42) as well for initialization (??)
	protected double newConc(double[] a, double[] b, double concentration) {
		// return concentration;
		double sum = 0;
		for (int i = 0; i < a.length; i++) {
			sum += a[i];
		}

		double s = sum;
		for (int j = 1; j < 20; j++) {
			double h1 = Gamma.digamma(s);
			double h2 = Gamma.trigamma(s);
			for (int k = 0; k < a.length; k++) {
				double mk = (a[k] / sum);
				h1 -= mk * (Gamma.digamma(a[k]) - Math.log(b[k]));
				h2 -= mk * mk * (Gamma.trigamma(mk * s));
			}

			s = 1 / (1 / s + h1 / (s * s * h2));
		}
		return s;
	}
}
