package org.hhn.topicgrouper.doc.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * The vocabulary of the hold out is guaranteed to be a subset of the rest
 * (training).
 */
public class LabelingHoldOutSplitter<T, L> {
	private DefaultLabelingDocumentProvider<T, L> holdOut;
	private DefaultLabelingDocumentProvider<T, L> rest;

	public LabelingHoldOutSplitter(Random random,
			LabelingDocumentProvider<T, L> origDocuments, double holdOutRatio,
			int minGlobalWordFrequency, int maxLabels) {
		this(random, origDocuments, holdOutRatio, minGlobalWordFrequency,
				maxLabels, new DefaultVocab<T>());
	}

	public LabelingHoldOutSplitter(Random random,
			LabelingDocumentProvider<T, L> origDocuments, double holdOutRatio,
			int minGlobalWordFrequency, int maxLabels, DefaultVocab<T> vocab) {
		this(random, origDocuments, holdOutRatio, minGlobalWordFrequency,
				selectMostFrequentLabels(origDocuments, maxLabels), vocab);
	}

	protected static <T, L> Collection<L> selectMostFrequentLabels(
			LabelingDocumentProvider<T, L> origDocuments, int maxLabels) {
		if (maxLabels > 0) {
			Collection<L> labels = new ArrayList<L>(
					origDocuments.getAllLabels());
			Collection<L> selectedLabels = new ArrayList<L>();

			// Get the most frequent labels.
			for (int i = 0; i < maxLabels || labels.isEmpty(); i++) {
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
		for (L label : selectedLabels) {
			// Keep holdout / rest ratio for every label.
			List<LabeledDocument<T, L>> documents = new ArrayList<LabeledDocument<T, L>>(
					origDocuments.getDocumentsWithLabel(label));
			int holdOutNumber = (int) (documents.size() * holdOutRatio);
			List<LabeledDocument<T, L>> holdOutDocuments = new ArrayList<LabeledDocument<T, L>>();
			for (int i = 0; i < holdOutNumber; i++) {
				holdOutDocuments.add(documents.remove(random.nextInt(documents
						.size())));
			}
			for (LabeledDocument<T, L> d : documents) {
				rest.addLabeledDocument(d, minGlobalWordFrequency);
			}
			for (LabeledDocument<T, L> d : holdOutDocuments) {
				holdOut.addLabeledDocument(d, 0, filter);
			}
		}
	}

	public DefaultLabelingDocumentProvider<T, L> getHoldOut() {
		return holdOut;
	}

	public DefaultLabelingDocumentProvider<T, L> getRest() {
		return rest;
	}
}
