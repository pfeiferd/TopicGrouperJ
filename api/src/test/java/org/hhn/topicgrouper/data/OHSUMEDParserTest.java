package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.LabeledDocument;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.data.OHSUMEDParser;
import org.junit.Test;

public class OHSUMEDParserTest {
	@Test
	public void testSparseAndDense() throws Exception {
		File dir = Files.createTempDirectory("ohsumed").toFile();
		File file = new File(dir, "ohscal.wc.arff");
		String arff = "% comment\n@relation ohscal.wc\n\n" + "@attribute patient numeric\n"
				+ "@attribute blood numeric\n" + "@ATTRIBUTE 'heart rate' numeric\n" + "@attribute cell real\n"
				+ "@attribute class {Antibodies,Carcinoma,Cardiovascular}\n" + "@data\n"
				+ "{0 3, 1 1, 4 Carcinoma}\n" + "{1 2, 2 5, 4 Cardiovascular}\n" + "{3 1}\n" // omitted class = first
				+ "% another comment\n" + "0,0,1,4.0,Carcinoma\n" + "\n";
		Files.write(file.toPath(), arff.getBytes(Charset.forName("UTF-8")));

		LabelingDocumentProvider<String, String> p = new OHSUMEDParser().getCorpusDocumentProvider(file);
		assertEquals(4, p.getDocuments().size());
		assertEquals(3, p.getAllLabels().size());
		assertEquals(2, p.getDocumentsWithLabel("Carcinoma").size());
		assertEquals(1, p.getDocumentsWithLabel("Antibodies").size());
		assertEquals(4, p.getVocab().getNumberOfWords());
		assertEquals(-1, p.getVocab().getIndex("class"));
		assertEquals(3, p.getWordFrequency(p.getVocab().getIndex("patient")));
		assertEquals(3, p.getWordFrequency(p.getVocab().getIndex("blood")));
		assertEquals(6, p.getWordFrequency(p.getVocab().getIndex("heart rate")));
		assertEquals(5, p.getWordFrequency(p.getVocab().getIndex("cell")));
		LabeledDocument<String, String> last = p.getLabeledDocuments().get(3);
		assertEquals("Carcinoma", last.getLabel());
		assertEquals(2, last.getWords()); // zeros of the dense row are skipped
		assertEquals(4, last.getWordFrequency(p.getVocab().getIndex("cell")));
		assertTrue(p.getLabeledDocuments().get(2).getLabel().equals("Antibodies"));
	}
}
