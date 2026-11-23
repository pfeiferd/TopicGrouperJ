package org.hhn.topicgrouper.eval.classify.impl;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;

public class AbstractTopicBasedTfIdfClassifierTest extends TestCase {
	private static class WordTfIdfClassifier extends
			AbstractTopicBasedTfIdfClassifier<String, String> {
		private final int[] topics;

		WordTfIdfClassifier(int[] topics) {
			this.topics = topics;
		}

		@Override
		protected int[] getTopicIndices() {
			return topics;
		}

		@Override
		protected int getTopicIndex(int wordIndex) {
			for (int t : topics) {
				if (t == wordIndex) {
					return t;
				}
			}
			return Integer.MIN_VALUE;
		}
	}

	public void testRocchioClassification() {
		DefaultLabelingDocumentProvider<String, String> train = new DefaultLabelingDocumentProvider<String, String>();
		DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = train
				.newLabeledDocument("A");
		d.addWord("a", 5);
		d.addWord("common", 1);
		d = train.newLabeledDocument("A");
		d.addWord("a", 2);
		d.addWord("common", 1);
		d = train.newLabeledDocument("B");
		d.addWord("b", 4);
		d.addWord("common", 1);
		int a = train.getVocab().getIndex("a");
		int b = train.getVocab().getIndex("b");
		int common = train.getVocab().getIndex("common");

		// Topic 99 does not exist in any document (document frequency 0).
		WordTfIdfClassifier c = new WordTfIdfClassifier(new int[] { a, b, common, 99 });
		c.train(train);

		DefaultLabelingDocumentProvider<String, String> test = new DefaultLabelingDocumentProvider<String, String>(
				(DefaultVocab<String>) train.getVocab());
		d = test.newLabeledDocument("A");
		d.addWord("a", 1);
		d.addWord("common", 3);
		d = test.newLabeledDocument("B");
		d.addWord("b", 1);
		d.addWord("common", 3);
		// "common" occurs in all documents, so its idf is 0 and it does not count.
		assertEquals("A", c.classify(test.getDocuments().get(0)));
		assertEquals("B", c.classify(test.getDocuments().get(1)));
		double[] res = c.test(test, null);
		assertEquals(1.0, res[0], 1e-12);
		assertEquals(1.0, res[1], 1e-12);
	}
}
