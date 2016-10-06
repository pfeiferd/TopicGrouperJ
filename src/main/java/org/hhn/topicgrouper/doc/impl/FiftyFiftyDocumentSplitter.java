package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;

public class FiftyFiftyDocumentSplitter<T> implements DocumentSplitter<T> {
	private final Random random;
	private final Split<T> splitA;
	private final Split<T> splitB;
	private Document<T> d;
	private SplitDocument a;
	private SplitDocument b;
	private int split2;

	public FiftyFiftyDocumentSplitter(Random random) {
		this.random = random;
		splitA = new Split<T>() {
			@Override
			public Document<T> getRefDoc() {
				return a;
			}

			@Override
			public Document<T> getTestDoc() {
				return b;
			}
		};
		splitB = new Split<T>() {
			@Override
			public Document<T> getRefDoc() {
				return b;
			}

			@Override
			public Document<T> getTestDoc() {
				return a;
			}
		};
	}

	@Override
	public void setDocument(Document<T> d) {
		split2 = 0;
		this.d = d;
		a = new SplitDocument();
		b = new SplitDocument();

		int half = d.getSize() / 2;
		int fillA = 0;
		int fillB = 0;
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			int fr = d.getWordFrequency(index);
			for (int j = 0; j < fr; j++) {
				if (fillB >= half || (fillA < half && random.nextBoolean())) {
					// add to A.
					a.addWordOccurrence(index);
					fillA++;
				} else {
					// add to B.
					b.addWordOccurrence(index);
					fillB++;
				}
			}
		}
	}

	@Override
	public Document<T> getDocument() {
		return d;
	}

	@Override
	public int getSplits() {
		return 2;
	}

	@Override
	public Split<T> nextSplit() {
		if (split2 == 0) {
			split2 = 1;
			return splitA;
		} else if (split2 == 1) {
			split2 = 2;
			return splitB;
		}
		return null;
	}

	protected class SplitDocument extends AbstractDocumentImpl<T> {
		@Override
		public DocumentProvider<T> getProvider() {
			return d.getProvider();
		}
	}
}