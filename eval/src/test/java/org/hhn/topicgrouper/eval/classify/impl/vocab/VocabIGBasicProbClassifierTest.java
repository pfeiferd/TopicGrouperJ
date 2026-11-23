package org.hhn.topicgrouper.eval.classify.impl.vocab;

import gnu.trove.map.hash.TObjectIntHashMap;
import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;

public class VocabIGBasicProbClassifierTest extends TestCase {
	private DefaultLabelingDocumentProvider<String, String> provider;

	@Override
	protected void setUp() {
		provider = new DefaultLabelingDocumentProvider<String, String>();
		// "u" is in every document, "d" only in the A documents,
		// "w" in one A and both B documents.
		DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = provider
				.newLabeledDocument("A");
		d.addWord("u");
		d.addWord("d");
		d.addWord("w");
		d = provider.newLabeledDocument("A");
		d.addWord("u");
		d.addWord("d");
		d = provider.newLabeledDocument("B");
		d.addWord("u");
		d.addWord("w");
		d = provider.newLabeledDocument("B");
		d.addWord("u");
		d.addWord("w");
	}

	private static double entropyTerm(double p) {
		return p * Math.log(p);
	}

	public void testScoreMatchesHandComputation() {
		VocabIGBasicProbClassifier<String, String> c = new VocabIGBasicProbClassifier<String, String>(0,
				provider, 3);
		int w = provider.getVocab().getIndex("w");
		double score = c.computeScore(w, provider, new TObjectIntHashMap<String>(),
				new TObjectIntHashMap<String>());
		// Documents with w: 3 (A: 1, B: 2), without: 1 (A: 1, B: 0); 2 labels,
		// Laplace smoothed: p(A|w) = 2/5, p(B|w) = 3/5, p(A|not w) = 2/3, p(B|not w) = 1/3
		double hw = -(entropyTerm(2.0 / 5) + entropyTerm(3.0 / 5));
		double hnw = -(entropyTerm(2.0 / 3) + entropyTerm(1.0 / 3));
		assertEquals(3.0 / 4 * hw + 1.0 / 4 * hnw, score, 1e-12);

		int u = provider.getVocab().getIndex("u");
		int dd = provider.getVocab().getIndex("d");
		double scoreU = c.computeScore(u, provider, new TObjectIntHashMap<String>(),
				new TObjectIntHashMap<String>());
		double scoreD = c.computeScore(dd, provider, new TObjectIntHashMap<String>(),
				new TObjectIntHashMap<String>());
		// u: p(A|u) = 3/6: conditional entropy log 2.
		assertEquals(Math.log(2), scoreU, 1e-12);
		// d: 2 with (A: 2) -> p(A|d) = 3/4, 2 without (B: 2) -> p(B|not d) = 3/4
		assertEquals(-(entropyTerm(3.0 / 4) + entropyTerm(1.0 / 4)), scoreD, 1e-12);
		assertTrue(scoreD < score && score < scoreU);
	}

	public void testBestWordsOrderedByScore() {
		VocabIGBasicProbClassifier<String, String> c = new VocabIGBasicProbClassifier<String, String>(0,
				provider, 2);
		assertEquals(2, c.topicIds.length);
		assertEquals(provider.getVocab().getIndex("d"), c.topicIds[0]);
		assertEquals(provider.getVocab().getIndex("w"), c.topicIds[1]);
		assertEquals(Integer.MIN_VALUE, c.getTopicIndex(provider.getVocab().getIndex("u")));

		c.train(provider);
		assertEquals("A", c.classify(provider.getDocuments().get(1)));
		assertEquals("B", c.classify(provider.getDocuments().get(2)));
		double[] res = c.test(provider, null);
		assertEquals(1.0, res[0], 1e-12);
		assertEquals(1.0, res[1], 1e-12);
	}

	public void testDFVariantSelectsMostFrequentWords() {
		VocabDFBasicProbClassifier<String, String> c = new VocabDFBasicProbClassifier<String, String>(0,
				provider, 2);
		assertEquals(provider.getVocab().getIndex("u"), c.topicIds[0]);
		assertEquals(provider.getVocab().getIndex("w"), c.topicIds[1]);
	}
}
