package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import junit.framework.TestCase;

/**
 * Tests that the figures are drawn from the CSV files into <code>figs</code>
 * under the expected names.
 */
public class FiguresTest extends TestCase {
	private File dir;

	@Override
	protected void setUp() throws Exception {
		dir = File.createTempFile("results", "");
		dir.delete();
		dir.mkdir();
	}

	@Override
	protected void tearDown() throws Exception {
		File[] figs = new File(dir, Figures.DIRECTORY).listFiles();
		if (figs != null) {
			for (File f : figs) {
				f.delete();
			}
		}
		new File(dir, Figures.DIRECTORY).delete();
		File[] files = dir.listFiles();
		if (files != null) {
			for (File f : files) {
				f.delete();
			}
		}
		dir.delete();
	}

	private String read(File f) throws IOException {
		Reader in = new InputStreamReader(new FileInputStream(f), Charset.forName("UTF-8"));
		try {
			StringBuilder sb = new StringBuilder();
			char[] buf = new char[4096];
			int n;
			while ((n = in.read(buf)) >= 0) {
				sb.append(buf, 0, n);
			}
			return sb.toString();
		} finally {
			in.close();
		}
	}

	public void testPerplexityAndSeriesFigures() throws Exception {
		Csv csv = new Csv(new File(dir, "perplexity-nips.csv"), "method", "topics", "perplexity", "alphaConc");
		csv.row("unigram", 1, 2000.0, Double.NaN);
		csv.row("tg", 10, 1800.5, 3.0);
		csv.row("tg", 20, 1700.5, 3.5);
		csv.row("lda", 10, 1750.0, Double.NaN);
		csv.row("lda", 20, 1650.0, Double.NaN);
		csv.close();
		Csv alpha = new Csv(new File(dir, "twc-alpha.csv"), "alpha1", "method", "perplexity", "perplexityStdDev",
				"errorRate", "errorRateStdDev", "tries");
		alpha.row(1.0, "tg", 150.0, 0.0, 0.1, 0.0, 1);
		alpha.row(2.0, "tg", 150.0, 0.0, 0.1, 0.0, 1);
		alpha.row(1.0, "lda", 140.0, 2.0, 0.3, 0.05, 3);
		alpha.row(2.0, "lda", 145.0, 2.0, 0.2, 0.05, 3);
		alpha.close();
		Csv topics = new Csv(new File(dir, "topics-nips.csv"), "topic", "size", "rank", "word", "frequency");
		topics.row(1, 2, 1, "a", 5);
		topics.close();

		Figures figures = new Figures(dir);
		assertEquals(2, figures.plotAll());
		File figs = new File(dir, "figs");
		String perplexity = read(new File(figs, "perplexity-nips.svg"));
		assertTrue(perplexity.contains(">Topic Grouper</text>"));
		assertTrue(perplexity.contains(">LDA</text>"));
		assertTrue(perplexity.contains(">unigram model</text>"));
		assertTrue(perplexity.contains(">hold-out perplexity</text>"));
		String error = read(new File(figs, "twc-alpha-error.svg"));
		assertTrue(error.contains(">error rate</text>"));
		assertTrue(error.contains("Dirichlet prior"));
		assertTrue(new File(figs, "twc-alpha-perplexity.svg").isFile());
		assertFalse(new File(figs, "topics-nips.svg").exists());
	}

	public void testClassificationPerformanceAndHomonymFigures() throws Exception {
		Csv c = new Csv(new File(dir, "classification-reuters.csv"), "method", "topics", "microAvg", "macroAvg",
				"lambda");
		c.row("tg-nb", 10, 0.7, 0.5, 1.0);
		c.row("tg-nb", 100, 0.8, 0.6, 1.0);
		c.row("vocab-ig", 10, 0.6, 0.4, Double.NaN);
		c.close();
		Csv p = new Csv(new File(dir, "performance-twc.csv"), "solver", "docs", "avgSeconds", "stdDevSeconds",
				"avgWords");
		p.row("ehac", 200, 1.0, 0.1, 400);
		p.row("ehac-mt", 200, 0.3, 0.05, 400);
		p.close();
		Csv h = new Csv(new File(dir, "homonyms.csv"), "homonyms", "method", "perplexity", "errorRate", "splits");
		h.row(0, "tg", 150.0, 0.03, 0);
		h.row(20, "tg", 170.0, 0.17, 0);
		h.row(0, "tg-sense", 150.0, 0.03, 200);
		h.row(20, "tg-sense", 168.0, 0.04, 210);
		h.close();
		Figures figures = new Figures(dir);
		figures.plot(new File(dir, "classification-reuters.csv"));
		figures.plot(new File(dir, "performance-twc.csv"));
		figures.plot(new File(dir, "homonyms.csv"));
		figures.plot(new File(dir, "something-else.txt"));
		File figs = new File(dir, "figs");
		assertTrue(read(new File(figs, "classification-reuters-micro.svg")).contains(">TG topics, naive Bayes</text>"));
		assertTrue(read(new File(figs, "classification-reuters-macro.svg")).contains("macro average"));
		assertTrue(read(new File(figs, "performance-twc.svg")).contains(">EHAC, all processors</text>"));
		assertTrue(read(new File(figs, "homonyms-error.svg")).contains(">Topic Grouper with senses</text>"));
		assertTrue(new File(figs, "homonyms-perplexity.svg").isFile());
	}
}
