package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;

/**
 * Splits a document randomly into two halves of (almost) equal size, A and B, and yields two splits:
 * A as reference with B as test, and B as reference with A as test.
 *
 * @param <T> the word type
 */
public class FiftyFiftyDocumentSplitter<T> implements DocumentSplitter<T> {
	private final Random random;
	private final Split<T> splitA;
	private final Split<T> splitB;
	private Document<T> d;
	private SplitDocument a;
	private SplitDocument b;
	private int splitCount;
	private int counter;

	/**
	 * Creates the splitter.
	 *
	 * @param random source of randomness for the assignment of word occurrences
	 */
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

	/**
	 * Randomly distributes the word occurrences of the document to two halves and restarts the iteration.
	 */
	@Override
	public void setDocument(Document<T> d) {
		counter++;
		splitCount = 0;
		this.d = d;
		a = new SplitDocument(d.getIndex());
		b = new SplitDocument(d.getIndex());

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

	/**
	 * @return the current (unsplit) document
	 */
	@Override
	public Document<T> getDocument() {
		return d;
	}

	/**
	 * @return 2
	 */
	@Override
	public int getSplits() {
		return 2;
	}

	/**
	 * @return the two splits in turn, then <code>null</code>
	 */
	@Override
	public Split<T> nextSplit() {
		if (splitCount == 0) {
			splitCount = 1;
			return splitA;
		} else if (splitCount == 1) {
			splitCount = 2;
			return splitB;
		}
		return null;
	}

	/**
	 * One half of the split document.
	 */
	protected class SplitDocument extends AbstractDocumentImpl<T> {
		private static final long serialVersionUID = 6914217677401557723L;

		/**
		 * Creates an empty half.
		 *
		 * @param index the index of the original document
		 */
		public SplitDocument(int index) {
			super(index);
		}
		
		@Override
		public DocumentProvider<T> getProvider() {
			return d.getProvider();
		}
	}
}