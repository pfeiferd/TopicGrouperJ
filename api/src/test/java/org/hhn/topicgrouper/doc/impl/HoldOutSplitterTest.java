package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabeledDocument;

public class HoldOutSplitterTest extends TestCase {
	/**
	 * 10 documents; every document contains "c", its own word "w<i>", and the
	 * first five also "p".
	 */
	private DefaultDocumentProvider<String> createProvider() {
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int i = 0; i < 10; i++) {
			DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
			d.addWord("c", 2);
			d.addWord("w" + i);
			if (i < 5) {
				d.addWord("p");
			}
		}
		return p;
	}

	private static void assertVocabSubset(DocumentProvider<String> holdOut,
			DocumentProvider<String> rest, int minFrequency) {
		assertEquals(rest.getVocab(), holdOut.getVocab());
		for (Document<String> d : holdOut.getDocuments()) {
			assertTrue(d.getSize() > 0);
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				String word = holdOut.getVocab().getWord(index);
				assertTrue(word, rest.getVocab().getIndex(word) != -1);
				assertTrue(word, rest.getWordFrequency(index) >= minFrequency);
			}
		}
	}

	public void testHoldOutSplitterInvariants() {
		DefaultDocumentProvider<String> p = createProvider();
		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(new Random(1), p, 0.3, 2);
		DefaultDocumentProvider<String> holdOut = splitter.getHoldOut();
		DefaultDocumentProvider<String> rest = splitter.getRest();

		assertEquals(3, holdOut.getDocuments().size());
		assertEquals(7, rest.getDocuments().size());
		assertVocabSubset(holdOut, rest, 2);

		// Minimum frequency holds in the training part: the unique words are gone,
		// "c" is kept.
		for (int i = 0; i < rest.getVocab().getNumberOfWords(); i++) {
			assertTrue(rest.getWordFrequency(i) >= 2);
		}
		for (int i = 0; i < 10; i++) {
			assertEquals(-1, rest.getVocab().getIndex("w" + i));
		}
		assertEquals(14, rest.getWordFrequency(rest.getVocab().getIndex("c")));
		// The original provider is untouched.
		assertEquals(10, p.getDocuments().size());
		assertEquals(12, p.getVocab().getNumberOfWords());

		// Document sizes add up: the documents are disjoint.
		int restC = 0, holdOutC = 0;
		for (Document<String> d : rest.getDocuments()) {
			restC += d.getWordFrequency(rest.getVocab().getIndex("c"));
		}
		for (Document<String> d : holdOut.getDocuments()) {
			holdOutC += d.getWordFrequency(holdOut.getVocab().getIndex("c"));
		}
		assertEquals(14, restC);
		assertEquals(6, holdOutC);
	}

	public void testMinimumFrequencyOnTrainingPartOnly() {
		DefaultDocumentProvider<String> p = createProvider();
		// Hold out 4 documents; "p" (global frequency 5) might drop below 5 in the rest.
		HoldOutSplitter<String> inRest = new HoldOutSplitter<String>(new Random(1), p, 4, 5,
				true, null);
		assertEquals(4, inRest.getHoldOut().getDocuments().size());
		assertEquals(6, inRest.getRest().getDocuments().size());
		assertEquals(-1, inRest.getRest().getVocab().getIndex("p"));
		assertVocabSubset(inRest.getHoldOut(), inRest.getRest(), 5);

		// Global frequencies: "p" is kept in the rest although it is less frequent there.
		HoldOutSplitter<String> global = new HoldOutSplitter<String>(new Random(1), p, 4, 5,
				false, null);
		assertTrue(global.getRest().getVocab().getIndex("p") != -1);
		assertVocabSubset(global.getHoldOut(), global.getRest(), 0);
	}

	public void testLabelingHoldOutSplitterKeepsRatioPerLabel() {
		DefaultLabelingDocumentProvider<String, String> p = new DefaultLabelingDocumentProvider<String, String>();
		for (int i = 0; i < 8; i++) {
			DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = p
					.newLabeledDocument("a");
			d.addWord("c");
			d.addWord("a" + i % 3);
		}
		for (int i = 0; i < 4; i++) {
			DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = p
					.newLabeledDocument("b");
			d.addWord("c");
			d.addWord("b" + i % 2);
		}
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(1), p, 0.25, 1, 0);
		DefaultLabelingDocumentProvider<String, String> holdOut = splitter.getHoldOut();
		DefaultLabelingDocumentProvider<String, String> rest = splitter.getRest();
		assertEquals(2, holdOut.getDocumentsWithLabel("a").size());
		assertEquals(1, holdOut.getDocumentsWithLabel("b").size());
		assertEquals(6, rest.getDocumentsWithLabel("a").size());
		assertEquals(3, rest.getDocumentsWithLabel("b").size());
		assertEquals(3, holdOut.getDocuments().size());
		assertEquals(9, rest.getDocuments().size());
		assertVocabSubset(holdOut, rest, 1);
		for (LabeledDocument<String, String> d : holdOut.getLabeledDocuments()) {
			assertNotNull(d.getLabel());
		}

		// Requesting more labels than there are must not loop forever.
		splitter = new LabelingHoldOutSplitter<String, String>(new Random(1), p, 0.25, 1, 5);
		assertEquals(2, splitter.getRest().getAllLabels().size());
		// Only the most frequent label.
		splitter = new LabelingHoldOutSplitter<String, String>(new Random(1), p, 0.25, 1, 1);
		assertEquals(1, splitter.getRest().getAllLabels().size());
		assertTrue(splitter.getRest().getAllLabels().contains("a"));
	}

	/**
	 * A hold out document keeps every word that is in the final training
	 * vocabulary, even if the word only occurs in training documents of another
	 * label.
	 */
	public void testLabelingHoldOutKeepsWordsOfOtherLabels() {
		DefaultLabelingDocumentProvider<String, String> p = new DefaultLabelingDocumentProvider<String, String>();
		// Whichever x document is held out, it contains a word ("shared1" or
		// "shared2") that only the training documents of label y contain.
		DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = p
				.newLabeledDocument("x");
		d.addWord("shared1");
		d.addWord("xonly");
		d = p.newLabeledDocument("x");
		d.addWord("shared2");
		d.addWord("xonly");
		for (int i = 0; i < 2; i++) {
			d = p.newLabeledDocument("y");
			d.addWord("shared1");
			d.addWord("shared2");
			d.addWord("yonly");
		}
		for (int seed = 0; seed < 5; seed++) {
			LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
					new Random(seed), p, 0.5, 1, 0);
			DefaultLabelingDocumentProvider<String, String> holdOut = splitter.getHoldOut();
			DefaultLabelingDocumentProvider<String, String> rest = splitter.getRest();
			assertEquals(1, rest.getDocumentsWithLabel("x").size());
			assertEquals(1, rest.getDocumentsWithLabel("y").size());
			// shared1, shared2, xonly, yonly
			assertEquals(4, rest.getVocab().getNumberOfWords());
			List<LabeledDocument<String, String>> x = holdOut.getDocumentsWithLabel("x");
			assertEquals(1, x.size());
			assertEquals(2, x.get(0).getSize());
			assertEquals(1, x.get(0).getWordFrequency(holdOut.getVocab().getIndex("xonly")));
			assertVocabSubset(holdOut, rest, 1);
		}
	}
}
