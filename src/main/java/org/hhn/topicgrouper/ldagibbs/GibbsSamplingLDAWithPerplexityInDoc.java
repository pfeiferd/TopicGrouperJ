package org.hhn.topicgrouper.ldagibbs;

import gnu.trove.iterator.TIntIterator;

import java.util.Arrays;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.report.BasicGibbsSolutionReporter;

public class GibbsSamplingLDAWithPerplexityInDoc extends GibbsSamplingLDAAdapt {
	private final DocumentProvider<String> trainingDocumentProvider;
	private final DocumentProvider<String> testDocumentProvider;
	private final BasicGibbsSolutionReporter solutionReporter;

	public GibbsSamplingLDAWithPerplexityInDoc(
			DocumentProvider<String> documentProvider, int topics,
			double inAlpha, double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider) throws Exception {
		this(documentProvider, symmetricAlpha(inAlpha, topics), inBeta,
				inNumIterations, inTopWords, inExpName, pathToTAfile,
				inSaveStep, testDocumentProvider);
	}

	public GibbsSamplingLDAWithPerplexityInDoc(
			DocumentProvider<String> documentProvider, double[] inAlpha,
			double inBeta, int inNumIterations, int inTopWords,
			String inExpName, String pathToTAfile, int inSaveStep,
			DocumentProvider<String> testDocumentProvider) throws Exception {
		super(documentProvider, inAlpha, inBeta, inNumIterations, inTopWords,
				inExpName, pathToTAfile, inSaveStep);
		this.testDocumentProvider = testDocumentProvider;
		this.trainingDocumentProvider = documentProvider;
		this.solutionReporter = new BasicGibbsSolutionReporter(System.out);
	}

	public static double[] symmetricAlpha(double alpha, int topics) {
		double[] v = new double[topics];
		Arrays.fill(v, alpha);
		return v;
	}

	@Override
	protected void afterSampling(int i, int numberOfIterations) {
		if (i > 0 && i % 10 == 0) {
			double d = computePerplexity(testDocumentProvider);
			perplexityComputed(i, d);
		}
	}

	protected void perplexityComputed(int step, double value) {
		solutionReporter.perplexityComputed(step, value);
	}

	public double computePerplexity(DocumentProvider<String> provider) {
		double sumA = 0;
		double sumB = 0;
		// Compute the document size excluding words not in the training
		// vocabulary.
		// (Therefore cannot use d.size() ...)
		int i = 0;
		for (Document<String> d : provider.getDocuments()) {
			int dSize = 0;
			for (int j = 0; j < trainingDocumentProvider.getNumberOfWords(); j++) {
				String word = trainingDocumentProvider.getWord(j);
				int index = provider.getIndex(word);
				if (index >= 0) {
					dSize += d.getWordFrequency(index);
				}
			}
			sumA += computeLogProbability(d, dSize, i);
			sumB += dSize;
			i++;
		}
		return Math.exp(-sumA / sumB);
	}

	public double computeLogProbability(Document<String> d, int dSize,
			int dIndex) {
		double res = logFakN(dSize);

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			String word = d.getProvider().getWord(index);
			int tIndex = trainingDocumentProvider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (tIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					double pw = computeWordProbability(tIndex, wordFr, d,
							dIndex);
					if (pw > 0) {
						res -= logFakN(wordFr);
						res += wordFr * Math.log(pw);
					}
//					else {
//						System.out.println("*******************");
//					}
				}
			}
		}
		return res;
	}

	private double computeWordProbability(int tIndex, int fr,
			Document<String> d, int dIndex) {
		double sum = 0;
		for (int i = 0; i < numTopics; i++) {
			sum += ((double) topicWordCount[i][tIndex]) / sumTopicWordCount[i]
					* docTopicCount[dIndex][i] / sumDocTopicCount[dIndex];
		}
		return sum;
	}

	public static double logFakN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}
}
