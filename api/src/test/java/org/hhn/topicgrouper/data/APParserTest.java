package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.APParser;
import org.junit.Test;

public class APParserTest {
	private static String doc(String no, String text) {
		return "<DOC>\n<DOCNO> " + no + " </DOCNO>\n<TEXT>\n" + text
				+ "\n</TEXT>\n</DOC>\n";
	}

	private static File sample() throws Exception {
		File dir = Files.createTempDirectory("ap").toFile();
		File file = new File(dir, "ap.txt");
		String content = doc("AP881218-0003",
				" The students were studying & the teachers were teaching. ")
				+ doc("AP880224-0195", " Oil prices rose 10 percent, oil traders said. ");
		Files.write(file.toPath(), content.getBytes(Charset.forName("US-ASCII")));
		return file;
	}

	@Test
	public void testStopWordsAndStemming() throws Exception {
		DocumentProvider<String> p = new APParser(true, true)
				.getCorpusDocumentProvider(sample());
		assertEquals(2, p.getDocuments().size());
		// stemmed, stop words removed, "10" dropped (does not start with a letter)
		assertTrue(p.getVocab().getIndex("student") >= 0);
		assertTrue(p.getVocab().getIndex("studi") >= 0);
		assertEquals(-1, p.getVocab().getIndex("the"));
		assertEquals(-1, p.getVocab().getIndex("10"));
		assertEquals(-1, p.getVocab().getIndex("&"));
		assertEquals(2, p.getWordFrequency(p.getVocab().getIndex("oil")));
		Document<String> second = p.getDocuments().get(1);
		assertEquals(2, second.getWordFrequency(p.getVocab().getIndex("oil")));
		assertEquals(0, second.getWordFrequency(p.getVocab().getIndex("student")));
	}

	@Test
	public void testNoStopWordsNoStemming() throws Exception {
		DocumentProvider<String> p = new APParser(false, false)
				.getCorpusDocumentProvider(sample());
		assertEquals(2, p.getDocuments().size());
		assertTrue(p.getVocab().getIndex("the") >= 0);
		assertTrue(p.getVocab().getIndex("students") >= 0);
		assertEquals(-1, p.getVocab().getIndex("student"));
		assertEquals(2, p.getWordFrequency(p.getVocab().getIndex("the")));
	}
}
