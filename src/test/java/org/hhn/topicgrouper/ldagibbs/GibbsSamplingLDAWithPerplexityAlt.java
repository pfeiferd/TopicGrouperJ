package org.hhn.topicgrouper.ldagibbs;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;

public class GibbsSamplingLDAWithPerplexityAlt extends
		AbstractGibbsSamplingLDAWithPerplexity {
	public GibbsSamplingLDAWithPerplexityAlt(
			DocumentProvider<String> documentProvider, int topics,
			double inAlpha, double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider, int ppSteps)
			throws Exception {
		this(new BasicGibbsSolutionReporter(System.out), documentProvider,
				symmetricAlpha(inAlpha, topics), inBeta, inNumIterations,
				inTopWords, inExpName, pathToTAfile, inSaveStep,
				testDocumentProvider, ppSteps);
	}

	public GibbsSamplingLDAWithPerplexityAlt(
			BasicGibbsSolutionReporter solutionReporter,
			DocumentProvider<String> documentProvider, double[] inAlpha,
			double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider, int ppSteps)
			throws Exception {
		super(solutionReporter, documentProvider, inAlpha, inBeta,
				inNumIterations, inTopWords, inExpName, pathToTAfile,
				inSaveStep, testDocumentProvider, ppSteps);
	}

	public double computePerplexity(DocumentProvider<String> provider) {
		double sumA = 0;
		double sumB = 0;
		DocumentProvider<String> trainingDocumentProvider = getTrainingDocumentProvider();
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		for (Document<String> d : provider.getDocuments()) {
			int dSize = 0;
			for (int j = 0; j < trainingDocumentProvider.getNumberOfWords(); j++) {
				String word = trainingDocumentProvider.getWord(j);
				int index = provider.getIndex(word);
				if (index >= 0) {
					dSize += d.getWordFrequency(index);
				}
			}
			sumA += computeLogProbability(d, dSize);
			sumB += dSize;
		}
		return Math.exp(-sumA / sumB);
	}

	private double[] ptd = new double[numTopics];

	public double computeLogProbability(Document<String> d, int dSize) {
		double res = bowFactor ? logFakN(dSize) : 0;
		DocumentProvider<String> trainingDocumentProvider = getTrainingDocumentProvider();

		// update ptd for d
		for (int i = 0; i < numTopics; i++) {
			ptd[i] = 0;
			for (int j = 0; j < trainingDocumentProvider.getNumberOfWords(); j++) {
				String word = trainingDocumentProvider.getWord(j);
				int index = d.getProvider().getIndex(word);
				if (index >= 0) {
					ptd[i] += ((double) d.getWordFrequency(index))
							* topicWordCount[i][j]
							/ trainingDocumentProvider.getWordFrequency(j);
				}
			}
			ptd[i] /= dSize;
		}

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			String word = d.getProvider().getWord(index);
			int tIndex = trainingDocumentProvider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					if (bowFactor) {
						res -= logFakN(wordFr);
					}
					res += wordFr
							* computeWordLogProbability(tIndex, wordFr, d);
				}
			}
		}
		return res;
	}

	private double computeWordLogProbability(int tIndex, int fr,
			Document<String> d) {
		double sum = 0;
		for (int i = 0; i < numTopics; i++) {
			sum += ((double) topicWordCount[i][tIndex]) / sumTopicWordCount[i]
					* ptd[i];
		}
		return Math.log(sum);
	}
}
