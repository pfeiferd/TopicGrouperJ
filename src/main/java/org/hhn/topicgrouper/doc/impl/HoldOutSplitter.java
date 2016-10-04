package org.hhn.topicgrouper.doc.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * The vocabulary of the hold out is guaranteed to be a subset of the rest (training).
 */
public class HoldOutSplitter<T> {
	private DefaultDocumentProvider<T> holdOut;
	private DefaultDocumentProvider<T> rest;

	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			double holdOutRatio, int minGlobalWordFrequency) {
		this(random, origDocuments,
				(int) (origDocuments.getDocuments().size() * Math.min(1,
						holdOutRatio)), minGlobalWordFrequency);
	}

	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			int holdOutNumber, int minGlobalWordFrequency) {
		List<Document<T>> documents = new ArrayList<Document<T>>(
				origDocuments.getDocuments());
		List<Document<T>> holdOutDocuments = new ArrayList<Document<T>>();
		for (int i = 0; i < holdOutNumber; i++) {
			holdOutDocuments.add(documents.remove(random.nextInt(documents
					.size())));
		}
		rest = new DefaultDocumentProvider<T>();
		for (Document<T> d : documents) {
			rest.addDocument(d, minGlobalWordFrequency);
		}
		DefaultDocumentProvider.DocumentWordFilter<T> filter = new DefaultDocumentProvider.DocumentWordFilter<T>() {
			@Override
			public boolean acceptWord(T word) {
				return rest.getIndex(word) != -1;
			}
		};
		holdOut = new DefaultDocumentProvider<T>();
		for (Document<T> d : holdOutDocuments) {
			holdOut.addDocument(d, minGlobalWordFrequency, filter);
		}
	}

	public DefaultDocumentProvider<T> getHoldOut() {
		return holdOut;
	}

	public DefaultDocumentProvider<T> getRest() {
		return rest;
	}
}
