package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.APLargeParser;
import org.junit.Test;

public class APLargeParserTest {
	private static String doc(String no, String head, String text) {
		return "<DOC>\n<DOCNO> " + no + " </DOCNO>\n<FILEID>AP-NR-02-12-88 2344EST</FILEID>\n"
				+ "<FIRST>r i AM-Vietnam-Amnesty     02-12 0397</FIRST>\n<HEAD>" + head
				+ "</HEAD>\n<BYLINE>By JOHN DOE</BYLINE>\n<TEXT>\n" + text + "\n</TEXT>\n</DOC>\n";
	}

	private static File sample() throws Exception {
		File dir = Files.createTempDirectory("aplarge").toFile();
		Files.write(new File(dir, "ap880212").toPath(),
				(doc("AP880212-0001", "Officials Released", "   The officials were released & freed, officials said.")
						+ doc("AP880212-0002", "Oil Prices", "   Oil prices rose 10 percent &lt; expected, traders said."))
						.getBytes(Charset.forName("ISO-8859-1")));
		Files.write(new File(dir, "ap880213").toPath(),
				doc("AP880213-0001", "Café Prices", "   Café prices rose.")
						.getBytes(Charset.forName("ISO-8859-1")));
		Files.write(new File(dir, "readme").toPath(), "not an ap file".getBytes(Charset.forName("ISO-8859-1")));
		return dir;
	}

	@Test
	public void testAllDocuments() throws Exception {
		DocumentProvider<String> p = new APLargeParser(null, true)
				.getCorpusDocumentProvider(sample(), Integer.MAX_VALUE);
		assertEquals(3, p.getDocuments().size());
		// HEAD and TEXT are both used: "offici" (stemmed) occurs 3 times in the first document
		int offici = p.getVocab().getIndex("offici");
		assertTrue(offici >= 0);
		Document<String> first = p.getDocuments().get(0);
		assertEquals(3, first.getWordFrequency(offici));
		assertEquals(4, p.getWordFrequency(p.getVocab().getIndex("price")));
		assertEquals(-1, p.getVocab().getIndex("the"));
		assertEquals(-1, p.getVocab().getIndex("10"));
		assertEquals(-1, p.getVocab().getIndex("john")); // BYLINE is not used
		assertEquals(-1, p.getVocab().getIndex("lt"));
		// ISO-8859-1: the e acute survives decoding
		boolean cafe = false;
		for (int i = 0; i < p.getVocab().getNumberOfWords(); i++) {
			cafe |= p.getVocab().getWord(i).startsWith("café");
		}
		assertTrue(cafe);
	}

	@Test
	public void testMaxDocuments() throws Exception {
		DocumentProvider<String> p = new APLargeParser(null, true, false)
				.getCorpusDocumentProvider(sample(), 1);
		assertEquals(1, p.getDocuments().size());
		assertTrue(p.getVocab().getIndex("officials") >= 0);
		assertEquals(-1, p.getVocab().getIndex("oil"));
	}
}
