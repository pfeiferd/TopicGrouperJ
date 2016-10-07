package org.hhn.topicgrouper.doc.impl;

import gnu.trove.iterator.TIntIterator;
import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentSplitter.Split;

public class EachWordDocumentSplitterTest extends TestCase {
	public void testDocumentSplit() {
		DefaultDocumentProvider<Integer> provider = new DefaultDocumentProvider<Integer>();
		DefaultDocumentProvider<Integer>.DefaultDocument d = provider
				.newDocument();
		for (int i = 1; i <= 10; i++) {
			d.addWord(i, i);
		}
		assertEquals(55, d.getSize());

		EachWordDocumentSplitter<Integer> ds = new EachWordDocumentSplitter<Integer>();
		ds.setDocument(d);

		int splits = ds.getSplits();
		assertEquals(55, splits);

		for (int i = 1; i <= 55; i++) {
			Split<Integer> s = ds.nextSplit();
			Document<Integer> td = s.getTestDoc();
			Document<Integer> rd = s.getRefDoc();
			
			assertEquals(1, td.getSize());
			TIntIterator it = td.getWordIndices().iterator();
			while (it.hasNext()) {
				int index = it.next();
				int word = d.getProvider().getWord(index);
				int fr = td.getWordFrequency(index);
				if (fr == 1) {
					assertEquals(word - 1, rd.getWordFrequency(index));
				}
				else {
					assertEquals(0, fr);
					assertEquals(word, rd.getWordFrequency(index));
				}
			}
		}
		assertEquals(null, ds.nextSplit());
	}
}
