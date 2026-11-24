package org.hhn.topicgrouper.tg.validation;

import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;
import org.hhn.topicgrouper.doc.impl.FiftyFiftyDocumentSplitter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.tg.impl.SenseSplittingTopicGrouper;

/**
 * The closed form must give the numbers of the left-to-right estimator, whose
 * sampling is deterministic for Topic Grouper solutions.
 */
public class TGExactPerplexityCalculatorTest extends TestCase {
	private DefaultDocumentProvider<String> train;
	private DefaultDocumentProvider<String> test;

	@Override
	protected void setUp() {
		train = new DefaultDocumentProvider<String>();
		String[][] docs = { { "a", "a", "b", "b", "c" }, { "a", "b", "b", "c", "c", "c" },
				{ "x", "x", "y", "y", "z" }, { "x", "y", "y", "z", "z", "z", "z" },
				{ "a", "b", "x", "y", "c" }, { "a", "a", "a", "y", "z" } };
		for (String[] words : docs) {
			DefaultDocumentProvider<String>.DefaultDocument d = train.newDocument();
			for (String w : words) {
				d.addWord(w);
			}
		}
		test = new DefaultDocumentProvider<String>((DefaultVocab<String>) train.getVocab());
		DefaultDocumentProvider<String>.DefaultDocument d = test.newDocument();
		d.addWord("a", 3);
		d.addWord("b", 1);
		d.addWord("y", 2);
		d.addWord("c", 1);
		d = test.newDocument();
		d.addWord("x", 2);
		d.addWord("z", 3);
		d.addWord("a", 1);
		d = test.newDocument();
		d.addWord("b", 4);
	}

	/** Runs the solver and keeps the solution at the given number of topics. */
	@SuppressWarnings("unchecked")
	private static TGSolution<String> solve(TGSolver<String> solver, final int topics) {
		final TGSolution<String>[] result = new TGSolution[1];
		solver.solve(new TGSolutionListener<String>() {
			public void beforeInitialization(int maxTopics, int documents) {
			}

			public void initalizing(double percentage) {
			}

			public void initialized(TGSolution<String> initialSolution) {
				if (initialSolution.getNumberOfTopics() == topics) {
					result[0] = initialSolution;
				}
			}

			public void updatedSolution(int newTopicIndex, int oldTopicIndex, double improvement,
					int t1Size, int t2Size, TGSolution<String> solution) {
				if (solution.getNumberOfTopics() == topics) {
					result[0] = solution;
				}
			}

			public void done() {
			}
		});
		assertNotNull(result[0]);
		return result[0];
	}

	private double exact(TGSolution<String> s, boolean bow, DocumentSplitter<String> splitter,
			double alphaConc, DocumentProvider<String> docs) {
		TGExactPerplexityCalculator<String> calc = new TGExactPerplexityCalculator<String>(bow, splitter,
				alphaConc);
		calc.init();
		calc.setSolution(s);
		return calc.computePerplexity(docs);
	}

	private double leftToRight(TGSolution<String> s, boolean bow, DocumentSplitter<String> splitter,
			double alphaConc, final int particles, DocumentProvider<String> docs) {
		TGLRPerplexityCalculator<String> calc = new TGLRPerplexityCalculator<String>(new Random(7), bow,
				splitter, alphaConc) {
			@Override
			protected int initParticles() {
				return particles;
			}
		};
		calc.init();
		calc.setSolution(s);
		return calc.computePerplexity(docs);
	}

	public void testEqualsLeftToRightEstimate() {
		for (int topics = 1; topics <= 4; topics++) {
			// A fresh solver per topic number: the solutions are live views of the solver.
			TGSolution<String> s = solve(new EHACTopicGrouper<String>(1, train, topics), topics);
			for (double alphaConc : new double[] { 0.5, 3, 50 }) {
				double e = exact(s, false, new DefaultDocumentSplitter<String>(), alphaConc, test);
				assertTrue(e > 1);
				assertEquals(e, leftToRight(s, false, new DefaultDocumentSplitter<String>(), alphaConc, 1, test),
						1e-9);
				assertEquals(e, leftToRight(s, false, new DefaultDocumentSplitter<String>(), alphaConc, 3, test),
						1e-9);
			}
		}
	}

	public void testDocumentCompletionAndBowFactor() {
		TGSolution<String> s = solve(new EHACTopicGrouper<String>(1, train, 2), 2);
		// Document completion: the reference half conditions the topic proportions.
		double e = exact(s, false, new FiftyFiftyDocumentSplitter<String>(new Random(3)), 2, test);
		double lr = leftToRight(s, false, new FiftyFiftyDocumentSplitter<String>(new Random(3)), 2, 2, test);
		assertEquals(e, lr, 1e-9);
		// The same words as reference and test document.
		e = exact(s, false, new DefaultDocumentSplitter<String>(false), 2, test);
		lr = leftToRight(s, false, new DefaultDocumentSplitter<String>(false), 2, 1, test);
		assertEquals(e, lr, 1e-9);
		// Bag-of-words factor.
		e = exact(s, true, new DefaultDocumentSplitter<String>(), 2, test);
		lr = leftToRight(s, true, new DefaultDocumentSplitter<String>(), 2, 1, test);
		assertEquals(e, lr, 1e-9);
		assertTrue(e < exact(s, false, new DefaultDocumentSplitter<String>(), 2, test));
	}

	public void testOneTopicIsUnigramModel() {
		TGSolution<String> s = solve(new EHACTopicGrouper<String>(1, train, 1), 1);
		double logP = 0;
		int n = 0;
		for (int w = 0; w < train.getVocab().getNumberOfWords(); w++) {
			double pw = (double) train.getWordFrequency(w) / train.getSize();
			for (int i = 0; i < test.getDocuments().size(); i++) {
				int f = test.getDocuments().get(i).getWordFrequency(w);
				logP += f * Math.log(pw);
				n += f;
			}
		}
		assertEquals(Math.exp(-logP / n), exact(s, false, new DefaultDocumentSplitter<String>(), 5, test), 1e-9);
	}

	public void testAlphaOptimizationUsesTheClosedForm() {
		TGSolution<String> s = solve(new EHACTopicGrouper<String>(1, train, 2), 2);
		TGExactPerplexityCalculator<String> calc = new TGExactPerplexityCalculator<String>();
		calc.init();
		calc.setSolution(s);
		calc.optimizeAlphaConc(0.1, 100, test, 6);
		double opt = calc.getAlphaConc();
		assertTrue(opt >= 0.1 && opt <= 100);
		double atOpt = calc.computePerplexity(test);
		assertTrue(atOpt <= exact(s, false, new DefaultDocumentSplitter<String>(), 0.1, test) + 1e-9);
		assertTrue(atOpt <= exact(s, false, new DefaultDocumentSplitter<String>(), 100, test) + 1e-9);
	}

	public void testRefusesSenseSolutions() {
		TGSolution<String> s = solve(new SenseSplittingTopicGrouper<String>(1, train, 2), 2);
		TGExactPerplexityCalculator<String> calc = new TGExactPerplexityCalculator<String>();
		calc.init();
		try {
			calc.setSolution(s);
			fail("sense solution accepted");
		} catch (IllegalArgumentException expected) {
		}
	}
}
