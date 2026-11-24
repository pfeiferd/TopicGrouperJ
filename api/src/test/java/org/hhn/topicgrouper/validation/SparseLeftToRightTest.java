package org.hhn.topicgrouper.validation;

import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;

/**
 * Restricting the samplers to the topics with non-zero probability must not change
 * the samples: with every word in exactly two of five topics both variants draw
 * the same random numbers and must produce the same estimate.
 */
public class SparseLeftToRightTest extends TestCase {
	private DefaultDocumentProvider<String> train;
	private DefaultDocumentProvider<String> test;

	@Override
	protected void setUp() {
		train = new DefaultDocumentProvider<String>();
		DefaultDocumentProvider<String>.DefaultDocument d = train.newDocument();
		d.addWord("a", 3);
		d.addWord("b", 2);
		d.addWord("c", 1);
		d.addWord("e", 1);
		test = new DefaultDocumentProvider<String>((DefaultVocab<String>) train.getVocab());
		d = test.newDocument();
		d.addWord("a", 4);
		d.addWord("b", 2);
		d.addWord("c", 3);
		d = test.newDocument();
		d.addWord("c", 5);
		d.addWord("e", 2);
		d.addWord("a", 1);
	}

	private static final int TOPICS = 5;

	/** Five topics; word w has probability only in topics w % 5 and (w + 1) % 5. */
	private AbstractTopicModeler<String> twoTopicsPerWord() {
		return new AbstractTopicModeler<String>(train.getVocab(), TOPICS) {
			@Override
			protected void initPhi() {
				double[] sum = new double[TOPICS];
				for (int w = 0; w < nWords; w++) {
					phi[w % TOPICS][w] = 1 + w;
					phi[(w + 1) % TOPICS][w] = 2 + w;
					sum[w % TOPICS] += 1 + w;
					sum[(w + 1) % TOPICS] += 2 + w;
				}
				for (int t = 0; t < TOPICS; t++) {
					for (int w = 0; w < nWords; w++) {
						phi[t][w] = sum[t] == 0 ? 0 : phi[t][w] / sum[t];
					}
				}
			}

			@Override
			protected void initTopicProb() {
				double[] p = { 0.3, 0.25, 0.2, 0.15, 0.1 };
				System.arraycopy(p, 0, topicProb, 0, TOPICS);
			}

			@Override
			public double getWordProb(int wordIndex) {
				double p = 0;
				for (int t = 0; t < TOPICS; t++) {
					p += phi[t][wordIndex] * topicProb[t];
				}
				return p;
			}

			@Override
			public double getAlpha(int i) {
				return 2 * topicProb[i];
			}

			@Override
			public double getAlphaConc() {
				return 2;
			}

			@Override
			public void setAlphaConc(double alphaConc) {
			}
		};
	}

	private double perplexity(final boolean sparse, final boolean unbiased, int particles) {
		PerplexityCalculatorLeftToRight<String> calc = new PerplexityCalculatorLeftToRight<String>(new Random(11),
				false, new DefaultDocumentSplitter<String>(), particles) {
			@Override
			protected boolean useSparseTopics() {
				return sparse;
			}

			@Override
			protected boolean useUnbiased() {
				return unbiased;
			}
		};
		calc.setTopicModeler(twoTopicsPerWord());
		return calc.computePerplexity(test);
	}

	public void testCandidateTopics() {
		int[][] c = PerplexityCalculatorLeftToRight.candidateTopics(twoTopicsPerWord());
		assertEquals(train.getVocab().getNumberOfWords(), c.length);
		for (int w = 0; w < c.length; w++) {
			assertEquals(2, c[w].length);
			assertTrue(c[w][0] < c[w][1]);
			assertTrue(c[w][0] == w % TOPICS || c[w][0] == (w + 1) % TOPICS);
			assertTrue(c[w][1] == w % TOPICS || c[w][1] == (w + 1) % TOPICS);
		}
		// Dense models get no candidate lists.
		assertNull(PerplexityCalculatorLeftToRight.candidateTopics(new UnigramModeler<String>(train, 3)));
	}

	public void testSparseAndDenseSamplersAgree() {
		for (int particles = 1; particles <= 3; particles++) {
			assertEquals(perplexity(false, true, particles), perplexity(true, true, particles), 1e-12);
			assertEquals(perplexity(false, false, particles), perplexity(true, false, particles), 1e-12);
		}
		assertTrue(perplexity(true, true, 2) > 1);
	}
}
