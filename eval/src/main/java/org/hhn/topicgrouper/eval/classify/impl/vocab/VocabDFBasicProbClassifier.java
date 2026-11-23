package org.hhn.topicgrouper.eval.classify.impl.vocab;

import gnu.trove.map.TObjectIntMap;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * Variant of {@link VocabIGBasicProbClassifier} that selects the words with the highest document frequency.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public class VocabDFBasicProbClassifier<T, L> extends
		VocabIGBasicProbClassifier<T, L> {
	
	/**
	 * Creates the classifier and selects the features.
	 *
	 * @param lambda smoothing weight
	 * @param documentProvider labeled documents used to rank the words
	 * @param keepWords maximum number of words to keep
	 */
	public VocabDFBasicProbClassifier(double lambda,
			LabelingDocumentProvider<T, L> documentProvider, int keepWords) {
		super(lambda, documentProvider, keepWords);
	}

	/**
	 * Scores a word by the inverse of its document frequency (smaller is better); label maps are unused.
	 *
	 * @return 1 / document frequency (infinity if the word occurs in no document)
	 */
	protected double computeScore(int wordIndex,
			LabelingDocumentProvider<T, L> documentProvider,
			TObjectIntMap<L> posMap, TObjectIntMap<L> negMap) {
		int df = 0;
		for (Document<T> d: documentProvider.getDocuments()) {
			if (d.getWordFrequency(wordIndex) > 0) {
				df++;
			}
		}
		return 1d / df;
	}
}
