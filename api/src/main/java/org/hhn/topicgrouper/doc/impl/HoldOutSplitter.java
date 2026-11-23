package org.hhn.topicgrouper.doc.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * Splits a document collection randomly into a hold out and a training part ("rest"),
 * e.g. for computing the perplexity on unseen documents.
 * The vocabulary of the hold out is guaranteed to be a subset of the rest
 * (training): words of hold out documents that do not occur in the training part are dropped.
 *
 * @param <T> the type of the words
 */
public class HoldOutSplitter<T> {
	private final DefaultDocumentProvider<T> holdOut;
	private final DefaultDocumentProvider<T> rest;

	/**
	 * Splits documents randomly into hold out and training part, with frequencies counted on the training part and a new vocabulary.
	 *
	 * @param random source of randomness
	 * @param origDocuments the documents to split
	 * @param holdOutRatio fraction of documents (at most 1) moved to the hold out set
	 * @param minWordFrequency words with lower frequency are removed from the training set
	 */
	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			double holdOutRatio, int minWordFrequency) {
		this(random, origDocuments,
				(int) (origDocuments.getDocuments().size() * Math.min(1,
						holdOutRatio)), minWordFrequency, true, null);
	}

	/**
	 * Splits documents randomly into hold out and training part.
	 *
	 * @param random source of randomness
	 * @param origDocuments the documents to split
	 * @param holdOutRatio fraction of documents (at most 1) moved to the hold out set
	 * @param minWordFrequency words with lower frequency are removed from the training set
	 * @param minFrInRest whether word frequencies are determined on the training part only
	 * @param vocab vocabulary for the hold out set, <code>null</code> to use the one of the training set
	 */
	public HoldOutSplitter(Random random, DocumentProvider<T> origDocuments,
			double holdOutRatio, int minWordFrequency, boolean minFrInRest,
			DefaultVocab<T> vocab) {
		this(random, origDocuments,
				(int) (origDocuments.getDocuments().size() * Math.min(1,
						holdOutRatio)), minWordFrequency, minFrInRest, vocab);
	}

	/**
	 * Splits documents randomly into hold out and training part with a fixed number of hold out documents.
	 *
	 * @param random source of randomness
	 * @param origDocuments the documents to split
	 * @param holdOutNumber number of randomly chosen documents moved to the hold out set
	 * @param minWordFrequency words with lower frequency are removed from the training set
	 * @param minFrInRest whether word frequencies are determined on the training part only
	 * @param vocab vocabulary for the hold out set, <code>null</code> to use the one of the training set
	 */
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

	/**
	 * The hold out part of the split.
	 *
	 * @return the hold out documents
	 */
	public DefaultDocumentProvider<T> getHoldOut() {
		return holdOut;
	}

	/**
	 * The training part of the split.
	 *
	 * @return the remaining (training) documents
	 */
	public DefaultDocumentProvider<T> getRest() {
		return rest;
	}
}
