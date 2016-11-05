package org.hhn.topicgrouper.classify;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;

public interface SupervisedDocumentClassifier<T, L> {
	public void train(LabelingDocumentProvider<T, L> provider);
	public L classify(Document<T> d);
}