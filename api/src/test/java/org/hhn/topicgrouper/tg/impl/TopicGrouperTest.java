package org.hhn.topicgrouper.tg.impl;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;

public class TopicGrouperTest extends TestCase {
	private DefaultDocumentProvider<String> createProvider() {
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		String[][] docs = { { "a", "a", "b", "b", "c" }, { "a", "b", "b", "c", "c" },
				{ "x", "x", "y", "y", "z" }, { "x", "y", "y", "z", "z" },
				{ "a", "b", "x", "y", "rare" } };
		for (String[] words : docs) {
			DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
			for (String w : words) {
				d.addWord(w);
			}
		}
		return p;
	}

	private static class Recorder implements TGSolutionListener<String> {
		int joins;
		double lastLikelihood;
		TGSolution<String> solution;

		public void beforeInitialization(int maxTopics, int documents) {
		}

		public void initalizing(double percentage) {
		}

		public void initialized(TGSolution<String> initialSolution) {
			solution = initialSolution;
		}

		public void updatedSolution(int newTopicIndex, int oldTopicIndex,
				double improvement, int t1Size, int t2Size,
				TGSolution<String> solution) {
			joins++;
			lastLikelihood = solution.getTotalLogLikelhood();
		}

		public void done() {
		}
	}

	public void testEHACAndLowMemAgree() {
		Recorder r1 = new Recorder();
		new EHACTopicGrouper<String>(2, createProvider(), 1).solve(r1);
		Recorder r2 = new Recorder();
		new LowMemTopicGrouper<String>(2, createProvider(), 1).solve(r2);

		// 6 frequent words ("rare" is dropped) -> 5 joins down to one topic.
		assertEquals(5, r1.joins);
		assertEquals(5, r2.joins);
		assertEquals(r1.lastLikelihood, r2.lastLikelihood, 1e-9);
		assertEquals(1, r1.solution.getNumberOfTopics());
	}

	public void testMinTopicsAndInfrequentWords() {
		DefaultDocumentProvider<String> p = createProvider();
		Recorder r = new Recorder();
		new EHACTopicGrouper<String>(2, p, 2).solve(r);
		assertEquals(4, r.joins);
		assertEquals(2, r.solution.getNumberOfTopics());
		// Both groups {a,b,c} and {x,y,z} should be found.
		int ta = r.solution.getTopicForWord(p.getVocab().getIndex("a"));
		assertEquals(ta, r.solution.getTopicForWord(p.getVocab().getIndex("b")));
		assertEquals(ta, r.solution.getTopicForWord(p.getVocab().getIndex("c")));
		assertFalse(ta == r.solution.getTopicForWord(p.getVocab().getIndex("x")));
		assertEquals(-1, r.solution.getTopicForWord(p.getVocab().getIndex("rare")));
	}

	public void testSingleTopicDoesNotFail() {
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		p.newDocument().addWord("only", 3);
		new EHACTopicGrouper<String>(1, p, 1).solve(new Recorder());
		new LowMemTopicGrouper<String>(1, p, 1).solve(new Recorder());
	}

	public void testInvalidMinWordFrequency() {
		try {
			new EHACTopicGrouper<String>(0, createProvider(), 1);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testTopicWordLogLikelihoodIsFinite() {
		DefaultDocumentProvider<String> p = createProvider();
		final AbstractTopicGrouper<String>[] holder = new AbstractTopicGrouper[1];
		holder[0] = new EHACTopicGrouper<String>(2, p, 1);
		holder[0].solve(new Recorder());
		int a = p.getVocab().getIndex("a");
		for (int t : new int[] { 0 }) {
			assertFalse(Double.isNaN(holder[0].computeTopicWordLogLikelihood(t, a)));
		}
	}
}
