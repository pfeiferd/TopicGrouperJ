package org.hhn.topicgrouper.classify.impl.vocab;

import gnu.trove.map.TObjectIntMap;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public class VocabDFNBClassifier<T, L> extends
		VocabIGNBClassifier<T, L> {
	
	public VocabDFNBClassifier(double lambda,
			LabelingDocumentProvider<T, L> documentProvider, int keepWords) {
		super(lambda, documentProvider, keepWords);
	}

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
