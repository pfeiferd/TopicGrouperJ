package org.hhn.topicgrouper.lda.impl;

import java.util.Random;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * LDA Gibbs sampler with a full topic-by-word matrix of beta parameters instead of a single
 * symmetric beta. The matrix can be re-estimated with Minka's fixed point iteration.
 *
 * @param <T> the type of the words
 */
public class LDAFullBetaGibbsSampler<T> extends LDAGibbsSampler<T> {
	/**
	 * fullBeta[t][w], the (unscaled, unsmoothed) Dirichlet prior of word w in topic t.
	 */
	private final double[][] fullBeta;
	/**
	 * Per topic, the sum of {@link #getBeta(int, int)} over all words.
	 */
	private final double[] fullBetaSum;
	/**
	 * Factor by which all beta values are scaled.
	 */
	private double betaConc;
	/**
	 * Weight of the uniform distribution mixed into each row of the beta matrix.
	 */
	private double smoothingMixBeta;

	/**
	 * Buffer for the mean log of the observed topic proportions per topic (Minka's precision update).
	 */
	private double[] logpkAlpha = new double[topicProb.length];
	/**
	 * Smoothing constant added to the counts in the alpha precision update.
	 */
	private double smoothingAlphaPrecision = 0.001;

	/**
	 * Buffer for the mean log of the observed word proportions per word (Minka's precision update).
	 */
	private double[] logpkBeta = new double[nWords];
	/**
	 * Smoothing constant added to the counts in the beta precision update.
	 */
	private double smoothingBetaPrecision = 0.001;

	/**
	 * Whether the updates only change the precision (sum) of alpha and beta.
	 */
	private boolean updatePrecisionOnly;

	/**
	 * Creates a sampler with a beta matrix; the beta concentration is 1 and no smoothing is mixed in.
	 *
	 * @param random source of randomness
	 * @param documentProvider the training documents
	 * @param alpha the Dirichlet prior over the topics of a document
	 * @param fullBeta beta[t][w], the Dirichlet prior of the words in topic t; modified by updates
	 */
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

	/**
	 * Sets the scaling factor of the beta matrix (default 1); takes effect with the next {@link #solve(int, int, LDASolutionListener)}.
	 *
	 * @param betaConc factor by which all beta values are scaled
	 */
	public void setBetaConc(double betaConc) {
		this.betaConc = betaConc;
	}

	/**
	 * Sets the smoothing of the beta matrix (default 0): the effective beta of a word in a topic is
	 * <code>betaConc * (smoothingMixBeta / W + (1 - smoothingMixBeta) * fullBeta[t][w])</code>.
	 *
	 * @param smoothingMixBeta weight of the uniform distribution when mixing it into beta
	 */
	public void setSmoothingMixBeta(double smoothingMixBeta) {
		this.smoothingMixBeta = smoothingMixBeta;
	}

	/**
	 * The smoothing of the beta matrix.
	 *
	 * @return weight of the uniform distribution when mixing it into beta
	 */
	public double getSmoothingMixBeta() {
		return smoothingMixBeta;
	}

	/**
	 * The scaling factor of the beta matrix.
	 *
	 * @return factor by which all beta values are scaled
	 */
	public double getBetaConc() {
		return betaConc;
	}

	/**
	 * Chooses between re-estimating all entries of alpha and beta or only their precision (default false).
	 *
	 * @param updatePrecisionOnly if true, updates only scale alpha and beta (Dirichlet precision) and keep their ratios
	 */
	public void setUpdatePrecisionOnly(boolean updatePrecisionOnly) {
		this.updatePrecisionOnly = updatePrecisionOnly;
	}

	/**
	 * Whether only the precision of alpha and beta is re-estimated.
	 *
	 * @return whether updates only change the precision
	 */
	public boolean isUpdatePrecisionOnly() {
		return updatePrecisionOnly;
	}

