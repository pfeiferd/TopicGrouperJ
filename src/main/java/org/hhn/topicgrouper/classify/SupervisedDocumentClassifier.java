package org.hhn.topicgrouper.classify;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

/**
 * A classifier that is trained on labeled documents and then predicts labels for unseen documents.
 *
 * @param <T> the word type of the documents
 * @param <L> the label type
 */
public interface SupervisedDocumentClassifier<T, L> {
	/**
	 * Trains the classifier, discarding any previously learned model.
	 *
	 * @param provider provider of the labeled training documents
	 */
	public void train(LabelingDocumentProvider<T, L> provider);

	/**
	 * Predicts the label of a document.
	 *
	 * @param d the document to classify
	 * @return the predicted label, or <code>null</code> if no label could be determined
	 */
	public L classify(Document<T> d);

	/**
	 * Evaluates the classifier on labeled test documents.
	 *
	 * @param testProvider provider of the labeled test documents
	 * @param res array to store the results in; if <code>null</code> a new array is allocated
	 * @return the result array (implementation specific layout, see implementing classes)
	 */
	public double[] test(LabelingDocumentProvider<T, L> testProvider, double[] res);
}