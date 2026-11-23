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
	/**
	 * All distinct labels of the documents of this provider.
	 *
	 * @return the labels
	 */
	public Collection<L> getAllLabels();
	/**
	 * All documents of this provider with their labels.
	 *
	 * @return the labeled documents, in the order of {@link #getDocuments()}
	 */
	public List<LabeledDocument<T,L>> getLabeledDocuments();
	/**
	 * The documents carrying a given label.
	 *
	 * @param label the label
	 * @return the documents with the label, empty if there are none
	 */
	public List<LabeledDocument<T,L>> getDocumentsWithLabel(L label);
}
