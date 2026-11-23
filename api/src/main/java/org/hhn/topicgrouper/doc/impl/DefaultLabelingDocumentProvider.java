package org.hhn.topicgrouper.doc.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * A {@link DefaultDocumentProvider} whose documents carry a label and can be retrieved by label.
 *
 * @param <T> the word type
 * @param <L> the label type
 */
public class DefaultLabelingDocumentProvider<T, L> extends
		DefaultDocumentProvider<T> implements LabelingDocumentProvider<T, L> {

	private static final long serialVersionUID = -3504489751831118587L;

	/**
	 * Modifiable lists of documents per label.
	 */
	protected final Map<L, List<LabeledDocument<T, L>>> labelsToDocuments;
	/**
	 * Unmodifiable views of the lists in {@link #labelsToDocuments}, handed out to clients.
	 */
	protected final Map<L, List<LabeledDocument<T, L>>> labelsToDocumentsImmutable;

	
	/**
	 * Creates a provider with an empty vocabulary.
	 */
	public DefaultLabelingDocumentProvider() {
		this(new DefaultVocab<T>());
	}
	
	/**
	 * Creates an empty provider using the given vocabulary.
	 *
	 * @param vocab the vocabulary
	 */
	public DefaultLabelingDocumentProvider(DefaultVocab<T> vocab) {
		super(vocab);
		labelsToDocuments = new HashMap<L, List<LabeledDocument<T, L>>>();
		labelsToDocumentsImmutable = new HashMap<L, List<LabeledDocument<T, L>>>();
	}

	/**
	 * @return the labels of all documents added so far (live view)
	 */
	@Override
	public Collection<L> getAllLabels() {
		return labelsToDocuments.keySet();
	}

	/**
	 * @return all documents (unmodifiable), typed as labeled documents
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LabeledDocument<T, L>> getLabeledDocuments() {
		return (List<LabeledDocument<T, L>>) (List<?>) getDocuments();
	}

	/**
	 * @param label the label
	 * @return the unmodifiable list of documents with that label, <code>null</code> if the label is unknown
	 */
	@Override
	public List<LabeledDocument<T, L>> getDocumentsWithLabel(L label) {
		return labelsToDocumentsImmutable.get(label);
	}

	/**
	 * Creates an empty document without label (<code>null</code> label).
	 */
	@Override
	public DefaultLabeledDocument newDocument() {
		return newLabeledDocument(null);
	}

	/**
	 * Creates an empty document with a label that is part of this provider.
	 *
	 * @param label the label
	 * @return the new document
	 */
	public DefaultLabeledDocument newLabeledDocument(L label) {
		DefaultLabeledDocument document = new DefaultLabeledDocument(label, entries.size());
		entries.add(document);
		List<LabeledDocument<T, L>> docs = labelsToDocuments.get(label);
		if (docs == null) {
			docs = new ArrayList<LabeledDocument<T, L>>();
			labelsToDocuments.put(label, docs);
			labelsToDocumentsImmutable.put(label,
					Collections.unmodifiableList(docs));
		}
		docs.add(document);

		return document;
	}

	/**
	 * Removes a document, which must be a {@link DefaultLabeledDocument}.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void removeDocument(DefaultDocument d) {
		removeLabeledDocument((DefaultLabeledDocument) d);
	}

	/**
	 * Removes a document from this provider and from its label's list.
	 *
	 * @param d the document to remove
	 */
	public void removeLabeledDocument(DefaultLabeledDocument d) {
		super.removeDocument(d);
		List<LabeledDocument<T, L>> docs = labelsToDocuments.get(d.getLabel());
		if (docs != null) {
			docs.remove(d);
		}
	}

	/**
	 * Copies a labeled document (the argument must be a {@link LabeledDocument}), keeping all words.
	 *
	 * @return the new document or <code>null</code> if no words were left
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DefaultLabeledDocument addDocument(Document<T> d) {
		return addLabeledDocument((LabeledDocument<T, L>) d);
	}

	/**
	 * Copies a labeled document (the argument must be a {@link LabeledDocument}).
	 *
	 * @return the new document or <code>null</code> if no words were left
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DefaultLabeledDocument addDocument(Document<T> d, int minFrequency) {
		return addLabeledDocument((LabeledDocument<T, L>) d, minFrequency);
	}

	/**
	 * Copies a labeled document (the argument must be a {@link LabeledDocument}).
	 *
	 * @return the new document or <code>null</code> if no words were left
	 */
	@SuppressWarnings("unchecked")
	@Override
	public DefaultLabeledDocument addDocument(Document<T> d, int minFrequency,
			DocumentWordFilter<T> filter) {
		return addLabeledDocument((LabeledDocument<T, L>) d, minFrequency,
				filter);
	}

	/**
	 * Copies a labeled document including its label.
	 *
	 * @param d the document to copy
	 * @param minFrequency words with a lower global frequency in the source provider are dropped (0 = keep all)
	 * @return the new document or <code>null</code> if no words were left
	 */
	public DefaultLabeledDocument addLabeledDocument(LabeledDocument<T, L> d,
			int minFrequency) {
		return addLabeledDocument(d, minFrequency, null);
	}

	/**
	 * Copies a labeled document including its label.
	 *
	 * @param d the document to copy
	 * @param minFrequency words with a lower global frequency in the source provider are dropped (0 = keep all)
	 * @param filter optional word filter
	 * @return the new document or <code>null</code> if no words were left (then nothing is added)
	 */
	public DefaultLabeledDocument addLabeledDocument(LabeledDocument<T, L> d,
			int minFrequency, DocumentWordFilter<T> filter) {
		DefaultLabeledDocument r = newLabeledDocument(d.getLabel());
		copyWords(d, r, minFrequency, filter);
		if (r.getSize() == 0) {
			removeLabeledDocument(r);
			return null;
		}
		else {
			return r;
		}
	}

	/**
	 * Copies a labeled document including its label, keeping all words.
	 *
	 * @param d the document to copy
	 * @return the new document or <code>null</code> if it has no words
	 */
	public DefaultLabeledDocument addLabeledDocument(LabeledDocument<T, L> d) {
		return addLabeledDocument(d, 0);
	}
	
	/**
	 * A document of this provider that has a label.
	 */
	public class DefaultLabeledDocument extends DefaultDocument implements
			LabeledDocument<T, L> {
		private static final long serialVersionUID = -378851753041871327L;
		/**
		 * The class label of the document.
		 */
		private final L label;

		/**
		 * Creates an empty labeled document.
		 *
		 * @param label the label
		 * @param index the index of the document in the provider
		 */
		public DefaultLabeledDocument(L label, int index) {
			super(index);
			this.label = label;
		}

		/**
		 * @return the label
		 */
		@Override
		public L getLabel() {
			return label;
		}

		/**
		 * @return the enclosing provider
		 */
		@Override
		public LabelingDocumentProvider<T, L> getProvider() {
			return DefaultLabelingDocumentProvider.this;
		}
	}
}
