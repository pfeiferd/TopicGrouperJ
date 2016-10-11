package org.hhn.topicgrouper.lda.impl;

import java.util.Random;

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
}
