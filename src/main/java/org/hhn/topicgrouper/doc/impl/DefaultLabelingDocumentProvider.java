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

public class DefaultLabelingDocumentProvider<T, L> extends
		DefaultDocumentProvider<T> implements LabelingDocumentProvider<T, L> {
	protected final Map<L, List<LabeledDocument<T, L>>> labelsToDocuments;
	protected final Map<L, List<LabeledDocument<T, L>>> labelsToDocumentsImmutable;

	
	public DefaultLabelingDocumentProvider() {
		this(new DefaultVocab<T>());
	}
	
	public DefaultLabelingDocumentProvider(DefaultVocab<T> vocab) {
		super(vocab);
		labelsToDocuments = new HashMap<L, List<LabeledDocument<T, L>>>();
		labelsToDocumentsImmutable = new HashMap<L, List<LabeledDocument<T, L>>>();
	}

	@Override
	public Collection<L> getAllLabels() {
		return labelsToDocuments.keySet();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<LabeledDocument<T, L>> getLabeledDocuments() {
		return (List<LabeledDocument<T, L>>) (List<?>) getDocuments();
	}

	@Override
	public List<LabeledDocument<T, L>> getDocumentsWithLabel(L label) {
		return labelsToDocumentsImmutable.get(label);
	}

	@Override
	public DefaultLabeledDocument newDocument() {
		return newLabeledDocument(null);
	}

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

	@SuppressWarnings("unchecked")
	@Override
	public void removeDocument(DefaultDocument d) {
		removeLabeledDocument((DefaultLabeledDocument) d);
	}

	public void removeLabeledDocument(DefaultLabeledDocument d) {
		super.removeDocument(d);
		List<LabeledDocument<T, L>> docs = labelsToDocuments.get(d.getLabel());
		if (docs != null) {
			docs.remove(d);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public DefaultLabeledDocument addDocument(Document<T> d) {
		return addLabeledDocument((LabeledDocument<T, L>) d);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DefaultLabeledDocument addDocument(Document<T> d, int minFrequency) {
		return addLabeledDocument((LabeledDocument<T, L>) d, minFrequency);
	}

	@SuppressWarnings("unchecked")
	@Override
	public DefaultLabeledDocument addDocument(Document<T> d, int minFrequency,
			DocumentWordFilter<T> filter) {
		return addLabeledDocument((LabeledDocument<T, L>) d, minFrequency,
				filter);
	}

	public DefaultLabeledDocument addLabeledDocument(LabeledDocument<T, L> d,
			int minFrequency) {
		return addLabeledDocument(d, minFrequency, null);
	}

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

	public DefaultLabeledDocument addLabeledDocument(LabeledDocument<T, L> d) {
		return addLabeledDocument(d, 0);
	}
	
	public class DefaultLabeledDocument extends DefaultDocument implements
			LabeledDocument<T, L> {
		private static final long serialVersionUID = -378851753041871327L;
		private final L label;

		public DefaultLabeledDocument(L label, int index) {
			super(index);
			this.label = label;
		}

		@Override
		public L getLabel() {
			return label;
		}

		@Override
		public LabelingDocumentProvider<T, L> getProvider() {
			return DefaultLabelingDocumentProvider.this;
		}
	}
}
