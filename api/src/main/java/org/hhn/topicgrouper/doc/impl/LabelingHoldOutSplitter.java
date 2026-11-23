package org.hhn.topicgrouper.doc.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * Splits a labeled document collection into a hold out (test) and a training part ("rest"),
 * keeping the hold out ratio for every label, e.g. for evaluating classifiers.
 * The vocabulary of the hold out is guaranteed to be a subset of the rest
 * (training): words of hold out documents that do not occur in the training part are dropped.
 *
 * @param <T> the type of the words
 * @param <L> the type of the labels
 */
public class LabelingHoldOutSplitter<T, L> {
	private DefaultLabelingDocumentProvider<T, L> holdOut;
	private DefaultLabelingDocumentProvider<T, L> rest;

	/**
	 * Splits labeled documents into hold out and training part per label, using the most frequent labels and a new vocabulary.
	 *
	 * @param random source of randomness
	 * @param origDocuments the documents to split
	 * @param holdOutRatio fraction of the documents of each label moved to the hold out set
	 * @param minGlobalWordFrequency words with a lower global frequency are dropped from the training set
	 * @param maxLabels number of most frequent labels to keep, 0 or less = all labels
	 */
	public LabelingHoldOutSplitter(Random random,
			LabelingDocumentProvider<T, L> origDocuments, double holdOutRatio,
			int minGlobalWordFrequency, int maxLabels) {
		this(random, origDocuments, holdOutRatio, minGlobalWordFrequency,
				maxLabels, new DefaultVocab<T>());
	}

	/**
	 * Splits labeled documents into hold out and training part per label, using the most frequent labels.
	 *
	 * @param random source of randomness
	 * @param origDocuments the documents to split
	 * @param holdOutRatio fraction of the documents of each label moved to the hold out set
	 * @param minGlobalWordFrequency words with a lower global frequency are dropped from the training set
	 * @param maxLabels number of most frequent labels to keep, 0 or less = all labels
	 * @param vocab vocabulary for the training set, <code>null</code> for a new one
	 */
	public LabelingHoldOutSplitter(Random random,
			LabelingDocumentProvider<T, L> origDocuments, double holdOutRatio,
			int minGlobalWordFrequency, int maxLabels, DefaultVocab<T> vocab) {
		this(random, origDocuments, holdOutRatio, minGlobalWordFrequency,
				selectMostFrequentLabels(origDocuments, maxLabels), vocab);
	}

	/**
	 * Selects the labels with the most documents.
	 *
	 * @param <T> the type of the words
	 * @param <L> the type of the labels
	 * @param origDocuments the documents
	 * @param maxLabels number of labels to select; 0 or less selects all labels
	 * @return the selected labels
	 */
	protected static <T, L> Collection<L> selectMostFrequentLabels(
			LabelingDocumentProvider<T, L> origDocuments, int maxLabels) {
		if (maxLabels > 0) {
			Collection<L> labels = new ArrayList<L>(
					origDocuments.getAllLabels());
			Collection<L> selectedLabels = new ArrayList<L>();

			// Get the most frequent labels.
			for (int i = 0; i < maxLabels && !labels.isEmpty(); i++) {
				L bestLabel = null;
				int maxDocs = 0;
				for (L label : labels) {
					int size = origDocuments.getDocumentsWithLabel(label)
							.size();
					if (origDocuments.getDocumentsWithLabel(label).size() > maxDocs) {
						maxDocs = size;
						bestLabel = label;
					}
				}
				labels.remove(bestLabel);
				selectedLabels.add(bestLabel);
			}
			return selectedLabels;
		} else {
			return origDocuments.getAllLabels();
		}
	}

	/**
	 * Splits labeled documents into hold out and training part per label, so that the ratio is kept for every label.
	 *
	 * @param random source of randomness
	 * @param origDocuments the documents to split
	 * @param holdOutRatio fraction of the documents of each label moved to the hold out set
	 * @param minGlobalWordFrequency words with a lower global frequency are dropped from the training set
	 * @param selectedLabels only documents with these labels are used
	 * @param vocab vocabulary for the training set, <code>null</code> for a new one
	 */
	public LabelingHoldOutSplitter(Random random,
			LabelingDocumentProvider<T, L> origDocuments, double holdOutRatio,
			int minGlobalWordFrequency, Collection<L> selectedLabels,
			DefaultVocab<T> vocab) {

		rest = new DefaultLabelingDocumentProvider<T, L>(vocab);
		if (vocab == null) {
			vocab = (DefaultVocab<T>) rest.getVocab();
		}
		holdOut = new DefaultLabelingDocumentProvider<T, L>(vocab);
		DefaultDocumentProvider.DocumentWordFilter<T> filter = new DefaultDocumentProvider.DocumentWordFilter<T>() {
			@Override
			public boolean acceptWord(T word) {
				return rest.getVocab().getIndex(word) != -1;
			}
		};
		List<LabeledDocument<T, L>> allHoldOutDocuments = new ArrayList<LabeledDocument<T, L>>();
		for (L label : selectedLabels) {
			// Keep holdout / rest ratio for every label.
			List<LabeledDocument<T, L>> documents = new ArrayList<LabeledDocument<T, L>>(
					origDocuments.getDocumentsWithLabel(label));
			int holdOutNumber = (int) (documents.size() * holdOutRatio);
			for (int i = 0; i < holdOutNumber; i++) {
				allHoldOutDocuments.add(documents.remove(random.nextInt(documents
						.size())));
			}
			for (LabeledDocument<T, L> d : documents) {
				rest.addLabeledDocument(d, minGlobalWordFrequency);
			}
		}
		// The hold out documents are filtered by the training vocabulary, so
		// they are added only once the training documents of all labels are in.
		for (LabeledDocument<T, L> d : allHoldOutDocuments) {
			holdOut.addLabeledDocument(d, 0, filter);
		}
	}

	/**
	 * The hold out (test) part of the split.
	 *
	 * @return the hold out documents
	 */
	public DefaultLabelingDocumentProvider<T, L> getHoldOut() {
		return holdOut;
	}

	/**
	 * The training part of the split.
	 *
	 * @return the remaining (training) documents
	 */
	public DefaultLabelingDocumentProvider<T, L> getRest() {
		return rest;
	}
}
