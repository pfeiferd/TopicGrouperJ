package org.hhn.topicgrouper.tgimpl;

import gnu.trove.iterator.TIntIterator;

import java.util.List;

import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solver;

public abstract class AbstractTopicGrouper<T> implements Solver<T> {
	protected final int minWordFrequency;
	protected final DocumentProvider<T> documentProvider;
	protected final int[] documentSizes;
	protected final double[] logDocumentSizes;
	protected final List<Document<T>> documents;

	public AbstractTopicGrouper(int minWordFrequency, DocumentProvider<T> documentProvider) {
		this.minWordFrequency = minWordFrequency;
		this.documentProvider = documentProvider;
		this.documents = documentProvider.getDocuments();
		this.documentSizes = getDocumentSizes();
		this.logDocumentSizes = getLogDocumentSizes();
	}

	private int[] getDocumentSizes() {
		int[] documentSizes = new int[documents.size()];
		for (int i = 0; i < documents.size(); i++) {
			int sum = 0;
			TIntIterator indices = documents.get(i).getWordIndices().iterator();
			while (indices.hasNext()) {
				int index = indices.next();
				if (documentProvider.getWordFrequency(index) >= minWordFrequency) {
					sum += documents.get(i).getWordFrequency(index);
				}
			}
			documentSizes[i] = sum;
		}
		return documentSizes;
	}

	private double[] getLogDocumentSizes() {
		double[] logDocumentSizes = new double[documentSizes.length];

		for (int i = 0; i < documentSizes.length; i++) {
			if (documentSizes[i] > 0) {
				logDocumentSizes[i] = Math.log(documentSizes[i]);
			}
		}

		return logDocumentSizes;
	}
}
