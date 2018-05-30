package org.hhn.topicgrouper.lda.impl;

import java.util.Random;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class LDAFullBetaGibbsSampler<T> extends LDAGibbsSampler<T> {
	private final double[][] fullBeta;
	private final double[] fullBetaSum;
	private double betaConc;
	private double smoothingMixBeta;

	private double[] logpkAlpha = new double[topicProb.length];
	private double smoothingAlphaPrecision = 0.001;
	private double smoothingAlphaPrecisionNom = smoothingAlphaPrecision
			* topicProb.length;

	private double[] logpkBeta = new double[nWords];
	private double smoothingBetaPrecision = 0.001;
	private double smoothingBetaPrecisionNom = smoothingBetaPrecision
			* topicProb.length;

	private boolean updatePrecisionOnly;

	public LDAFullBetaGibbsSampler(Random random,
			DocumentProvider<T> documentProvider, double[] alpha,
			double[][] fullBeta) {
		super(random, documentProvider, alpha, 0);
		this.fullBeta = fullBeta;
		updatePrecisionOnly = false;
		fullBetaSum = new double[fullBeta.length];
		betaConc = 1;
		smoothingMixBeta = 0;
		updateFullBetaSum();
	}

	public void setBetaConc(double betaConc) {
		this.betaConc = betaConc;
	}

	public void setSmoothingMixBeta(double smoothingMixBeta) {
		this.smoothingMixBeta = smoothingMixBeta;
	}

	public double getSmoothingMixBeta() {
		return smoothingMixBeta;
	}

	public double getBetaConc() {
		return betaConc;
	}

	public void setUpdatePrecisionOnly(boolean updatePrecisionOnly) {
		this.updatePrecisionOnly = updatePrecisionOnly;
	}

	public boolean isUpdatePrecisionOnly() {
		return updatePrecisionOnly;
	}

	public double getSmoothingAlphaPrecision() {
		return smoothingAlphaPrecision;
	}

	public void setSmoothingAlphaPrecision(double smoothingAlphaPrecision) {
		this.smoothingAlphaPrecision = smoothingAlphaPrecision;
	}

	public double getSmoothingBetaPrecision() {
		return smoothingBetaPrecision;
	}

	public void setSmoothingBetaPrecision(double smoothingBetaPrecision) {
		this.smoothingBetaPrecision = smoothingBetaPrecision;
	}

	@Override
	protected void updateAlpha() {
		if (updatePrecisionOnly) {
			updateAlphaPrecision();
		} else {
			super.updateAlpha();
		}
	}

	@Override
	public double getBeta(int topicIndex, int wordIndex) {
		return betaConc * (smoothingMixBeta / fullBeta[topicIndex].length + (1 - smoothingMixBeta) * fullBeta[topicIndex][wordIndex]);
	}

	@Override
	public double getBetaSum(int topicIndex) {
		return fullBetaSum[topicIndex];
	}
	
	@Override
	protected void initialize(LDASolutionListener<T> listener) {
		updateFullBetaSum();
		super.initialize(listener);
	}

	@Override
	protected void updateBeta() {
		if (updatePrecisionOnly) {
			updateBetaPrecision();
		} else {
			updateFullBeta();
		}
	}

	protected void updateFullBeta() {
		if (betaConc != 1) {
			throw new IllegalStateException(
					"only works if concentration parameter is not set.");
		}
		for (int j = 0; j < topicFrCount.length; j++) {
			double[] beta = fullBeta[j];
			double sumBeta = fullBetaSum[j];

			double sumDigammaNSumBeta = 0;
			int nonZeroCasesA = 0;
			for (int i = 0; i < documentSize.length; i++) {
				if (documentTopicAssignmentCount[i][j] > 0) {
					sumDigammaNSumBeta += Gamma
							.digamma(documentTopicAssignmentCount[i][j]
									+ sumBeta);
					nonZeroCasesA++;
				}
			}
			double diff = sumDigammaNSumBeta - nonZeroCasesA
					* Gamma.digamma(sumBeta);

			for (int k = 0; k < nWords; k++) {
				double sumDigammaNiBeta = 0;
				int nonZeroCases = 0;
				for (int i = 0; i < documentSize.length; i++) {
					int[] assignment = documentWordOccurrenceLastTopicAssignment[i]
							.get(k);
					int count = 0;
					if (assignment != null) {
						for (int h = 0; h < assignment.length; h++) {
							if (assignment[h] == j) {
								count++;
							}
						}
					}
					if (count > 0) {
						sumDigammaNiBeta += Gamma.digamma(count + beta[k]);
						nonZeroCases++;
					}
				}
				beta[k] *= nonZeroCases == 0 ? 0
						: (sumDigammaNiBeta - nonZeroCases
								* Gamma.digamma(beta[k]))
								/ diff;
			}
		}
		updateFullBetaSum();
	}

	protected void updateFullBetaSum() {
		for (int i = 0; i < fullBeta.length; i++) {
			fullBetaSum[i] = 0;
			for (int j = 0; j < fullBeta[i].length; j++) {
				fullBetaSum[i] += getBeta(i, j);
			}
		}
	}

	/*
	 * Update on the basis of equation 33 from
	 * "Estimating a Dirichlet distribution by Thomas P. Minka"
	 */
	protected void updateAlphaPrecision() {
		for (int j = 0; j < alpha.length; j++) {
			logpkAlpha[j] = 0;
			for (int i = 0; i < documentSize.length; i++) {
				logpkAlpha[j] += Math
						.log((documentTopicAssignmentCount[i][j] + smoothingAlphaPrecision)
								/ (documentSize[i] + smoothingAlphaPrecisionNom));
			}
			logpkAlpha[j] = logpkAlpha[j] / documentSize.length;
		}

		double s = 0;
		for (int i = 0; i < alpha.length; i++) {
			s += alpha[i];
		}
		for (int i = 0; i < alpha.length; i++) {
			alpha[i] /= s;
		}
		double sum1 = 0;
		double sum2 = 0;
		for (int k = 0; k < alpha.length; k++) {
			if (alpha[k] > 0) {
				sum1 += alpha[k] * Gamma.digamma(s * alpha[k]);
				sum2 += alpha[k] * logpkAlpha[k];
			}
		}

		double h = (alpha.length - 1) / s - Gamma.digamma(s) + sum1 - sum2;
		s = (alpha.length - 1) / h;

		for (int i = 0; i < alpha.length; i++) {
			alpha[i] *= s;
		}
		alphaSum = alphaSum();
	}

	/*
	 * Update on the basis of equation 33 from
	 * "Estimating a Dirichlet distribution by Thomas P. Minka"
	 */
	protected void updateBetaPrecision() {
		if (betaConc != 1) {
			throw new IllegalStateException(
					"only works if concentration parameter is not set.");
		}
		for (int t = 0; t < fullBeta.length; t++) {
			double[] beta = fullBeta[t];
			for (int j = 0; j < nWords; j++) {
				logpkBeta[j] = 0;
				for (int i = 0; i < documentSize.length; i++) {
					int[] assignment = documentWordOccurrenceLastTopicAssignment[i]
							.get(j);
					int count = 0;
					if (assignment != null) {
						for (int h = 0; h < assignment.length; h++) {
							if (assignment[h] == t) {
								count++;
							}
						}
					}
					logpkBeta[j] += Math
							.log((count + smoothingBetaPrecision)
									/ ((assignment == null ? 0
											: assignment.length) + smoothingBetaPrecisionNom));
				}
				logpkBeta[j] = logpkBeta[j] / nWords;
			}

			double s = 0;
			for (int i = 0; i < beta.length; i++) {
				s += beta[i];
			}
			for (int i = 0; i < beta.length; i++) {
				beta[i] /= s;
			}
			double sum1 = 0;
			double sum2 = 0;
			for (int k = 0; k < beta.length; k++) {
				if (beta[k] > 0) {
					sum1 += beta[k] * Gamma.digamma(s * beta[k]);
					sum2 += beta[k] * logpkBeta[k];
				}
			}

			double h = (beta.length - 1) / s - Gamma.digamma(s) + sum1 - sum2;
			s = (beta.length - 1) / h;

			for (int i = 0; i < beta.length; i++) {
				beta[i] *= s;
			}
		}
		updateFullBetaSum();
	}

	// @Override
	protected void reportBeta() {
		for (int i = 0; i < fullBeta.length; i++) {
			for (int j = 0; j < nWords; j++) {
				T word = provider.getVocab().getWord(j);
				System.out.print(word + "=" + fullBeta[i][j] + " ");
			}
			System.out.println();
		}
	}
}
