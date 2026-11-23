package org.hhn.topicgrouper.eval;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import junit.framework.TestCase;

import org.hhn.topicgrouper.eval.ecir2019.PerplexityExperiment;

public class PerplexityExperimentTest extends TestCase {
	public void testTwcSmoke() throws Exception {
		File dir = Files.createTempDirectory("perplexity").toFile();
		new PerplexityExperiment(Settings.quick(), dir).run(
				new PerplexityExperiment.Corpus("twc", new Datasets(dir).twc(400).split).withPlsa());
		File out = new File(dir, "perplexity-twc.csv");
		List<String> lines = Files.readAllLines(out.toPath());
		assertEquals("method;topics;perplexity;alphaConc", lines.get(0));
		double unigram = Double.NaN, tg10 = Double.NaN;
		for (String line : lines) {
			String[] c = line.split(";");
			if (c[0].equals("unigram")) {
				unigram = Double.parseDouble(c[2]);
			}
			if (c[0].equals("tg") && c[1].equals("10")) {
				tg10 = Double.parseDouble(c[2]);
			}
		}
		assertTrue(unigram > 1);
		// Topics must help compared to the unigram model on topical data.
		assertTrue(tg10 < unigram);
	}
}
