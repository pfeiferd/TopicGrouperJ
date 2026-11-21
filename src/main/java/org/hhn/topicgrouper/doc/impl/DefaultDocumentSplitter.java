package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;

/**
 * Trivial splitter that yields exactly one split in which the whole document is the test document.
 *
 * @param <T> the word type
 */
public class DefaultDocumentSplitter<T> implements DocumentSplitter<T> {
	private final Split<T> defaultSplit;
	private Document<T> document;
	private boolean done;

	/**
	 * Creates a splitter whose reference document is <code>null</code>.
	 */
	public DefaultDocumentSplitter() {
		this(true);
	}

	/**
	 * Creates the splitter.
	 *
	 * @param nullRefDoc if <code>true</code> the reference document is <code>null</code>, otherwise it is the document itself
	 */
	public DefaultDocumentSplitter(final boolean nullRefDoc) {
		defaultSplit = new Split<T>() {
			@Override
			public Document<T> getRefDoc() {
				return nullRefDoc ? null : document;
			}
			
			@Override
			public Document<T> getTestDoc() {
				return document;
			}
		};
	}

	/**
	 * Sets the document to split and restarts the split iteration.
	 */
	@Override
	public void setDocument(Document<T> d) {
		this.document = d;
		done = false;
	}

	/**
	 * @return the current document
	 */
	@Override
	public Document<T> getDocument() {
		return document;
	}

	/**
	 * @return the single split on the first call, <code>null</code> afterwards
	 */
	@Override
	public Split<T> nextSplit() {
		if (!done) {
			done = true;
			return defaultSplit;
		}
		else {
			return null;
		}
	}

	/**
	 * @return 1
	 */
	@Override
	public int getSplits() {
		return 1;
	}
}
