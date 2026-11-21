package org.hhn.topicgrouper.evaluation;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import junit.framework.TestCase;

public class PerplexityExperimentTest extends TestCase {
	public void testTwcSmoke() throws Exception {
		File out = File.createTempFile("perplexity", ".csv");
		out.deleteOnExit();
		PerplexityExperiment.run("twc", Datasets.twc(400).split, Settings.quick(), out, true);
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
