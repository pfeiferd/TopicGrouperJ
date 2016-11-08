package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class DefaultDocumentProvider<T> extends WordMapDocumentProvider<T> {
	public DefaultDocumentProvider() {
		this(new DefaultVocab<T>());
	}
	
	public DefaultDocumentProvider(DefaultVocab<T> vocab) {
		super(vocab, new ArrayList<Document<T>>(), new TIntArrayList());
	}

	public DefaultDocument newDocument() {
		DefaultDocument document = new DefaultDocument();
		entries.add(document);
		return document;
	}

	public DefaultDocument addDocument(Document<T> d, int minFrequency) {
		return addDocument(d, minFrequency, null);
	}

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

	public DefaultDocument addDocument(Document<T> d) {
		return addDocument(d, 0);
	}

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
	}

	public class DefaultDocument extends AbstractDocumentImpl<T> {
		private boolean removed = false;

		public void addWord(T word) {
			addWord(word, 1);
		}

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
		
		@Override
		public DocumentProvider<T> getProvider() {
			return DefaultDocumentProvider.this;
		}
	}

	public interface DocumentWordFilter<T> {
		public boolean acceptWord(T word);
	}
}
