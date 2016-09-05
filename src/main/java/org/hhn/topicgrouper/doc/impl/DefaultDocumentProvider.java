package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class DefaultDocumentProvider<T> extends WordMapDocumentProvider<T> {
	private int nextIndex;

	public DefaultDocumentProvider() {
		super(new TObjectIntHashMap<T>(), new TIntObjectHashMap<T>(),
				new ArrayList<Document<T>>(), new TIntArrayList());
	}

	public DefaultDocument newDocument() {
		DefaultDocument document = new DefaultDocument();
		entries.add(document);
		return document;
	}

	public DefaultDocument addDocument(Document<T> d, int minFrequency) {
		DefaultDocument r = newDocument();
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			if (minFrequency == 0
					|| d.getProvider().getWordFrequency(index) >= minFrequency) {
				r.addWord(d.getProvider().getWord(index),
						d.getWordFrequency(index));
			}
		}
		return r;
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
			int index;
			if (!wordToIndex.containsKey(word)) {
				index = nextIndex++;
				wordToIndex.put(word, index);
				indexToWord.put(index, word);
				if (index >= indexToFr.size()) {
					indexToFr.fill(indexToFr.size(), index + 1, 0);
				}
			} else {
				index = wordToIndex.get(word);
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
}