	/**
	 * The smoothing constant of the alpha precision update (default 0.001).
	 *
	 * @return the smoothing constant used in the alpha precision update
	 */
	public double getSmoothingAlphaPrecision() {
		return smoothingAlphaPrecision;
	}

	/**
	 * Sets the smoothing constant added to the topic counts of a document in the alpha precision
	 * update, so that empty topics do not produce a log of zero.
	 *
	 * @param smoothingAlphaPrecision the smoothing constant used in the alpha precision update
	 */
	public void setSmoothingAlphaPrecision(double smoothingAlphaPrecision) {
		this.smoothingAlphaPrecision = smoothingAlphaPrecision;
	}

	/**
	 * The smoothing constant of the beta precision update (default 0.001).
	 *
	 * @return the smoothing constant used in the beta precision update
	 */
	public double getSmoothingBetaPrecision() {
		return smoothingBetaPrecision;
	}

	/**
	 * Sets the smoothing constant added to the word counts in the beta precision update, so that
	 * unseen words do not produce a log of zero.
	 *
	 * @param smoothingBetaPrecision the smoothing constant used in the beta precision update
	 */
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

	/**
	 * Performs one fixed point step of Minka, "Estimating a Dirichlet distribution", 2000,
	 * equation 55 on every entry of the beta matrix. For topic t the samples of the Dirichlet
	 * over the words are the documents (the words of document d assigned to t); documents
	 * without words in t are skipped.
	 *
	 * @throws IllegalStateException if the beta concentration is not 1
	 */
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

	/**
	 * Recomputes the row sums of the beta matrix.
	 */
	protected void updateFullBetaSum() {
		for (int i = 0; i < fullBeta.length; i++) {
			fullBetaSum[i] = 0;
			for (int j = 0; j < fullBeta[i].length; j++) {
				fullBetaSum[i] += getBeta(i, j);
			}
		}
	}

	/**
	 * Performs one fixed point step of Minka, "Estimating a Dirichlet distribution", 2000,
	 * equation 33 on the precision s = sum_k alpha_k, keeping the mean m = alpha / s fixed:
	 * s_new = (K-1) / ((K-1)/s - digamma(s) + sum_k m_k digamma(s m_k) - sum_k m_k log p_k),
	 * where log p_k is the average over the documents of the log of the (smoothed) proportion of
	 * words assigned to topic k.
	 */
	protected void updateAlphaPrecision() {
		double smoothingAlphaPrecisionNom = smoothingAlphaPrecision
				* alpha.length;
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

	/**
	 * Performs one fixed point step of Minka, "Estimating a Dirichlet distribution", 2000,
	 * equation 33 on the precision of each row of the beta matrix, keeping the row's mean fixed.
	 * As in {@link #updateFullBeta()}, the samples of the Dirichlet of topic t are the
	 * documents (the words of document i assigned to t), the categories are
	 * the words. Documents without words in t carry no information about the
	 * word distribution of t and are skipped.
	 *
	 * @throws IllegalStateException if the beta concentration is not 1
	 */
	protected void updateBetaPrecision() {
		if (betaConc != 1) {
			throw new IllegalStateException(
					"only works if concentration parameter is not set.");
		}
		double smoothingBetaPrecisionNom = smoothingBetaPrecision * nWords;
		for (int t = 0; t < fullBeta.length; t++) {
			double[] beta = fullBeta[t];
			int samples = 0;
			for (int i = 0; i < documentSize.length; i++) {
				if (documentTopicAssignmentCount[i][t] > 0) {
					samples++;
				}
			}
			if (samples == 0) {
				continue;
			}
			for (int j = 0; j < nWords; j++) {
				logpkBeta[j] = 0;
				for (int i = 0; i < documentSize.length; i++) {
					int nit = documentTopicAssignmentCount[i][t];
					if (nit == 0) {
						continue;
					}
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
									/ (nit + smoothingBetaPrecisionNom));
				}
				logpkBeta[j] = logpkBeta[j] / samples;
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
	/**
	 * Prints the beta matrix to standard output.
	 */
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
