package org.hhn.topicgrouper.doc;

import java.util.Collection;
import java.util.List;

/**
 * A {@link DocumentProvider} whose documents carry labels (for classification).
 *
 * @param <T> the type of the words
 * @param <L> the type of the labels
 */
public interface LabelingDocumentProvider<T,L> extends DocumentProvider<T> {
	public Collection<L> getAllLabels();
	public List<LabeledDocument<T,L>> getLabeledDocuments();
	public List<LabeledDocument<T,L>> getDocumentsWithLabel(L label);
}
