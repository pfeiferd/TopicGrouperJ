package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;

public class FiftyFiftyDocumentSplitterTest extends TestCase {
	public void testHalvesArePartition() {
		DefaultDocumentProvider<Integer> provider = new DefaultDocumentProvider<Integer>();
		DefaultDocumentProvider<Integer>.DefaultDocument d = provider.newDocument();
		for (int i = 1; i <= 6; i++) {
			d.addWord(i, i);
		}
		assertEquals(21, d.getSize());

		FiftyFiftyDocumentSplitter<Integer> ds = new FiftyFiftyDocumentSplitter<Integer>(
				new Random(1));
		for (int round = 0; round < 5; round++) {
			ds.setDocument(d);
			assertSame(d, ds.getDocument());
			assertEquals(2, ds.getSplits());
			Split<Integer> s1 = ds.nextSplit();
			Split<Integer> s2 = ds.nextSplit();
			assertNull(ds.nextSplit());

			Document<Integer> a = s1.getRefDoc();
			Document<Integer> b = s1.getTestDoc();
			assertSame(a, s2.getTestDoc());
			assertSame(b, s2.getRefDoc());
			assertEquals(d.getIndex(), a.getIndex());
			assertSame(provider, a.getProvider());

			assertEquals(21, a.getSize() + b.getSize());
			assertTrue(Math.abs(a.getSize() - b.getSize()) <= 1);
			TIntIterator it = d.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				assertEquals(d.getWordFrequency(index), a.getWordFrequency(index)
						+ b.getWordFrequency(index));
			}
		}
	}
}
