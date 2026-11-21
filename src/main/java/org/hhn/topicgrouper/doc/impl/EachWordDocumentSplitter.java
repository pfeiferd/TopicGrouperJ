package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;

/**
 * Leave-one-out splitter: there is one split per word occurrence of the document. The test document
 * consists of that single word, the reference document is the original document (optionally minus that occurrence).
 *
 * @param <T> the word type
 */
public class EachWordDocumentSplitter<T> implements DocumentSplitter<T> {
	/**
	 * View of the current document, optionally with the current word occurrence removed.
	 */
	protected final Document<T> reducedDocument;
	/**
	 * View containing only the current word (size 1).
	 */
	protected final Document<T> oneWordDocument;
	/**
	 * The split (reference, test) that is returned by {@link #nextSplit()}; its content changes with the iteration.
	 */
	protected final Split<T> currentSplit;

	private Document<T> d;
	/**
	 * Index of the word of the current split, -1 if none.
	 */
	protected int currentIndex;
	/**
	 * Number of occurrences of the current word that were already returned.
	 */
	protected int currentFr;
	/**
	 * Iterator over the distinct words of the document.
	 */
	protected TIntIterator it;

	/**
	 * Creates the splitter.
	 *
	 * @param reduceRefDoc if <code>true</code> the test word occurrence is removed from the reference document
	 */
	public EachWordDocumentSplitter(final boolean reduceRefDoc) {
		reducedDocument = new Document<T>() {
			@Override
			public int getIndex() {
				return d.getIndex();
			}
			
			@Override
			public int getWords() {
				return d.getWords();
			}

			@Override
			public int getSize() {
				return d.getSize() - (reduceRefDoc ? 1 : 0);
			}

			@Override
			public TIntSet getWordIndices() {
				return d.getWordIndices();
			}

			@Override
			public int getWordFrequency(int index) {
				int fr = d.getWordFrequency(index);
				return (reduceRefDoc && index == currentIndex) ? fr - 1 : fr;
			}

			@Override
			public DocumentProvider<T> getProvider() {
				return d.getProvider();
			}
		};

		oneWordDocument = new Document<T>() {
			@Override
			public int getIndex() {
				return d.getIndex();
			}
			
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

	/**
	 * Sets the document to split and restarts the iteration.
	 */
	@Override
	public void setDocument(Document<T> d) {
		this.d = d;
		it = d.getWordIndices().iterator();
		currentIndex = -1;
		currentFr = 0;
	}

	/**
	 * @return the current document
	 */
	@Override
	public Document<T> getDocument() {
		return d;
	}

	/**
	 * @return the number of splits, i.e. the size (word occurrences) of the document
	 */
	@Override
	public int getSplits() {
		return d.getSize();
	}

	/**
	 * @return the next split (the same instance with changed content), or <code>null</code> when all word occurrences have been visited
	 */
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
