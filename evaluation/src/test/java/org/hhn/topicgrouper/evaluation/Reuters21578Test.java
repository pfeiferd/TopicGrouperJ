package org.hhn.topicgrouper.evaluation;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import junit.framework.TestCase;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.evaluation.data.Reuters21578;

public class Reuters21578Test extends TestCase {
	private static String doc(String split, String topics, String body) {
		return "<REUTERS TOPICS=\"YES\" LEWISSPLIT=\"" + split + "\" NEWID=\"1\">\n<DATE>1-JAN-1987</DATE>\n<TOPICS>"
				+ topics + "</TOPICS>\n<TEXT>\n<TITLE>Oil prices</TITLE>\n<BODY>" + body
				+ " Reuter &#3;</BODY>\n</TEXT>\n</REUTERS>\n";
	}

	public void testModApteSplit() throws Exception {
		File dir = Files.createTempDirectory("reuters").toFile();
		String sgm = "<!DOCTYPE lewis SYSTEM \"lewis.dtd\">\n"
				+ doc("TRAIN", "<D>crude</D><D>nat-gas</D><D>ship</D>", "Oil and gas prices rose &lt;today&gt;.")
				+ doc("TRAIN", "", "No topic, dropped.")
				+ doc("TEST", "<D>earn</D>", "Earnings of the companies fell.")
				+ doc("NOT-USED", "<D>earn</D>", "Not used.");
		Files.write(new File(dir, "reut2-000.sgm").toPath(), sgm.getBytes(Charset.forName("ISO-8859-1")));

		Reuters21578 reuters = new Reuters21578(true);
		LabelingDocumentProvider<String, String> train = reuters.getCorpusDocumentProvider(dir, true, false);
		assertEquals(2, train.getDocuments().size()); // once per top-10 topic (crude, ship)
		assertTrue(train.getAllLabels().contains("crude"));
		assertTrue(train.getVocab().getIndex("oil") >= 0);
		assertEquals(-1, train.getVocab().getIndex("&lt;"));
		assertEquals(1, reuters.getCorpusDocumentProvider(dir, false, true).getDocuments().size());
		assertEquals(3, reuters.getCorpusDocumentProvider(dir, true, true).getDocuments().size());
	}
}
