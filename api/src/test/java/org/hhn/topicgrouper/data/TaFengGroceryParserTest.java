package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.TaFengGroceryParser;
import org.junit.Test;

public class TaFengGroceryParserTest {
	private static File sample() throws Exception {
		File dir = Files.createTempDirectory("tafeng").toFile();
		File file = new File(dir, "D11");
		String csv = "Transaction date;Customer ID;Age;Residence area;Product subclass;Product ID;Amount;Asset;Sales price\n"
				+ "2000-11-01 00:00:00;00046855;D;E;110411;4710085120468;3;51;57\n"
				+ "2000-11-01 00:00:00;00046855;D;E;110411;4710085120475;1;30;33\n"
				+ "2000-11-01 00:00:00;00046855;D;E;130106;4710011401128;1;60;68\n"
				// same customer, other day
				+ "2000-11-02 00:00:00;00046855;D;E;110411;4710085120468;2;34;38\n"
				// unlikely amounts: dropped
				+ "2000-11-02 00:00:00;00046855;D;E;130106;4710011401128;99;60;68\n"
				+ "2000-11-02 00:00:00;00046855;D;E;130106;4710011401128;0;60;68\n"
				// other customer
				+ "2000-11-02 00:00:00;01234567;F;G;110411;4710085120468;1;17;19\n";
		Files.write(file.toPath(), csv.getBytes(Charset.forName("UTF-8")));
		return file;
	}

	@Test
	public void testPerPurchase() throws Exception {
		DocumentProvider<String> p = new TaFengGroceryParser(-1, true, false)
				.getCorpusDocumentProvider(sample());
		assertEquals(3, p.getDocuments().size());
		assertEquals(3, p.getVocab().getNumberOfWords());
		assertEquals(6, p.getWordFrequency(p.getVocab().getIndex("4710085120468")));
		assertEquals(3, p.getDocuments().get(0).getWordFrequency(p.getVocab().getIndex("4710085120468")));
		assertEquals(1, p.getWordFrequency(p.getVocab().getIndex("4710011401128")));
		assertEquals(8, p.getSize());
	}

	@Test
	public void testPerCustomerWithSubclass() throws Exception {
		DocumentProvider<String> p = new TaFengGroceryParser(-1, false, true)
				.getCorpusDocumentProvider(sample());
		assertEquals(2, p.getDocuments().size());
		assertEquals(2, p.getVocab().getNumberOfWords());
		assertEquals(7, p.getWordFrequency(p.getVocab().getIndex("110411")));
		assertEquals(6, p.getDocuments().get(0).getWordFrequency(p.getVocab().getIndex("110411")));
	}

	@Test
	public void testMaxDocs() throws Exception {
		DocumentProvider<String> p = new TaFengGroceryParser(1, true, false)
				.getCorpusDocumentProvider(sample());
		assertEquals(1, p.getDocuments().size());
		assertEquals(5, p.getSize());
	}
}
