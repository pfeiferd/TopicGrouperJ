package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;

public class FiftyFiftyDocumentSplitter<T> {
	private final Random random;

	public FiftyFiftyDocumentSplitter(Random random) {
		this.random = random;
	}

	@SuppressWarnings("unchecked")
	public Document<T>[] split(Document<T> d) {
		SplitDocument<T> a = new SplitDocument<T>(d);
		SplitDocument<T> b = new SplitDocument<T>(d);

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
		return new Document[] { a, b };
	}

	protected static class SplitDocument<T> extends AbstractDocumentImpl<T> {
		private final Document<T> baseDocument;

		public SplitDocument(Document<T> d) {
			baseDocument = d;
		}

		@Override
		public DocumentProvider<T> getProvider() {
			return baseDocument.getProvider();
		}
	}
}