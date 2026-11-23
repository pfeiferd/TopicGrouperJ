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
	 *
	 * @param d the document to split
	 */
	public void setDocument(Document<T> d);

	/**
	 * The document currently being split.
	 *
	 * @return the document set with {@link #setDocument(Document)}, <code>null</code> if none
	 */
	public Document<T> getDocument();

	/**
	 * Delivers the next split of the current document.
	 *
	 * @return the next split or <code>null</code> if there are no more splits.
	 */
	public Split<T> nextSplit();

	/**
	 * The number of splits that {@link #nextSplit()} delivers for the current document.
	 *
	 * @return the number of splits
	 */
	public int getSplits();

	/**
	 * A reference / test document pair.
	 *
	 * @param <T> the type of the words
	 */
	public interface Split<T> {
		/**
		 * The reference document. May be <code>null</code> if no reference is used.
		 *
		 * @return the reference document or <code>null</code>
		 */
		public Document<T> getRefDoc();

		/**
		 * The document on which a model is evaluated.
		 *
		 * @return the test document
		 */
		public Document<T> getTestDoc();
	}
}
