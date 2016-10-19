package org.hhn.topicgrouper.tg.validation;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSolution;

public class TGPerplexityCalculator<T> {
	private final boolean bowFactor;
	private final DocumentSplitter<T> documentSplitter;

	public TGPerplexityCalculator() {
		this(true, new DefaultDocumentSplitter<T>());
	}

	public TGPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider,
			TGSolution<T> s) {
		double sumA = 0;
		long sumB = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			documentSplitter.setDocument(doc);
			int splits = documentSplitter.getSplits();
			for (int i = 0; i < splits; i++) {
				Split<T> nextSplit = documentSplitter.nextSplit();
				Document<T> rd = nextSplit.getRefDoc();
				Document<T> d = nextSplit.getTestDoc();
				sumA += computeLogProbability(rd, d, s);
				sumB += d.getSize();
				// splitLogSum += computeLogProbability(rd, d, s);
				// splitSizeSum += d.getSize();
			}
		}
		return Math.exp(-sumA / sumB);
	}

	public double computeLogProbability(Document<T> refD, Document<T> d,
			TGSolution<T> s) {
		double res = bowFactor ? logFacN(d.getSize()) : 0;

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int sIndex = s.getIndex(word);
			if (sIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					if (bowFactor) {
						res -= logFacN(wordFr);
					}
					int topicIndex = s.getTopicForWord(sIndex);
					TIntCollection words = s.getTopics()[topicIndex];
					res += wordFr
							* computeWordLogProbability(sIndex, refD, s, words,
									topicIndex);
				}
			}
		}
		return res;
	}

	protected double computeWordLogProbability(int sIndex, Document<T> refD,
			TGSolution<T> s, TIntCollection words, int topicIndex) {
		// Estimate log p(w|t):
		double logpwt = Math.log(((double) s.getGlobalWordFrequency(sIndex))
				/ s.getTopicFrequency(topicIndex));
		double logptd;

		if (refD == null) {
			// No reference document available: Estimat log ptd via log pt
			logptd = Math.log(((double) s.getTopicFrequency(topicIndex))
					/ s.getSize());
		} else {
			int topicFrInDoc = 0;
			TIntIterator it = words.iterator();
			while (it.hasNext()) {
				int swIndex = it.next();
				int dIndex = refD.getProvider().getIndex(s.getWord(swIndex));
				if (dIndex >= 0) {
					topicFrInDoc += refD.getWordFrequency(dIndex);
				}
			}
			// Estimate log p(t|d)
			logptd = Math.log(smoothedPtd(topicFrInDoc, refD.getSize(), sIndex,
					topicIndex, s));
		}
		// if (logpwt > 0 || logptd > 0) {
		// throw new IllegalStateException("Is there bug?");
		// }
		return logpwt + logptd;
	}

	protected double smoothedPtd(int topicFrInDoc, int docSize, int wordIndex,
			int topicIndex, TGSolution<T> s) {
		double lambda = getSmoothingLambda(s);
		return (((1 - lambda) * topicFrInDoc) / docSize)
		// Smoothing via global frequency of topic;
				+ (lambda * s.getTopicFrequency(topicIndex)) / s.getSize();
	}

	protected double getSmoothingLambda(TGSolution<T> s) {
		return 0.5;
	}

	public static double logFacN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}
}
