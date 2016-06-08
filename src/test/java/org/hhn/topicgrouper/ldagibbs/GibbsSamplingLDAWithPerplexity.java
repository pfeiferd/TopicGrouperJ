package org.hhn.topicgrouper.ldagibbs;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;

import cc.mallet.topics.MarginalProbEstimator;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;

public class GibbsSamplingLDAWithPerplexity extends GibbsSamplingLDAAdapt {
	private final InstanceList il;
	private final int dSize;
	private final MarginalProbEstimator mpe;
	private final int[][] wordTopicCount;

	public GibbsSamplingLDAWithPerplexity(
			DocumentProvider<String> documentProvider, double[] inAlpha,
			double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider) throws Exception {
		super(documentProvider, inAlpha, inBeta, inNumIterations, inTopWords,
				inExpName, pathToTAfile, inSaveStep);
		wordTopicCount = new int[vocabularySize][numTopics];
		mpe = new MarginalProbEstimator(numTopics, alpha, alphaSum, beta,
				wordTopicCount, sumTopicWordCount);
		il = new InstanceList();
		Alphabet alphabet = new Alphabet(vocabularySize);
		int sum = 0;
		for (Document<String> d : testDocumentProvider.getDocuments()) {
			FeatureSequence fs = new FeatureSequence(alphabet);
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int f = d.getWordFrequency(index);
				String word = d.getProvider().getWord(index);
				int tIndex = documentProvider.getIndex(word);
				if (tIndex >= 0) {
					for (int i = 0; i < f; i++) {
						fs.add(tIndex);
					}
				}
			}
			il.add(new Instance(fs, null, null, null));
			sum += d.getSize();
		}
		dSize = sum;
	}

	@Override
	protected void afterSampling(int i, int numberOfIterations) {
		if (i > 0 && i % 200 == 0) {
			double d = computePerplexity();
			perplexityComputed(i, d);
		}
	}

	public double computePerplexity() {
		return Math.exp(-computeLogProbabilitySum() / dSize);
	}

	public double computeLogProbabilitySum() {
		// It hurts but we must transpose:
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				wordTopicCount[j][i] = topicWordCount[i][j];
			}
		}

		return mpe.evaluateLeftToRight(il, 10, false, null);
	}

	protected void perplexityComputed(int step, double value) {
		System.out.println(step + ": " + value);
	}
}
