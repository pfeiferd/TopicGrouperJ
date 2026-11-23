package org.hhn.topicgrouper.tg.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSenseSolutionListener;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolver;

/**
 * The multithreaded solvers must produce exactly the joins of the single
 * threaded ones, and the incremental (sparse) log likelihoods must equal the
 * ones computed from scratch.
 */
public class ParallelSolverTest extends TestCase {
	/** Several themes, zipf-like word frequencies, some shared words. */
	private static DefaultDocumentProvider<String> createCorpus(int themes, int wordsPerTheme, int docs, int length,
			long seed) {
		Random random = new Random(seed);
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int i = 0; i < docs; i++) {
			DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
			int theme = random.nextInt(themes);
			for (int j = 0; j < length; j++) {
				int w = (int) Math.min(wordsPerTheme - 1, Math.floor(wordsPerTheme * Math.pow(random.nextDouble(), 2)));
				if (random.nextInt(10) == 0) {
					d.addWord("shared" + random.nextInt(wordsPerTheme / 4 + 1));
				} else {
					d.addWord("t" + theme + "w" + w);
				}
			}
		}
		return p;
	}

	/** Records the sequence of joins and splits. */
	private static class Recorder implements TGSenseSolutionListener<String> {
		final List<String> events = new ArrayList<String>();
		final List<Double> improvements = new ArrayList<Double>();
		double finalLikelihood;
		int maxTopics;
		boolean initializedAtMax;

		public void beforeInitialization(int maxTopics, int documents) {
			this.maxTopics = maxTopics;
		}

		public void initalizing(double percentage) {
			assertTrue(percentage >= 0 && percentage <= 1.0000001);
		}

		public void initialized(TGSolution<String> initialSolution) {
			initializedAtMax = initialSolution.getNumberOfTopics() == maxTopics;
		}

		public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement, int t1Size,
				int t2Size, TGSolution<String> solution) {
			events.add("join " + newTopicIndex + " " + oldTopicIndex + " " + t1Size + " " + t2Size);
			improvements.add(improvement);
			finalLikelihood = solution.getTotalLogLikelhood();
		}

		public void splitSense(int topicIndex, int wordIndex, int senseIndex, int documents, double improvement,
				TGSenseSolution<String> solution) {
			events.add("split " + topicIndex + " " + wordIndex + " " + senseIndex + " " + documents);
			improvements.add(improvement);
		}

		public void done() {
		}
	}

	private Recorder run(TGSolver<String> solver) {
		Recorder r = new Recorder();
		solver.solve(r);
		assertTrue(r.initializedAtMax);
		return r;
	}

	private void assertSameRun(Recorder a, Recorder b) {
		assertEquals(a.events.size(), b.events.size());
		for (int i = 0; i < a.events.size(); i++) {
			assertEquals("event " + i, a.events.get(i), b.events.get(i));
			assertEquals("improvement " + i, a.improvements.get(i), b.improvements.get(i), 1e-9);
		}
		assertEquals(a.finalLikelihood, b.finalLikelihood, 1e-9);
	}

	public void testEHACThreadsGiveSameResult() {
		DefaultDocumentProvider<String> p = createCorpus(4, 60, 300, 25, 3);
		Recorder single = run(new EHACTopicGrouper<String>(2, p, 1));
		assertTrue(single.events.size() > 100);
		for (int threads : new int[] { 2, 3, 7 }) {
			assertSameRun(single, run(new EHACTopicGrouper<String>(2, p, 1, threads)));
		}
		// Stopping early as well.
		assertSameRun(run(new EHACTopicGrouper<String>(2, p, 5)), run(new EHACTopicGrouper<String>(2, p, 5, 4)));
	}

	public void testLowMemThreadsGiveSameResult() {
		DefaultDocumentProvider<String> p = createCorpus(4, 60, 300, 25, 3);
		Recorder single = run(new LowMemTopicGrouper<String>(2, p, 1));
		Recorder ehac = run(new EHACTopicGrouper<String>(2, p, 1));
		// The lazy solver may resolve exact ties differently from EHAC, the result is the same.
		assertEquals(ehac.events.size(), single.events.size());
		assertEquals(ehac.finalLikelihood, single.finalLikelihood, 1e-9);
		for (int threads : new int[] { 2, 5 }) {
			assertSameRun(single, run(new LowMemTopicGrouper<String>(2, p, 1, threads)));
		}
		LowMemTopicGrouper<String> eagerSingle = new LowMemTopicGrouper<String>(2, p, 1);
		eagerSingle.setDeferJCUpdates(false);
		Recorder eager = run(eagerSingle);
		LowMemTopicGrouper<String> eagerThreads = new LowMemTopicGrouper<String>(2, p, 1, 3);
		eagerThreads.setDeferJCUpdates(false);
		assertSameRun(eager, run(eagerThreads));
		assertEquals(ehac.finalLikelihood, eager.finalLikelihood, 1e-9);
	}

	public void testSenseSplittingThreadsGiveSameResult() {
		DefaultDocumentProvider<String> p = createCorpus(3, 40, 200, 20, 5);
		Recorder single = run(new SenseSplittingTopicGrouper<String>(1, p, 1));
		boolean splits = false;
		for (String e : single.events) {
			splits |= e.startsWith("split");
		}
		assertTrue("expected splits", splits);
		for (int threads : new int[] { 2, 4 }) {
			assertSameRun(single, run(new SenseSplittingTopicGrouper<String>(1, p, 1,
					SenseSplittingTopicGrouper.DEFAULT_MIN_SENSE_LIFT, threads)));
		}
	}

	/**
	 * Checks after every join that the document lists cover the documents of
	 * the topic, that the incrementally maintained document part and the log
	 * likelihood of the joined topic equal the values computed from scratch over
	 * all documents, and that the sparse pair likelihood equals the dense one.
	 */
	private static class CheckingGrouper extends EHACTopicGrouper<String> {
		int checks;

		CheckingGrouper(DefaultDocumentProvider<String> p, int threads) {
			super(1, p, 1, threads);
		}

		private double denseDocPart(int topic) {
			int[] row = topicFrequencyPerDocuments[topic];
			double sum = 0;
			for (int d = 0; d < row.length; d++) {
				if (row[d] > 0 && documentSizes[d] > 0) {
					sum += row[d] * (Math.log(row[d]) - logDocumentSizes[d]);
				}
			}
			return sum;
		}

		private double densePairLikelihood(int t1, int t2) {
			int[] a = topicFrequencyPerDocuments[t1];
			int[] b = topicFrequencyPerDocuments[t2];
			double sum = 0;
			for (int d = 0; d < a.length; d++) {
				int fr = a[d] + b[d];
				if (fr > 0 && documentSizes[d] > 0) {
					sum += fr * (Math.log(fr) - logDocumentSizes[d]);
				}
			}
			sum += sumWordFrTimesLogWordFrByTopic[t1] + sumWordFrTimesLogWordFrByTopic[t2];
			int sizeSum = topicSizes[t1] + topicSizes[t2];
			return sum - sizeSum * Math.log(sizeSum);
		}

		@Override
		protected void joinTopics(int jci, int gone, double logLikelihood, double improvement,
				org.hhn.topicgrouper.tg.TGSolutionListener<String> solutionListener) {
			assertEquals(densePairLikelihood(jci, gone), logLikelihood, 1e-7);
			assertEquals(densePairLikelihood(jci, gone), computeTwoTopicLogLikelihood(jci, gone), 1e-7);
			assertEquals(densePairLikelihood(jci, gone), computeTwoTopicLogLikelihood(gone, jci), 1e-7);
			super.joinTopics(jci, gone, logLikelihood, improvement, solutionListener);
			assertEquals(denseDocPart(jci), topicDocPart[jci], 1e-7);
			assertEquals(denseDocPart(jci) + sumWordFrTimesLogWordFrByTopic[jci]
					- topicSizes[jci] * Math.log(topicSizes[jci]), topicLogLikelihoods[jci], 1e-7);
			// Every document with a count is in the document list.
			int[] row = topicFrequencyPerDocuments[jci];
			int[] docs = topicDocs[jci];
			int n = topicDocsSize[jci];
			int k = 0;
			for (int d = 0; d < row.length; d++) {
				if (row[d] > 0) {
					while (k < n && docs[k] < d) {
						k++;
					}
					assertTrue("document " + d + " missing", k < n && docs[k] == d);
				}
			}
			for (int i = 1; i < n; i++) {
				assertTrue(docs[i - 1] < docs[i]);
			}
			checks++;
		}
	}

	public void testIncrementalLikelihoodsMatchDenseOnes() {
		DefaultDocumentProvider<String> p = createCorpus(3, 30, 120, 15, 11);
		CheckingGrouper g = new CheckingGrouper(p, 3);
		Recorder r = run(g);
		assertEquals(r.events.size(), g.checks);
		assertTrue(g.checks > 50);
	}

	public void testThreadsAccessor() {
		DefaultDocumentProvider<String> p = createCorpus(2, 5, 10, 5, 1);
		assertEquals(1, new EHACTopicGrouper<String>(1, p, 1).getThreads());
		assertEquals(4, new EHACTopicGrouper<String>(1, p, 1, 4).getThreads());
		assertEquals(1, new LowMemTopicGrouper<String>(1, p, 1, 0).getThreads());
	}
}
