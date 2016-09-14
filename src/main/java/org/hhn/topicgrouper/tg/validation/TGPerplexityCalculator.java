package org.hhn.topicgrouper.tg.validation;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

public class TGPerplexityCalculator<T> {
	private final boolean bowFactor;

	public TGPerplexityCalculator() {
		this(true);
	}

	public TGPerplexityCalculator(boolean bowFactor) {
		this.bowFactor = bowFactor;
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider, TGSolution<T> s) {
		double sumA = 0;
		long sumB = 0;
		for (Document<T> d : testDocumentProvider.getDocuments()) {
			int dSize = 0;
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				T word = testDocumentProvider.getWord(index);
				int sIndex = s.getIndex(word);
				if (sIndex >= 0) {
					dSize += d.getWordFrequency(index);
				}
			}
			sumA += computeLogProbability(d, dSize, s);
			sumB += dSize;
		}
		return Math.exp(-sumA / sumB);
	}

	public double computeLogProbability(Document<T> d, int dSize, TGSolution<T> s) {
		double res = bowFactor ? logFacN(dSize) : 0;

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int sIndex = s.getIndex(word);
			if (sIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0 /*&& words != null*/) {
					if (bowFactor) {
						res -= logFacN(wordFr);
					}
					int topicIndex = s.getTopicForWord(sIndex);
					TIntCollection words = s.getTopics()[topicIndex];
					res += wordFr
							* computeWordLogProbability(sIndex, d, dSize, s,
									words, topicIndex);
				}
			}
		}
		return res;
	}

	private double computeWordLogProbability(int sIndex, Document<T> d,
			int dSize, TGSolution<T> s, TIntCollection words, int topicIndex) {
		int topicFrInDoc = 0;
		TIntIterator it = words.iterator();
		while (it.hasNext()) {
			int swIndex = it.next();
			int dIndex = d.getProvider().getIndex(s.getWord(swIndex));
			if (dIndex >= 0) {
				topicFrInDoc += d.getWordFrequency(dIndex);
			}
		}
		return Math.log(topicFrInDoc)
				+ Math.log(s.getGlobalWordFrequency(sIndex)) - Math.log(dSize)
				- Math.log(s.getTopicFrequency(topicIndex));
	}

	public static double logFacN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}
}
