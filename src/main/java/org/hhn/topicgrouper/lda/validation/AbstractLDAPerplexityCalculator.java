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
	protected double[] ptd;
	private final DocumentSplitter<T> documentSplitter;

	private Split<T> nextSplit;

	public AbstractLDAPerplexityCalculator(boolean bowFactor) {
		this(bowFactor, new DefaultDocumentSplitter<T>());
	}
	
	public AbstractLDAPerplexityCalculator(boolean bowFactor,
			DocumentSplitter<T> documentSplitter) {
		this.bowFactor = bowFactor;
		this.documentSplitter = documentSplitter;
	}

	public double computePerplexity(DocumentProvider<T> testDocumentProvider,
			LDAGibbsSampler<T> sampler) {
		if (ptd == null || ptd.length != sampler.getNTopics()) {
			ptd = new double[sampler.getNTopics()];
		}
		double sumA = 0;
		long sumB = 0;

		for (Document<T> doc : testDocumentProvider.getDocuments()) {
			documentSplitter.setDocument(doc);
			int splits = documentSplitter.getSplits();
			for (int i = 0; i < splits; i++) {
				nextSplit = documentSplitter.nextSplit();
				Document<T> rd = nextSplit.getRefDoc();
				Document<T> d = nextSplit.getTestDoc();
				sumA += computeLogProbability(rd, d, sampler);
				sumB += d.getSize();
			}
		}
		return Math.exp(-sumA / sumB);
	}


	protected double computeLogProbability(Document<T> refD, Document<T> d,
			LDAGibbsSampler<T> sampler) {
		DocumentProvider<T> provider = sampler.getDocumentProvider();
		double res = bowFactor ? TGPerplexityCalculator.logFacN(d.getSize())
				: 0;

		// update ptd via reference document
		updatePtd(refD, sampler);

		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getWord(index);
			int sIndex = provider.getIndex(word);
			// Ensure the word is in the training vocabulary.
			if (sIndex >= 0) {
				int wordFr = d.getWordFrequency(index);
				if (wordFr > 0) {
					if (bowFactor) {
						res -= TGPerplexityCalculator.logFacN(wordFr);
					}
					res += wordFr
							* computeWordLogProbability(sIndex, refD, sampler);
				}
			}
		}
		return res;
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
		for (int i = 0; i < ptd.length; i++) {
			if (sampler.getTopicFrCount(i) > 0) { // To avoid division by zero.
													// Also correct: If a topic
													// has zero probability
													// (zero frequency), it
													// cannot be allocated to
													// produce a word.
				sum += ((double) sampler.getTopicWordAssignmentCount(i, sIndex))
						/ sampler.getTopicFrCount(i) * ptd[i];
			}
		}
		return Math.log(sum);
	}
}
