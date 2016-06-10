package org.hhn.topicgrouper.ldagibbs;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;

public class GibbsSamplingLDAWithPerplexityInDoc extends
		AbstractGibbsSamplingLDAWithPerplexity {
	public GibbsSamplingLDAWithPerplexityInDoc(
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

	public GibbsSamplingLDAWithPerplexityInDoc(
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
		int[] dSize = new int[1];
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		int i = 0;
		for (Document<String> d : provider.getDocuments()) {
			sumA += computeLogProbability(d, dSize, i);
			sumB += dSize[0];
			i++;
		}
		return Math.exp(-sumA / sumB);
	}

	public double computeLogProbability(Document<String> d, int[] dSize,
			int dIndex) {
		double res = 0;
		dSize[0] = 0;
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			String word = d.getProvider().getWord(index);
			int tIndex = getTrainingDocumentProvider().getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					double pw = computeWordProbability(tIndex, wordFr, d,
							dIndex);
					if (pw > 0) {
						res -= logFakN(wordFr);
						res += wordFr * Math.log(pw);
					} else {
						// Do nothing: should not happen, but it does.
						// We simple ignore the probability which is in favour
						// of LDA.
					}
					dSize[0] += wordFr;
				}
			}
		}
		res += logFakN(dSize[0]);
		return res;
	}

	private double computeWordProbability(int tIndex, int fr,
			Document<String> d, int dIndex) {
		double sum = 0;
		for (int i = 0; i < numTopics; i++) {
			sum += (((double) topicWordCount[i][tIndex]) / sumTopicWordCount[i])
					* (((double) docTopicCount[dIndex][i]) / sumDocTopicCount[dIndex]);
		}
		return sum;
	}
}
