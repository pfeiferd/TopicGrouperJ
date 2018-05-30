package org.hhn.topicgrouper.doc;

import java.util.Collection;
import java.util.List;

public interface LabelingDocumentProvider<T,L> extends DocumentProvider<T> {
	public Collection<L> getAllLabels();
	public List<LabeledDocument<T,L>> getLabeledDocuments();
	public List<LabeledDocument<T,L>> getDocumentsWithLabel(L label);
}
