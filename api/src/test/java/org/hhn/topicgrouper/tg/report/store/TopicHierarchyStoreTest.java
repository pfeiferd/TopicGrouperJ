package org.hhn.topicgrouper.tg.report.store;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSenseSolutionListener;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListenerMultiplexer;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.SenseSplittingTopicGrouper;
import org.hhn.topicgrouper.tg.report.MindMapSolutionReporter;

/**
 * Tests the recording of hierarchies with senses, the JSON and object files and
 * the replay of earlier solutions.
 */
public class TopicHierarchyStoreTest extends TestCase {
	private static final int WORDS = 40;
	private static final int DRAWS = 20;

	/** Two themes with a homonym "h" that accompanies the first word of every theme. */
	private static DefaultDocumentProvider<String> createCorpus(boolean homonym, int themes, int docsPerTheme) {
		Random random = new Random(7);
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int theme = 0; theme < themes; theme++) {
			for (int i = 0; i < docsPerTheme; i++) {
				DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
				for (int j = 0; j < DRAWS; j++) {
					int w = random.nextInt(WORDS);
					d.addWord("t" + theme + "w" + w);
					if (homonym && w == 0) {
						d.addWord("h");
					}
				}
			}
		}
		return p;
	}

	/**
	 * Records, per number of topics, the type sets of the topics of the live
	 * solution (the last state at that number of topics) and the types whose
	 * frequency dropped to 0 by a reunification.
	 */
	private static class Recorder implements TGSenseSolutionListener<String> {
		final Map<Integer, Set<Set<Integer>>> topicsAt = new HashMap<Integer, Set<Set<Integer>>>();
		final Map<Integer, Set<Integer>> zeroTypesAt = new HashMap<Integer, Set<Integer>>();
		int maxTopics;

		public void beforeInitialization(int maxTopics, int documents) {
			this.maxTopics = maxTopics;
		}

		public void initalizing(double percentage) {
		}

		public void initialized(TGSolution<String> initialSolution) {
			record(initialSolution);
		}

		public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement, int t1Size,
				int t2Size, TGSolution<String> solution) {
			record(solution);
		}

		public void splitSense(int topicIndex, int wordIndex, int senseIndex, int documents, double improvement,
				TGSenseSolution<String> solution) {
			record(solution);
		}

		public void done() {
		}

		private void record(TGSolution<String> solution) {
			Set<Set<Integer>> sets = new HashSet<Set<Integer>>();
			for (TIntCollection topic : solution.getTopics()) {
				if (topic != null) {
					Set<Integer> s = new HashSet<Integer>();
					TIntIterator it = topic.iterator();
					while (it.hasNext()) {
						s.add(it.next());
					}
					sets.add(s);
				}
			}
			topicsAt.put(solution.getNumberOfTopics(), sets);
			Set<Integer> zero = new HashSet<Integer>();
			if (solution instanceof TGSenseSolution) {
				TGSenseSolution<String> s = (TGSenseSolution<String>) solution;
				for (int t = solution.getVocab().getNumberOfWords(); t < s.getNumberOfWordTypes(); t++) {
					if (s.getGlobalWordFrequency(t) == 0) {
						zero.add(t);
					}
				}
			}
			zeroTypesAt.put(solution.getNumberOfTopics(), zero);
		}
	}

	private MindMapSolutionReporter<String> reporter;
	private Recorder recorder;
	private TopicHierarchy<String> hierarchy;

	private void solveWithSenses() {
		DefaultDocumentProvider<String> p = createCorpus(true, 2, 60);
		reporter = new MindMapSolutionReporter<String>(5, false, 1.1, 0);
		recorder = new Recorder();
		TGSolutionListenerMultiplexer<String> listeners = new TGSolutionListenerMultiplexer<String>();
		listeners.addSolutionListener(reporter);
		listeners.addSolutionListener(recorder);
		new SenseSplittingTopicGrouper<String>(1, p, 1).solve(listeners);
		hierarchy = reporter.getHierarchy();
	}

	private void solvePlain() {
		DefaultDocumentProvider<String> p = createCorpus(false, 2, 30);
		reporter = new MindMapSolutionReporter<String>(5, false, 1.1, 0);
		recorder = new Recorder();
		TGSolutionListenerMultiplexer<String> listeners = new TGSolutionListenerMultiplexer<String>();
		listeners.addSolutionListener(reporter);
		listeners.addSolutionListener(recorder);
		new EHACTopicGrouper<String>(1, p, 1).solve(listeners);
		hierarchy = reporter.getHierarchy();
	}

	private Set<Integer> leafIds(TopicHierarchy<String> h, MapNode<String> node) {
		List<MapNode<String>> leaves = new ArrayList<MapNode<String>>();
		h.collectLeaves(node, leaves);
		Set<Integer> res = new HashSet<Integer>();
		for (MapNode<String> leaf : leaves) {
			res.add(leaf.getWordInfo().getWordId());
		}
		return res;
	}

	private Set<String> leafLabels(TopicHierarchy<String> h, MapNode<String> node) {
		List<MapNode<String>> leaves = new ArrayList<MapNode<String>>();
		h.collectLeaves(node, leaves);
		Set<String> res = new HashSet<String>();
		for (MapNode<String> leaf : leaves) {
			res.add(leaf.getWordInfo().getLabel());
		}
		return res;
	}

	public void testReporterRecordsSenses() {
		solveWithSenses();
		assertTrue("expected splits", reporter.getNumberOfSplits() >= 1);
		assertEquals(reporter.getNumberOfSplits(), hierarchy.getNumberOfSenses());
		assertEquals(recorder.maxTopics, hierarchy.getNumberOfWords());
		assertEquals(1, hierarchy.getRoots().size());
		// Every node is preceded by its children, frequencies add up over the leaves.
		for (int i = 0; i < hierarchy.size(); i++) {
			MapNode<String> node = hierarchy.get(i);
			assertEquals(i, hierarchy.indexOf(node));
			if (!node.isLeaf()) {
				int sum = 0;
				List<MapNode<String>> leaves = new ArrayList<MapNode<String>>();
				hierarchy.collectLeaves(node, leaves);
				for (MapNode<String> leaf : leaves) {
					sum += leaf.getWordInfo().getFrequency();
					assertTrue(hierarchy.indexOf(leaf) < i);
				}
				assertEquals("node " + node.getId(), node.getTopicFrequency(), sum);
				assertEquals(leaves.size(), hierarchy.getTopicSize(node));
			}
		}
		// Split nodes have a sense leaf as child and an id of at least the number of words.
		int splitNodes = 0;
		for (MapNode<String> node : hierarchy.getNodes()) {
			if (hierarchy.isSplitNode(node)) {
				splitNodes++;
				assertTrue(node.getId() >= hierarchy.getNumberOfWords());
				assertTrue(node.getRightNode().isSenseLeaf());
				assertEquals(hierarchy.indexOf(node) - 1, hierarchy.indexOf(node.getRightNode()));
				WordInfo<String> sense = node.getRightNode().getWordInfo();
				assertTrue(sense.getDocuments() > 0);
				assertTrue(sense.getSense() >= 1);
				// The word of the sense has a word leaf of its own (the remainder).
				boolean found = false;
				for (MapNode<String> leaf : hierarchy.getNodes()) {
					found |= leaf.isLeaf() && !leaf.isSenseLeaf() && leaf.getWordInfo().getWord().equals(sense.getWord());
				}
				assertTrue(found);
			}
		}
		assertEquals(reporter.getNumberOfSplits(), splitNodes);
	}

	public void testTopicsAtMatchesSolver() {
		solveWithSenses();
		assertTopicsAtMatchSolver(hierarchy);
		solvePlain();
		assertEquals(0, hierarchy.getNumberOfSenses());
		assertTopicsAtMatchSolver(hierarchy);
	}

	private void assertTopicsAtMatchSolver(TopicHierarchy<String> h) {
		for (int n = 1; n <= h.getMaxTopics(); n++) {
			List<MapNode<String>> nodes = h.getTopicsAt(n);
			assertEquals(n, nodes.size());
			Set<Set<Integer>> sets = new HashSet<Set<Integer>>();
			for (MapNode<String> node : nodes) {
				Set<Integer> ids = leafIds(h, node);
				ids.removeAll(recorder.zeroTypesAt.get(n));
				sets.add(ids);
			}
			assertEquals("at " + n + " topics", recorder.topicsAt.get(n), sets);
		}
		try {
			h.getTopicsAt(0);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testMapNodeTGSolutionReplaysSolver() {
		solveWithSenses();
		MapNodeTGSolution<String> s = new MapNodeTGSolution<String>(hierarchy);
		assertEquals(hierarchy.getNumberOfWords(), s.getVocab().getNumberOfWords());
		assertEquals(hierarchy.getRoot().getTopicFrequency(), s.getSize());
		for (int n : new int[] { 1, 2, 5, hierarchy.getMaxTopics() }) {
			s.setNumberOfTopics(n);
			assertEquals(n, s.getNumberOfTopics());
			assertEquals(n, s.getTopicIds().length);
			Set<Set<Integer>> sets = new HashSet<Set<Integer>>();
			double total = 0;
			for (int t = 0; t < n; t++) {
				Set<Integer> ids = new HashSet<Integer>();
				TIntIterator it = s.getTopics()[t].iterator();
				while (it.hasNext()) {
					int type = it.next();
					ids.add(type);
					assertEquals(s.getTopicIds()[t], s.getTopicForWord(type));
				}
				ids.removeAll(recorder.zeroTypesAt.get(n));
				sets.add(ids);
				total += s.getTopicLogLikelihoods()[t];
			}
			assertEquals(recorder.topicsAt.get(n), sets);
			assertEquals(total, s.getTotalLogLikelhood(), 1e-9);
		}
		assertEquals(-1, s.getTopicForWord(99999));
	}

	public void testJsonRoundTrip() throws Exception {
		solveWithSenses();
		for (boolean pretty : new boolean[] { false, true }) {
			StringWriter sw = new StringWriter();
			new TopicHierarchyJsonWriter<String>(pretty).write(hierarchy, sw);
			String json = sw.toString();
			assertTrue(json.contains("\"format\":" + (pretty ? " " : "") + "\"topic-grouper-hierarchy\""));
			assertTrue(json.contains("\"sense\":" + (pretty ? " " : "") + "1"));
			TopicHierarchy<String> read = new TopicHierarchyJsonReader().read(new StringReader(json));
			assertSameHierarchy(hierarchy, read, false);
		}
		// Via a compressed file.
		File file = File.createTempFile("hierarchy", ".json.gz");
		try {
			new TopicHierarchyJsonWriter<String>().write(hierarchy, file);
			assertSameHierarchy(hierarchy, new TopicHierarchyJsonReader().read(file), false);
		} finally {
			file.delete();
		}
	}

	public void testObjectRoundTrip() throws Exception {
		solveWithSenses();
		File file = File.createTempFile("hierarchy", ".tg");
		try {
			new TopicHierarchyObjectWriter<String>().write(hierarchy, file);
			assertSameHierarchy(hierarchy, new TopicHierarchyObjectReader<String>().read(file), true);
		} finally {
			file.delete();
		}
	}

	private void assertSameHierarchy(TopicHierarchy<String> a, TopicHierarchy<String> b, boolean sameIds) {
		assertEquals(a.size(), b.size());
		assertEquals(a.getNumberOfWords(), b.getNumberOfWords());
		assertEquals(a.getNumberOfSenses(), b.getNumberOfSenses());
		assertEquals(a.getRoots().size(), b.getRoots().size());
		for (int i = 0; i < a.size(); i++) {
			MapNode<String> x = a.get(i);
			MapNode<String> y = b.get(i);
			assertEquals(x.getId(), y.getId());
			assertEquals(x.isLeaf(), y.isLeaf());
			assertEquals(x.isSenseLeaf(), y.isSenseLeaf());
			assertEquals(x.getTopicFrequency(), y.getTopicFrequency());
			assertEquals(x.getLikelihood(), y.getLikelihood(), 1e-12);
			assertEquals(x.getDeltaLikelihood(), y.getDeltaLikelihood(), 1e-12);
			assertEquals(x.getTotalLikelihood(), y.getTotalLikelihood(), 1e-12);
			assertEquals(x.isMarked(), y.isMarked());
			assertEquals(a.getTopicsAfter(i), b.getTopicsAfter(i));
			assertEquals(a.getTopicSize(x), b.getTopicSize(y));
			assertEquals(x.getParent() == null, y.getParent() == null);
			if (x.getParent() != null) {
				assertEquals(a.indexOf(x.getParent()), b.indexOf(y.getParent()));
			}
			assertEquals(leafLabels(a, x), leafLabels(b, y));
			List<String> topA = new ArrayList<String>();
			for (WordInfo<String> info : x.getTopTopicWordInfos()) {
				topA.add(info.getLabel() + "/" + info.getFrequency());
			}
			List<String> topB = new ArrayList<String>();
			for (WordInfo<String> info : y.getTopTopicWordInfos()) {
				topB.add(info.getLabel() + "/" + info.getFrequency());
			}
			assertEquals(topA, topB);
			if (x.isLeaf()) {
				assertEquals(x.getWordInfo().getDocuments(), y.getWordInfo().getDocuments());
				if (sameIds || !x.isSenseLeaf()) {
					assertEquals(x.getWordInfo().getWordId(), y.getWordInfo().getWordId());
				}
			}
		}
	}

	/** A hierarchy of a given depth (one topic absorbing one word at a time). */
	static TopicHierarchy<String> createChain(int words) {
		List<MapNode<String>> nodes = new ArrayList<MapNode<String>>();
		for (int i = 0; i < words; i++) {
			List<WordInfo<String>> list = new ArrayList<WordInfo<String>>();
			list.add(new WordInfo<String>(i, 10 + i, "w" + i));
			nodes.add(new MapNode<String>(-i, null, null, list, -1000, 0, 0, 10 + i));
		}
		MapNode<String> current = nodes.get(0);
		for (int i = 1; i < words; i++) {
			MapNode<String> leaf = nodes.get(i);
			List<WordInfo<String>> top = new ArrayList<WordInfo<String>>();
			top.add(leaf.getWordInfo());
			top.add(current.getTopTopicWordInfos().get(0));
			MapNode<String> parent = new MapNode<String>(words - i, current, leaf, top, -1000 - i, -10 - i, -1,
					current.getTopicFrequency() + leaf.getTopicFrequency());
			current.setParent(parent);
			leaf.setParent(parent);
			nodes.add(parent);
			current = parent;
		}
		return new TopicHierarchy<String>(nodes);
	}

	public void testDeepHierarchyDoesNotRecurse() throws Exception {
		TopicHierarchy<String> deep = createChain(20000);
		assertEquals(20000, deep.getTopicSize(deep.getRoot()));
		assertEquals(3, deep.getTopicsAt(3).size());
		StringWriter sw = new StringWriter();
		new TopicHierarchyJsonWriter<String>().write(deep, sw);
		TopicHierarchy<String> read = new TopicHierarchyJsonReader().read(new StringReader(sw.toString()));
		assertEquals(deep.size(), read.size());
		assertEquals(20000, read.getTopicSize(read.getRoot()));
		for (int i = 0; i < deep.size(); i++) {
			assertEquals(deep.get(i).getId(), read.get(i).getId());
		}
	}

	public void testReaderRejectsOtherJson() throws Exception {
		try {
			new TopicHierarchyJsonReader().read(new StringReader("{\"format\": \"something\", \"vocab\": [], \"roots\": []}"));
			fail();
		} catch (java.io.IOException e) {
			assertTrue(e.getMessage().contains("format"));
		}
		try {
			new TopicHierarchyJsonReader().read(new StringReader("{\"roots\": []}"));
			fail();
		} catch (java.io.IOException e) {
			// vocabulary must precede the roots
		}
	}
}
