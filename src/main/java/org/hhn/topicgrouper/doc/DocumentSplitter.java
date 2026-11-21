package org.hhn.topicgrouper.doc;


/**
 * Splits a document into pairs of a reference document (used to infer
 * document specific parameters) and a test document (used to evaluate the model),
 * e.g. for document completion perplexity.
 *
 * @param <T> the type of the words
 */
public interface DocumentSplitter<T> {
	/**
	 * Sets the document to split and restarts the iteration over its splits.
	 */
	public void setDocument(Document<T> d);

	public Document<T> getDocument();

	/**
	 * @return the next split or <code>null</code> if there are no more splits.
	 */
	public Split<T> nextSplit();

	/**
	 * The number of splits that {@link #nextSplit()} delivers for the current document.
	 */
	public int getSplits();

	/**
	 * A reference / test document pair.
	 */
	public interface Split<T> {
		/**
		 * The reference document. May be <code>null</code> if no reference is used.
		 */
		public Document<T> getRefDoc();

		/**
		 * The document on which a model is evaluated.
		 */
		public Document<T> getTestDoc();
	}
}
