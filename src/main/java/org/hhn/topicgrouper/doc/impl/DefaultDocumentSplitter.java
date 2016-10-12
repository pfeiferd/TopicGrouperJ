package org.hhn.topicgrouper.doc.impl;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter;

public class DefaultDocumentSplitter<T> implements DocumentSplitter<T> {
	private final Split<T> defaultSplit;
	private Document<T> document;
	private boolean done;
	
	public DefaultDocumentSplitter() {
		defaultSplit = new Split<T>() {
			@Override
			public Document<T> getRefDoc() {
				return document;
			}
			
			@Override
			public Document<T> getTestDoc() {
				return document;
			}
		};
	}

	@Override
	public void setDocument(Document<T> d) {
		this.document = d;
		done = false;
	}

	@Override
	public Document<T> getDocument() {
		return document;
	}

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

	@Override
	public int getSplits() {
		return 1;
	}
}
