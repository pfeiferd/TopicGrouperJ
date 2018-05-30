package org.hhn.topicgrouper.doc.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * The vocabulary of the hold out is guaranteed to be a subset of the rest
 * (training).
 */
public class HoldOutSplitter<T> {
	private final DefaultDocumentProvider<T> holdOut;
	private final DefaultDocumentProvider<T> rest;

	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			double holdOutRatio, int minWordFrequency) {
		this(random, origDocuments,
				(int) (origDocuments.getDocuments().size() * Math.min(1,
						holdOutRatio)), minWordFrequency, true, null);
	}

	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			double holdOutRatio, int minWordFrequency, boolean minFrInRest,
			DefaultVocab<T> vocab) {
		this(random, origDocuments,
				(int) (origDocuments.getDocuments().size() * Math.min(1,
						holdOutRatio)), minWordFrequency, minFrInRest, vocab);
	}

	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			int holdOutNumber, int minWordFrequency, boolean minFrInRest,
			DefaultVocab<T> vocab) {
		List<Document<T>> documents = new ArrayList<Document<T>>(
				origDocuments.getDocuments());
		List<Document<T>> holdOutDocuments = new ArrayList<Document<T>>();
		for (int i = 0; i < holdOutNumber; i++) {
			holdOutDocuments.add(documents.remove(random.nextInt(documents
					.size())));
		}
		// This is ugly cause it takes a lot of resources, but for now ok:
		// It helps to ensure that only words with a minimum frequency IN THE
		// TRAINING DATA are kept.
		if (minFrInRest) {
			DefaultDocumentProvider<T> restHelper = new DefaultDocumentProvider<T>();
			for (Document<T> d : documents) {
				restHelper.addDocument(d, 1);
			}
			documents = restHelper.getDocuments();
		}
		rest = new DefaultDocumentProvider<T>();
		for (Document<T> d : documents) {
			rest.addDocument(d, minWordFrequency);
		}
		DefaultDocumentProvider.DocumentWordFilter<T> filter = new DefaultDocumentProvider.DocumentWordFilter<T>() {
			@Override
			public boolean acceptWord(T word) {
				return rest.getVocab().getIndex(word) != -1;
			}
		};
		if (vocab == null) {
			vocab = (DefaultVocab<T>) rest.getVocab();
		}
		holdOut = new DefaultDocumentProvider<T>(vocab);
		for (Document<T> d : holdOutDocuments) {
			holdOut.addDocument(d, 0, filter);
		}
	}

	public DefaultDocumentProvider<T> getHoldOut() {
		return holdOut;
	}

	public DefaultDocumentProvider<T> getRest() {
		return rest;
	}
}
