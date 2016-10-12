package org.hhn.topicgrouper.lda.validation;

import gnu.trove.iterator.TIntIterator;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public class AbstractLDAPerplexityCalculator<T> {
	protected final boolean bowFactor;
	protected final DocumentSplitter<T> documentSplitter;
	protected final double[] sValues;
	protected double[] ptd;

	public AbstractLDAPerplexityCalculator(boolean bowFactor) {
		this(bowFactor, new DefaultDocumentSplitter<T>(), 1);
	}

	public AbstractLDAPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter, int samplingMax) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
		sValues = new double[samplingMax];
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider,
			LDAGibbsSampler<T> sampler) {
		if (ptd == null || ptd.length != sampler.getNTopics()) {
			ptd = new double[sampler.getNTopics()];
		}
		double sumA = 0;
		long sumB = 0;
		int n = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			documentSplitter.setDocument(doc);
			int splits = documentSplitter.getSplits();
			for (int i = 0; i < splits; i++) {
				Split<T> nextSplit = documentSplitter.nextSplit();
				Document<T> rd = nextSplit.getRefDoc();
				Document<T> d = nextSplit.getTestDoc();
				double a = computeLogProbability(rd, d, sampler);
				double b = d.getSize();
//				System.out.println(n++ + ", " + a + ", " + b );
				sumA += computeLogProbability(rd, d, sampler);
				sumB += d.getSize();
			}
		}
		return Math.exp(-sumA / sumB);
	}

	protected double computeLogProbability(Document<T> refD, Document<T> d,
			LDAGibbsSampler<T> sampler) {
		DocumentProvider<T> provider = sampler.getDocumentProvider();
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
				T word = d.getProvider().getWord(index);
				int sIndex = provider.getIndex(word);
				// Ensure the word is in the training vocabulary.
				if (sIndex >= 0) {
					int wordFr = d.getWordFrequency(index);
					if (wordFr > 0) {
						sValue += wordFr
								* computeWordLogProbability(sIndex, refD,
										sampler);
					}
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
		}
		else {
			res = sValuesAvg;
		}

		if (bowFactor) {
			// Loop over d
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				T word = d.getProvider().getWord(index);
				int sIndex = provider.getIndex(word);
				// Ensure the word is in the training vocabulary.
				if (sIndex >= 0) {
					int wordFr = d.getWordFrequency(index);
					if (wordFr > 0) {
						res -= TGPerplexityCalculator.logFacN(wordFr);
					}
				}
			}
			res += TGPerplexityCalculator.logFacN(d.getSize());
		}
		return res;
	}

	protected void initPtd(Document<T> d, LDAGibbsSampler<T> sampler) {
	}

	protected void updatePtd(Document<T> d, LDAGibbsSampler<T> sampler) {
		double sum = 0;
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] = sampler.getTopicFrCount(i);
			sum += ptd[i];
		}
		for (int i = 0; i < ptd.length; i++) {
			ptd[i] /= sum;
		}
	}

	protected double computeWordLogProbability(int sIndex, Document<T> refD,
			LDAGibbsSampler<T> sampler) {
		double sum = 0;
		for (int i = 0; i < sampler.getNTopics(); i++) {
			if (sampler.getTopicFrCount(i) > 0) { // To avoid division by zero.
													// Also correct: If a topic
													// has zero probability
													// (zero frequency), it
													// cannot be allocated to
													// produce a word.
				sum += ((double) sampler.getTopicWordAssignmentCount(i, sIndex) + sampler
						.getBeta(i, sIndex))
						/ (sampler.getTopicFrCount(i) + sampler.getBetaSum(i))
						* ptd[i];
			}
		}
		return Math.log(sum);
	}
}
