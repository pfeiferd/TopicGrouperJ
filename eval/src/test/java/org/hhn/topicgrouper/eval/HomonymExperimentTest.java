package org.hhn.topicgrouper.eval;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import junit.framework.TestCase;

import org.hhn.topicgrouper.eval.homonyms.HomonymExperiment;

public class HomonymExperimentTest extends TestCase {
	public void testSmoke() throws Exception {
		File dir = Files.createTempDirectory("homonyms").toFile();
		new HomonymExperiment(new Datasets(dir), Settings.quick(), dir).run();
		List<String> lines = Files.readAllLines(new File(dir, "homonyms.csv").toPath());
		assertEquals("homonyms;method;perplexity;errorRate;splits", lines.get(0));
		boolean sense = false;
		for (String line : lines.subList(1, lines.size())) {
			String[] c = line.split(";");
			assertEquals(5, c.length);
			assertTrue(Double.parseDouble(c[2]) > 1);
			sense |= c[1].equals("tg-sense");
			if (c[1].equals("tg")) {
				assertEquals("0", c[4]);
			}
		}
		assertTrue(sense);
	}
}
