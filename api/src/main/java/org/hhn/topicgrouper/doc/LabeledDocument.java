package org.hhn.topicgrouper.doc;


/**
 * A document with a class label.
 *
 * @param <T> the type of the words
 * @param <L> the type of the label
 */
public interface LabeledDocument<T,L> extends Document<T> {
	/**
	 * The class label of this document.
	 *
	 * @return the label
	 */
	L getLabel();
}
