package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.BelgianRetailStoreParser;
import org.junit.Test;

public class BelgianRetailStoreParserTest {
	@Test
	public void testBaskets() throws Exception {
		File dir = Files.createTempDirectory("retail").toFile();
		File file = new File(dir, "retail.dat");
		Files.write(file.toPath(), "30 31 32 \n33 34 35 \n\n36 37 38 39 40 41 42 43 44 45 46 \n38 39 47 48 \n"
				.getBytes(Charset.forName("US-ASCII")));

		DocumentProvider<String> p = new BelgianRetailStoreParser().getCorpusDocumentProvider(file);
		assertEquals(4, p.getDocuments().size());
		assertEquals(19, p.getVocab().getNumberOfWords());
		assertEquals(21, p.getSize());
		assertEquals(2, p.getWordFrequency(p.getVocab().getIndex("38")));
		assertEquals(1, p.getWordFrequency(p.getVocab().getIndex("30")));
		assertEquals(3, p.getDocuments().get(0).getSize());
		assertEquals(11, p.getDocuments().get(2).getSize());
		assertEquals(-1, p.getVocab().getIndex(""));
	}
}
