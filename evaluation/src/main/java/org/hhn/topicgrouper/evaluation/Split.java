package org.hhn.topicgrouper.evaluation;

import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * A training / test split of a document collection.
 * 
 * @param <P>
 *            the type of the document providers
 */
public final class Split<P extends DocumentProvider<String>> {
	/** Documents used to learn the model. */
	public final P train;
	/** Held out documents used to evaluate the model. The vocabulary is a subset of the training vocabulary. */
	public final P test;

	/**
	 * @param train
	 *            the training documents
	 * @param test
	 *            the test documents
	 */
	public Split(P train, P test) {
		this.train = train;
		this.test = test;
	}
}
