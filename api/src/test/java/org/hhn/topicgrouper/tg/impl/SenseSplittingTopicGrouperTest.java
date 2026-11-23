package org.hhn.topicgrouper.tg.impl;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSenseSolution;
import org.hhn.topicgrouper.tg.TGSenseSolutionListener;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class SenseSplittingTopicGrouperTest extends TestCase {
	private static final int WORDS = 40;
	private static final int DRAWS = 20;

	/**
	 * Themes of the given number of words, documents of 20 draws, documents
	 * ordered by theme. If homonym is true the word "h" is emitted together with
	 * every draw of the first word of each theme, so it is a homonym tied to the
	 * words t0w0, t1w0, ... The homonym only gets a sense if it joins such a word
	 * while that word is still a single-word topic, which is the case for two
	 * themes of 40 words and for three themes of 80 words (with 60 documents
	 * each); with more themes the homonym is too frequent to be joined early.
	 */
	private static DefaultDocumentProvider<String> createCorpus(boolean homonym, int themes,
			int docsPerTheme, int wordsPerTheme) {
		Random random = new Random(7);
		DefaultDocumentProvider<String> p = new DefaultDocumentProvider<String>();
		for (int theme = 0; theme < themes; theme++) {
			for (int i = 0; i < docsPerTheme; i++) {
				DefaultDocumentProvider<String>.DefaultDocument d = p.newDocument();
				for (int j = 0; j < DRAWS; j++) {
					int w = random.nextInt(wordsPerTheme);
					d.addWord("t" + theme + "w" + w);
					if (homonym && w == 0) {
						d.addWord("h");
					}
				}
			}
		}
		return p;
	}

	private static class Recorder implements TGSenseSolutionListener<String> {
		final List<Double> likelihoods = new ArrayList<Double>();
		int splits;
		TGSolution<String> solution;

		public void beforeInitialization(int maxTopics, int documents) {
		}

		public void initalizing(double percentage) {
		}

		public void initialized(TGSolution<String> initialSolution) {
			solution = initialSolution;
		}

		public void updatedSolution(int newTopicIndex, int oldTopicIndex,
				double improvement, int t1Size, int t2Size, TGSolution<String> s) {
			likelihoods.add(s.getTotalLogLikelhood());
		}

		public void splitSense(int topicIndex, int wordIndex, int senseIndex,
				int documents, double improvement, TGSenseSolution<String> s) {
			splits++;
		}

		public void done() {
		}
	}

	/** Each theme's words end up in one topic, the themes in different ones. */
	private static void assertThemesSeparated(DefaultDocumentProvider<String> p, TGSolution<String> s,
			int themes, int wordsPerTheme) {
		Set<Integer> topicsOfThemes = new HashSet<Integer>();
		for (int theme = 0; theme < themes; theme++) {
			Set<Integer> topicsOfTheme = new HashSet<Integer>();
			for (int w = 0; w < wordsPerTheme; w++) {
				topicsOfTheme.add(s.getTopicForWord(p.getVocab().getIndex("t" + theme + "w" + w)));
			}
			assertEquals(1, topicsOfTheme.size());
			topicsOfThemes.addAll(topicsOfTheme);
		}
		assertEquals(themes, topicsOfThemes.size());
	}

	/** The occurrences of the word are kept over all of its types. */
	private static void assertOccurrencesKept(DefaultDocumentProvider<String> p, TGSenseSolution<String> s,
			String word) {
		int w = p.getVocab().getIndex(word);
		int sum = 0;
		for (int type = 0; type < s.getNumberOfWordTypes(); type++) {
			if (s.getOriginalWord(type) == w) {
				sum += s.getGlobalWordFrequency(type);
			}
		}
		assertEquals(p.getWordFrequency(w), sum);
	}

	public void testNoSplittingEqualsEHAC() {
		DefaultDocumentProvider<String> p = createCorpus(true, 2, 60, WORDS);
		Recorder plain = new Recorder();
		new EHACTopicGrouper<String>(1, p, 1).solve(plain);
		Recorder noSense = new Recorder();
		new SenseSplittingTopicGrouper<String>(1, p, 1, Double.POSITIVE_INFINITY).solve(noSense);
		assertEquals(0, noSense.splits);
		assertEquals(plain.likelihoods.size(), noSense.likelihoods.size());
		for (int i = 0; i < plain.likelihoods.size(); i++) {
			assertEquals(plain.likelihoods.get(i), noSense.likelihoods.get(i), 1e-6);
		}
	}

	public void testHomonymGetsTwoSenses() {
		DefaultDocumentProvider<String> p = createCorpus(true, 2, 60, WORDS);
		Recorder r = new Recorder();
		SenseSplittingTopicGrouper<String> tg = new SenseSplittingTopicGrouper<String>(1, p, 2);
		tg.solve(r);
		assertTrue("expected a split", r.splits >= 1);
		assertEquals(r.splits, tg.getNumberOfSplits());

		TGSenseSolution<String> s = (TGSenseSolution<String>) r.solution;
		assertEquals(2, s.getNumberOfTopics());
		assertThemesSeparated(p, s, 2, WORDS);
		int h = p.getVocab().getIndex("h");
		Set<Integer> topicsOfH = new HashSet<Integer>();
		for (int type = 0; type < s.getNumberOfWordTypes(); type++) {
			if (s.getOriginalWord(type) == h && s.getGlobalWordFrequency(type) > 0) {
				topicsOfH.add(s.getTopicForWord(type));
				int[] docs = s.getSenseDocuments(type);
				if (docs != null) {
					// All documents of a sense belong to one theme.
					for (int d : docs) {
						assertEquals(docs[0] / 60, d / 60);
					}
				}
			}
		}
		assertEquals("h must be in both topics", 2, topicsOfH.size());
		assertOccurrencesKept(p, s, "h");
	}

	/** @return the number of senses (without the remainder) of h; fails if one is not pure */
	private static int pureSensesOfH(DefaultDocumentProvider<String> p, TGSenseSolution<String> s,
			int docsPerTheme) {
		int h = p.getVocab().getIndex("h");
		int senses = 0;
		for (int type = 0; type < s.getNumberOfWordTypes(); type++) {
			int[] docs = s.getSenseDocuments(type);
			if (docs == null || docs.length == 0 || s.getOriginalWord(type) != h) {
				continue;
			}
			senses++;
			int theme = docs[0] / docsPerTheme;
			for (int d : docs) {
				assertEquals(theme, d / docsPerTheme);
			}
		}
		return senses;
	}

	/** The homonym of three themes gets a pure sense for two of them, the remainder is the third. */
	public void testThreeThemesPureSenses() {
		DefaultDocumentProvider<String> p = createCorpus(true, 3, 60, 2 * WORDS);
		Recorder r = new Recorder();
		new SenseSplittingTopicGrouper<String>(1, p, 3).solve(r);
		TGSenseSolution<String> s = (TGSenseSolution<String>) r.solution;
		assertThemesSeparated(p, s, 3, 2 * WORDS);
		assertEquals(2, pureSensesOfH(p, s, 60));
		assertOccurrencesKept(p, s, "h");
	}

	/**
	 * The topic model used for the perplexity maps the senses back to their word:
	 * every topic's phi sums to 1, the word has a positive probability in every
	 * topic holding one of its types, and summed over the topics (weighted by
	 * the topic frequencies) all occurrences of the word are accounted for.
	 */
	public void testPerplexityModelDesensesAcrossTopics() {
		DefaultDocumentProvider<String> p = createCorpus(true, 3, 60, 2 * WORDS);
		Recorder r = new Recorder();
		new SenseSplittingTopicGrouper<String>(1, p, 3).solve(r);
		TGSenseSolution<String> s = (TGSenseSolution<String>) r.solution;
		AbstractTopicModeler<String> m = new TGPerplexityCalculator<String>(false,
				new DefaultDocumentSplitter<String>(), 1).createTopicModeler(s);
		int[] ids = s.getTopicIds();
		int words = p.getVocab().getNumberOfWords();
		assertEquals(words, m.getNWords());
		for (int i = 0; i < m.getNTopics(); i++) {
			double sum = 0;
			for (int w = 0; w < words; w++) {
				sum += m.getPhi(i, w);
			}
			assertEquals(1.0, sum, 1e-9);
		}
		int h = p.getVocab().getIndex("h");
		int topicsOfH = 0;
		for (int w = 0; w < words; w++) {
			double occurrences = 0;
			for (int i = 0; i < m.getNTopics(); i++) {
				occurrences += m.getPhi(i, w) * s.getTopicFrequency(ids[i]);
				if (w == h && m.getPhi(i, w) > 0) {
					topicsOfH++;
				}
			}
			assertEquals(p.getWordFrequency(w), occurrences, 1e-6);
		}
		assertEquals(3, topicsOfH);
	}

	/**
	 * Without a homonym, words of a theme may get senses on the way, but all
	 * senses end in the topic of their word and become one with it again.
	 */
	public void testNoHomonymNoSenseSurvives() {
		DefaultDocumentProvider<String> p = createCorpus(false, 2, 60, WORDS);
		Recorder r = new Recorder();
		SenseSplittingTopicGrouper<String> tg = new SenseSplittingTopicGrouper<String>(1, p, 2);
		tg.solve(r);
		TGSenseSolution<String> s = (TGSenseSolution<String>) r.solution;
		assertThemesSeparated(p, s, 2, WORDS);
		for (int type = p.getVocab().getNumberOfWords(); type < s.getNumberOfWordTypes(); type++) {
			assertEquals(0, s.getGlobalWordFrequency(type));
		}
		assertEquals(tg.getNumberOfSplits(), tg.getNumberOfMerges());
	}

	public void testLiftValidation() {
		DefaultDocumentProvider<String> p = createCorpus(true, 2, 10, WORDS);
		assertEquals(SenseSplittingTopicGrouper.DEFAULT_MIN_SENSE_LIFT,
				new SenseSplittingTopicGrouper<String>(1, p, 1).getMinSenseLift());
		new SenseSplittingTopicGrouper<String>(1, p, 1, 0);
		try {
			new SenseSplittingTopicGrouper<String>(1, p, 1, -1);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
		try {
			new SenseSplittingTopicGrouper<String>(1, p, 1, Double.NaN);
			fail();
		} catch (IllegalArgumentException e) {
			// expected
		}
	}

	/** Recomputes the log likelihood of the final solution from scratch. */
	public void testBookkeepingMatchesDirectLikelihood() {
		DefaultDocumentProvider<String> p = createCorpus(true, 2, 60, WORDS);
		Recorder r = new Recorder();
		new SenseSplittingTopicGrouper<String>(1, p, 2).solve(r);
		TGSenseSolution<String> s = (TGSenseSolution<String>) r.solution;
		assertTrue(r.splits >= 1);

		int words = p.getVocab().getNumberOfWords();
		int types = s.getNumberOfWordTypes();
		List<Document<String>> docs = p.getDocuments();
		// count[type][doc]
		int[][] count = new int[types][docs.size()];
		for (int d = 0; d < docs.size(); d++) {
			TIntIterator it = docs.get(d).getWordIndices().iterator();
			while (it.hasNext()) {
				int w = it.next();
				count[w][d] = docs.get(d).getWordFrequency(w);
			}
		}
		for (int type = words; type < types; type++) {
			for (int d : s.getSenseDocuments(type)) {
				count[type][d] = count[s.getOriginalWord(type)][d];
				count[s.getOriginalWord(type)][d] = 0;
			}
		}
		Map<Integer, double[]> perTopicDoc = new HashMap<Integer, double[]>();
		Map<Integer, Double> topicTotal = new HashMap<Integer, Double>();
		for (int type = 0; type < types; type++) {
			int t = s.getTopicForWord(type);
			if (!perTopicDoc.containsKey(t)) {
				perTopicDoc.put(t, new double[docs.size()]);
				topicTotal.put(t, 0d);
			}
			for (int d = 0; d < docs.size(); d++) {
				perTopicDoc.get(t)[d] += count[type][d];
				topicTotal.put(t, topicTotal.get(t) + count[type][d]);
			}
		}
		double ll = 0;
		int live = 0;
		for (int type = 0; type < types; type++) {
			int t = s.getTopicForWord(type);
			int fr = 0;
			for (int d = 0; d < docs.size(); d++) {
				fr += count[type][d];
				if (count[type][d] > 0) {
					double pwt = s.getGlobalWordFrequency(type) / topicTotal.get(t);
					double ptd = perTopicDoc.get(t)[d] / docs.get(d).getSize();
					ll += count[type][d] * Math.log(pwt * ptd);
				}
			}
			assertEquals(fr, s.getGlobalWordFrequency(type));
			if (fr > 0) {
				live++;
			}
		}
		assertEquals(ll, s.getTotalLogLikelhood(), 1e-6);
		// The topics contain all live types exactly once (merged senses are gone).
		int members = 0;
		for (TIntCollection c : s.getTopics()) {
			if (c != null) {
				members += c.size();
			}
		}
		assertEquals(live, members);
	}
}
