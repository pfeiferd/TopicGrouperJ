package org.hhn.topicgrouper.validation;

import gnu.trove.iterator.TIntIterator;

import java.util.Random;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

public class InDocumentHoldOutSplitter<T> {
	private DefaultDocumentProvider<T> holdOut;
	private DefaultDocumentProvider<T> rest;

	public InDocumentHoldOutSplitter(Random random,
			DocumentProvider<T> origDocuments, double holdOutRatio,
			int minGlobalWordFrequency) {
		rest = new DefaultDocumentProvider<T>();
		holdOut = new DefaultDocumentProvider<T>();
		for (Document<T> d : origDocuments.getDocuments()) {
			addDocument(d, minGlobalWordFrequency, random, holdOutRatio);
		}
	}

	private void addDocument(Document<T> d, int minFrequency, Random random,
			double holdOutRatio) {
		DefaultDocumentProvider<T>.DefaultDocument r = rest.newDocument();
		DefaultDocumentProvider<T>.DefaultDocument h = holdOut.newDocument();
		TIntIterator it = d.getWordIndices().iterator();
		while (it.hasNext()) {
			int index = it.next();
			if (d.getProvider().getWordFrequency(index) >= minFrequency) {
				int fr = d.getWordFrequency(index);
				int c = 0;
				for (int i = 0; i < fr; i++) {
					// One draw per word occurrence
					if (random.nextDouble() <= holdOutRatio) {
						c++;
					}
				}
				if (c > 0) {
					h.addWord(d.getProvider().getWord(index), c);
				}
				if (fr - c > 0) {
					r.addWord(d.getProvider().getWord(index), fr - c);
				}
			}
		}
	}

	public DefaultDocumentProvider<T> getHoldOut() {
		return holdOut;
	}

	public DefaultDocumentProvider<T> getRest() {
		return rest;
	}
}
