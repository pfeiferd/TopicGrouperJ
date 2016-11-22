package org.hhn.topicgrouper.lda.impl;

import java.util.Random;

import org.apache.commons.math3.special.Gamma;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class LDAFullBetaGibbsSampler<T> extends LDAGibbsSampler<T> {
	private final double[][] fullBeta;
	private final double[] fullBetaSum;

	public LDAFullBetaGibbsSampler(DocumentProvider<T> documentProvider,
			double[] alpha, double[][] fullBeta, Random random) {
		super(documentProvider, alpha, 0, random);
		this.fullBeta = fullBeta;
		fullBetaSum = new double[fullBeta.length];
		for (int i = 0; i < fullBeta.length; i++) {
			for (int j = 0; j < fullBeta[i].length; j++) {
				fullBetaSum[i] += fullBeta[i][j];
			}
		}
	}

	@Override
	public double getBeta(int topicIndex, int wordIndex) {
		return fullBeta[topicIndex][wordIndex];
	}

	@Override
	public double getBetaSum(int topicIndex) {
		return fullBetaSum[topicIndex];
	}
	
	@Override
	protected void updateBeta() {
		updateFullBeta(fullBeta, fullBetaSum);
	}

	public void updateFullBeta(double[][] fullBeta, double[] fullBetaSum) {
		for (int j = 0; j < topicFrCount.length; j++) {
			double[] alpha = fullBeta[j];
			double sumAlpha = fullBetaSum[j];

			double sumDigammaNSumAlpha = 0;
			for (int i = 0; i < documentSize.length; i++) {
				sumDigammaNSumAlpha += Gamma
						.digamma(documentTopicAssignmentCount[j][i] + sumAlpha);
			}
			double diff = sumDigammaNSumAlpha - documentSize.length
					* Gamma.digamma(sumAlpha);

			for (int k = 0; k < nWords; k++) {
				double sumDigammaNiAlpha = 0;
				for (int i = 0; i < documentSize.length; i++) {
					int[] assignment = documentWordOccurrenceLastTopicAssignment[i][k];
					int count = 0;
					for (int h = 0; h < assignment.length; h++) {
						if (assignment[j] == j) {
							count++;
						}
					}

					sumDigammaNiAlpha += Gamma.digamma(count + alpha[k]);
				}
				alpha[k] *= (sumDigammaNiAlpha - documentSize.length
						* Gamma.digamma(alpha[k]))
						/ diff;
			}
		}

		for (int i = 0; i < fullBeta.length; i++) {
			for (int j = 0; j < fullBeta[i].length; j++) {
				fullBetaSum[i] += fullBeta[i][j];
			}
		}
	}
}
