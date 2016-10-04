package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class EachWordDocumentSplitter<T> implements DocumentSplitter<T> {
	protected final Document<T> reducedDocument;
	protected final Document<T> oneWordDocument;
	protected final Split<T> currentSplit;

	private Document<T> d;
	protected int toExcludeIndex;
	protected int currentIndex;
	protected int currentFr;
	protected TIntIterator it;

	public EachWordDocumentSplitter() {
		reducedDocument = new Document<T>() {
			@Override
			public int getWords() {
				return d.getWords();
			}

			@Override
			public int getSize() {
				return d.getSize() - 1;
			}

			@Override
			public TIntSet getWordIndices() {
				return d.getWordIndices();
			}

			@Override
			public int getWordFrequency(int index) {
				int fr = d.getWordFrequency(index);
				return index == toExcludeIndex ? fr - 1 : fr;
			}

			@Override
			public DocumentProvider<T> getProvider() {
				return d.getProvider();
			}
		};

		oneWordDocument = new Document<T>() {
			@Override
			public int getWords() {
				return d.getWords();
			}

			@Override
			public int getSize() {
				return 1;
			}

			@Override
			public TIntSet getWordIndices() {
				return d.getWordIndices();
			}

			@Override
			public int getWordFrequency(int index) {
				return index == currentIndex ? 1 : 0;
			}

			@Override
			public DocumentProvider<T> getProvider() {
				return d.getProvider();
			}
		};
		currentSplit = new Split<T>() {
			@Override
			public Document<T> getRefDoc() {
				return reducedDocument;
			}

			@Override
			public Document<T> getTestDoc() {
				return oneWordDocument;
			}
		};
	}

	@Override
	public void setDocument(Document<T> d) {
		this.d = d;
		it = d.getWordIndices().iterator();
		currentIndex = -1;
		currentFr = 0;
	}

	@Override
	public Document<T> getDocument() {
		return d;
	}

	@Override
	public int getSplits() {
		return d.getSize();
	}

	@Override
	public Split<T> nextSplit() {
		if (currentIndex != -1) {
			int fr = d.getWordFrequency(currentIndex);
			currentFr++;
			if (currentFr < fr) {
				return currentSplit;
			}
		}
		while (it.hasNext()) {
			currentIndex = it.next();
			int fr = d.getWordFrequency(currentIndex);
			if (fr > 0) {
				currentFr = 0;
				return currentSplit;
			}
		}
		currentIndex = -1;
		return null;
	}
}
