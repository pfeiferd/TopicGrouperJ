package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.Document;

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
