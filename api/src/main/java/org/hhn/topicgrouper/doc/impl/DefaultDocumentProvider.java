package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

/**
 * A mutable {@link org.hhn.topicgrouper.doc.DocumentProvider}: documents and their
 * words are added incrementally, the vocabulary grows accordingly.
 *
 * @param <T> the type of the words
 */
public class DefaultDocumentProvider<T> extends WordMapDocumentProvider<T> {

	private static final long serialVersionUID = -6944447751973536193L;

	/**
	 * Creates a provider with an empty vocabulary.
	 */
	public DefaultDocumentProvider() {
		this(new DefaultVocab<T>());
	}
	
	/**
	 * Creates an empty provider using the given vocabulary.
	 *
	 * @param vocab the vocabulary, may already contain words
	 */
	public DefaultDocumentProvider(DefaultVocab<T> vocab) {
		super(vocab, new ArrayList<Document<T>>(), new TIntArrayList());
	}

	/**
	 * Creates an empty document that is part of this provider.
	 *
	 * @return the new document, appended to the end of {@link #getDocuments()}
	 */
	public DefaultDocument newDocument() {
		DefaultDocument document = new DefaultDocument(entries.size());
		entries.add(document);
		return document;
	}

	/**
	 * Copies a document without word filter.
	 *
	 * @param d the document to copy
	 * @param minFrequency minimum global frequency of words, 0 = keep all
	 * @return the new document or <code>null</code> if no words were left
	 */
	public DefaultDocument addDocument(Document<T> d, int minFrequency) {
		return addDocument(d, minFrequency, null);
	}

	/**
	 * Copies a document (words are mapped to this provider's vocabulary).
	 *
	 * @param d the document to copy
	 * @param minFrequency words with a lower global frequency in the source provider are dropped (0 = keep all)
	 * @param filter optional word filter
	 * @return the new document or <code>null</code> if no words were left (then nothing is added)
	 */
	public DefaultDocument addDocument(Document<T> d, int minFrequency,
			DocumentWordFilter<T> filter) {
		DefaultDocument r = newDocument();
		copyWords(d, r, minFrequency, filter);
		if (r.getSize() == 0) {
			removeDocument(r);
			return null;
		}
		else {
			return r;
		}
	}
	
	/**
	 * Copies the words of a document that pass the frequency and filter criteria.
	 *
	 * @param d source document
	 * @param r target document
	 * @param minFrequency words with a lower global frequency in the source provider are skipped (0 = keep all)
	 * @param filter optional word filter, may be <code>null</code>
	 */
	protected void copyWords(Document<T> d, DefaultDocument r, int minFrequency,
			DocumentWordFilter<T> filter) {
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			T word = d.getProvider().getVocab().getWord(index);
			if (filter == null || filter.acceptWord(word)) {
				int globalFr = d.getProvider().getWordFrequency(index);
				if (minFrequency == 0 || globalFr >= minFrequency) {
					r.addWord(word, d.getWordFrequency(index));
				}
			}
		}		
	}

	/**
	 * Copies a document keeping all words.
	 *
	 * @param d the document to copy
	 * @return the new document or <code>null</code> if it has no words
	 */
	public DefaultDocument addDocument(Document<T> d) {
		return addDocument(d, 0);
	}

	/**
	 * Removes the document and its word counts and renumbers the remaining documents.
	 *
	 * @param d the document to remove; afterwards no words can be added to it
	 */
	public void removeDocument(DefaultDocument d) {
		if (entries.remove(d)) {
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int fr = d.getWordFrequency(index);
				indexToFr.set(index, indexToFr.get(index) - fr);
				size -= fr;
				d.removed = true;
			}
		}
		// Update indices (quite ugly...)
		int i = 0;
		for (Document<T> d2 : entries) {
			((DefaultDocument)d2).index = i++;
		}
	}

	/**
	 * A document that belongs to this provider and whose words can be added; it keeps the
	 * provider's vocabulary and word frequencies up to date.
	 */
	public class DefaultDocument extends AbstractDocumentImpl<T> {
		private static final long serialVersionUID = 6606498048898601551L;
		/**
		 * Whether the document was removed from its provider via {@link DefaultDocumentProvider#removeDocument(DefaultDocument)}.
		 */
		private boolean removed = false;

		/**
		 * Creates an empty document; use {@link DefaultDocumentProvider#newDocument()} to register it with the provider.
		 *
		 * @param index the index of the document in the provider
		 */
		public DefaultDocument(int index) {
			super(index);
		}

		/**
		 * Adds one occurrence of a word.
		 *
		 * @param word the word, added to the vocabulary if new
		 */
		public void addWord(T word) {
			addWord(word, 1);
		}

		/**
		 * Adds several occurrences of a word.
		 *
		 * @param word the word, added to the vocabulary if new
		 * @param times the number of occurrences
		 * @throws IllegalStateException if the document was removed from its provider
		 */
		public void addWord(T word, int times) {
			if (removed) {
				throw new IllegalStateException(
						"document removed from its provider");
			}
			int index = vocab.addEntry(word);
			if (index >= indexToFr.size()) {
				indexToFr.fill(indexToFr.size(), index + 1, 0);
			}
			addWordOccurrence(index, times);
			indexToFr.set(index, indexToFr.get(index) + times);
			size += times;
		}
		
		/**
		 * @return the enclosing provider
		 */
		@Override
		public DocumentProvider<T> getProvider() {
			return DefaultDocumentProvider.this;
		}
	}

	/**
	 * Decides which words are copied when documents are added to a provider.
	 *
	 * @param <T> the word type
	 */
	public interface DocumentWordFilter<T> {
		/**
		 * Decides whether a word is copied.
		 *
		 * @param word the word
		 * @return <code>true</code> if the word should be kept
		 */
		public boolean acceptWord(T word);
	}
}
