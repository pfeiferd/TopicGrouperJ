package org.hhn.topicgrouper.eval;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

import org.hhn.topicgrouper.eval.featurereduction.ClassificationExperiment;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.LabelingHoldOutSplitter;

public class ClassificationExperimentTest extends TestCase {
	public void testSeparableClasses() throws Exception {
		DefaultLabelingDocumentProvider<String, String> provider = new DefaultLabelingDocumentProvider<String, String>();
		Random random = new Random(1);
		for (int i = 0; i < 80; i++) {
			String label = i % 2 == 0 ? "a" : "b";
			DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = provider
					.newLabeledDocument(label);
			for (int j = 0; j < 20; j++) {
				d.addWord(label + random.nextInt(15));
				d.addWord("common" + random.nextInt(5));
			}
		}
		LabelingHoldOutSplitter<String, String> splitter = new LabelingHoldOutSplitter<String, String>(
				new Random(42), provider, 0.25, 1, -1);
		Split<LabelingDocumentProvider<String, String>> split = new Split<LabelingDocumentProvider<String, String>>(
				splitter.getRest(), splitter.getHoldOut());

		File dir = Files.createTempDirectory("classification").toFile();
		new ClassificationExperiment(Settings.quick(), dir).run("test", split, true);
		File out = new File(dir, "classification-test.csv");

		List<String> lines = Files.readAllLines(out.toPath());
		assertEquals("method;topics;microAvg;macroAvg;lambda", lines.get(0));
		boolean tgNb = false, vocab = false, lda = false;
		for (String line : lines.subList(1, lines.size())) {
			String[] c = line.split(";");
			assertEquals(5, c.length);
			double micro = Double.parseDouble(c[2]);
			assertTrue(micro >= 0 && micro <= 1);
			if (c[0].equals("tg-nb") && c[1].equals("2")) {
				tgNb = true;
				assertTrue("two topics should separate the classes: " + micro, micro > 0.9);
			}
			vocab |= c[0].equals("vocab-ig");
			lda |= c[0].equals("lda-nb");
		}
		assertTrue(tgNb && vocab && lda);
	}
}
