package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.OnlineRetailParser;
import org.junit.Test;

public class OnlineRetailParserTest {
	private static File sample() throws Exception {
		File dir = Files.createTempDirectory("onlineretail").toFile();
		File file = new File(dir, "Online Retail.csv");
		String csv = "InvoiceNo;StockCode;Description;Quantity;InvoiceDate;UnitPrice;CustomerID;Country\n"
				+ "536365;85123A;WHITE HANGING HEART T-LIGHT HOLDER;6;01.12.2010 08:26;2,55;17850;United Kingdom\n"
				+ "536365;71053;WHITE METAL LANTERN;6;01.12.2010 08:26;3,39;17850;United Kingdom\n"
				+ "536365;POST;POSTAGE;1;01.12.2010 08:26;18;17850;United Kingdom\n"
				// same customer, second invoice, one huge quantity line (dropped) and one normal line
				+ "536366;85123A;WHITE HANGING HEART T-LIGHT HOLDER;500;01.12.2010 08:28;2,55;17850;United Kingdom\n"
				+ "536366;22633;HAND WARMER UNION JACK;2;01.12.2010 08:28;1,85;17850;United Kingdom\n"
				// cancelled invoice and its cancellation
				+ "536367;84879;ASSORTED COLOUR BIRD ORNAMENT;32;01.12.2010 08:34;1,69;13047;United Kingdom\n"
				+ "C536367;84879;ASSORTED COLOUR BIRD ORNAMENT;-32;01.12.2010 08:34;1,69;13047;United Kingdom\n"
				// invoice with a zero quantity line: dropped entirely
				+ "536368;22960;JAM MAKING SET WITH JARS;0;01.12.2010 08:34;4,25;13047;United Kingdom\n"
				+ "536368;22913;RED COAT RACK PARIS FASHION;3;01.12.2010 08:34;4,95;13047;United Kingdom\n"
				// other customer
				+ "536369;21756;BATH BUILDING BLOCK WORD;3;01.12.2010 08:35;5,95;12583;France\n";
		Files.write(file.toPath(), csv.getBytes(Charset.forName("UTF-8")));
		return file;
	}

	@Test
	public void testPerInvoiceWithQuantities() throws Exception {
		DocumentProvider<String> p = new OnlineRetailParser(true, 25, false)
				.getCorpusDocumentProvider(sample());
		assertEquals(3, p.getDocuments().size()); // 536365, 536366, 536369
		String heart = "85123A: WHITE HANGING HEART T-LIGHT HOLDER";
		assertEquals(6, p.getWordFrequency(p.getVocab().getIndex(heart)));
		assertEquals(6, p.getDocuments().get(0).getWordFrequency(p.getVocab().getIndex(heart)));
		assertEquals(2, p.getWordFrequency(p.getVocab().getIndex("22633: HAND WARMER UNION JACK")));
		assertEquals(-1, p.getVocab().getIndex("POST: POSTAGE"));
		assertEquals(-1, p.getVocab().getIndex("84879: ASSORTED COLOUR BIRD ORNAMENT"));
		assertEquals(-1, p.getVocab().getIndex("22913: RED COAT RACK PARIS FASHION"));
		assertEquals(4, p.getVocab().getNumberOfWords());
		assertEquals(17, p.getSize());
	}

	@Test
	public void testPerCustomerIgnoringQuantities() throws Exception {
		DocumentProvider<String> p = new OnlineRetailParser(false, 1000, true)
				.getCorpusDocumentProvider(sample());
		assertEquals(2, p.getDocuments().size()); // 17850, 12583
		String heart = "85123A: WHITE HANGING HEART T-LIGHT HOLDER";
		assertEquals(2, p.getWordFrequency(p.getVocab().getIndex(heart)));
		assertTrue(p.getDocuments().get(0).getSize() == 4);
		assertEquals(5, p.getSize());
	}
}
