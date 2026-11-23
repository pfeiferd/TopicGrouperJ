package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.USSupermarketParser;
import org.junit.Test;

public class USSupermarketParserTest {
	private static File sample() throws Exception {
		File dir = Files.createTempDirectory("uss").toFile();
		Files.write(new File(dir, "vocab.uss.csv").toPath(),
				("product_id,category,product_name\n" + "1,Baking Goods,\"Washington Berry Juice\"\n"
						+ "2,Dairy,\"Washington Mango Drink\"\n" + "3,Beverages,\"Big Time Cola\"\n")
						.getBytes(Charset.forName("UTF-8")));
		Files.write(new File(dir, "docword.uss.csv").toPath(),
				("customer_id,purchase_id,product_id,quantity\n" + "3,1,1,2.0\n" + "3,1,3,1.0\n"
						+ "3,2,1,1.0\n" + "7,1,2,4.0\n" + "7,1,3,3.0\n").getBytes(Charset.forName("UTF-8")));
		return dir;
	}

	@Test
	public void testPerPurchase() throws Exception {
		DocumentProvider<String> p = new USSupermarketParser(-1, true)
				.getCorpusDocumentProvider(sample(), "uss");
		assertEquals(3, p.getDocuments().size());
		assertEquals(3, p.getVocab().getNumberOfWords());
		int juice = p.getVocab().getIndex("1|Baking Goods|Washington Berry Juice");
		assertEquals(3, p.getWordFrequency(juice));
		assertEquals(2, p.getDocuments().get(0).getWordFrequency(juice));
		assertEquals(4, p.getWordFrequency(p.getVocab().getIndex("2|Dairy|Washington Mango Drink")));
		assertEquals(11, p.getSize());
	}

	@Test
	public void testPerCustomerAndMaxDocs() throws Exception {
		File dir = sample();
		DocumentProvider<String> p = new USSupermarketParser(-1, false)
				.getCorpusDocumentProvider(dir, "uss");
		assertEquals(2, p.getDocuments().size());
		assertEquals(3, p.getDocuments().get(0).getWordFrequency(
				p.getVocab().getIndex("1|Baking Goods|Washington Berry Juice")));

		p = new USSupermarketParser(1, false).getCorpusDocumentProvider(dir, "uss");
		assertEquals(1, p.getDocuments().size());
		assertEquals(4, p.getSize());
	}
}
