package org.hhn.topicgrouper.doc;


public interface DocumentSplitter<T> {
	public void setDocument(Document<T> d);

	public Document<T> getDocument();

	public Split<T> nextSplit();

	public int getSplits();

	public interface Split<T> {
		public Document<T> getRefDoc();

		public Document<T> getTestDoc();
	}
}
