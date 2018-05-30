package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;

public class BasicPerplexityCalculator<T> {
	protected final boolean bowFactor;
	protected final DocumentSplitter<T> documentSplitter;
	protected final double[] sValues;
	protected double[] ptd;

	protected AbstractTopicModeler<T> topicModeler;

	public BasicPerplexityCalculator(boolean bowFactor) {
		this(bowFactor, new DefaultDocumentSplitter<T>(), 1);
	}

	public BasicPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int samplingMax) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
		sValues = new double[samplingMax];
	}
	
	public AbstractTopicModeler<T> getTopicModeler() {
		return topicModeler;
	}

	public void setTopicModeler(AbstractTopicModeler<T> topicModeler) {
		this.topicModeler = topicModeler;
		ptd = new double[topicModeler.getNTopics()];
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider) {
		if (!testDocumentProvider.getVocab().equals(topicModeler.getVocab())) {
			throw new IllegalStateException(
					"training and test vocab not identical");
		}
		double sumA = 0;
		long sumB = 0;
		// int n = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			documentSplitter.setDocument(doc);
			int splits = documentSplitter.getSplits();
			for (int i = 0; i < splits; i++) {
				Split<T> nextSplit = documentSplitter.nextSplit();
				Document<T> rd = nextSplit.getRefDoc();
				Document<T> d = nextSplit.getTestDoc();
				double b = d.getSize();
				if (b != 0 && (rd == null || rd.getSize() != 0)) {
					double a = computeLogProbability(rd, d, topicModeler);
					// System.out.println(n++ + ", " + a + ", " + b );
					sumA += a; // computeLogProbability(rd, d, sampler);
					sumB += b; // d.getSize();
				}
			}
		}
		return Math.exp(-sumA / sumB);
	}

	protected double computeLogProbability(Document<T> refD, Document<T> d,
			AbstractTopicModeler<T> sampler) {
		// update ptd via reference document
		initPtd(refD, sampler);

		double sValuesAvg = 0;

		for (int i = 0; i < sValues.length; i++) {
			double sValue = 0;
			// update ptd via reference document
			updatePtd(refD, sampler);

			// Loop over d
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					sValue += wordFr
							* computeWordLogProbability(index, refD, sampler);
				}
			}

			sValues[i] = sValue;
			sValuesAvg += sValue;
		}

		double res;
		if (sValues.length > 1) {
			// Compute the average of sValue in the "non-log space" without
			// numeric problems by
			// factoring out a common prefactor of all sValues:
			sValuesAvg /= sValues.length;
			for (int i = 0; i < sValues.length; i++) {
				sValues[i] -= sValuesAvg;
			}
			double nonLogAvg = 0;
			for (int i = 0; i < sValues.length; i++) {
				nonLogAvg += Math.exp(sValues[i]);
			}
			nonLogAvg /= sValues.length;

			res = sValuesAvg + Math.log(nonLogAvg);
		} else {
			res = sValuesAvg;
		}

		if (bowFactor) {
			// Loop over d
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					res -= logFacN(wordFr);
				}
			}
			res += logFacN(d.getSize());
		}
		return res;
	}

	protected void initPtd(Document<T> d, AbstractTopicModeler<T> sampler) {
	}

	protected void updatePtd(Document<T> d, AbstractTopicModeler<T> sampler) {
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = sampler.getTopicProb(i);
		}
	}

	protected double computeWordLogProbability(int sIndex, Document<T> refD,
			AbstractTopicModeler<T> sampler) {
		double sum = 0;
		for (int i = 0; i < sampler.getNTopics(); i++) {
			sum += sampler.getPhi(i, sIndex) * ptd[i];
		}

		return Math.log(sum);
	}

	public static double logFacN(int n) {
		double sum = 0;
		for (int i = 1; i <= n; i++) {
			sum += Math.log(i);
		}
		return sum;
	}

	public void optimizeAlphaConc(double minLambda, double maxLambda,
			final DocumentProvider<T> provider, int steps) {
		PeakValueOptimizer peakValueOptimizer = new PeakValueOptimizer() {
			public double test(double value) {
				topicModeler.setAlphaConc(value);
				return -computePerplexity(provider);
			}
		};
		peakValueOptimizer.optimizeLambda(minLambda, maxLambda, steps);
	}
}
