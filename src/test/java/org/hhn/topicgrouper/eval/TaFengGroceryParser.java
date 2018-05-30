package org.hhn.topicgrouper.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.util.StatsReport;

public class TaFengGroceryParser {
	private final DocumentProvider<String> documentProvider;

	public TaFengGroceryParser(File folder, String name, int maxDocs,
			boolean perPurchase, boolean useProductSubclass) {
		try {

			FileReader docReader = new FileReader(new File(folder, name));
			documentProvider = createDocumentProvider(new BufferedReader(
					docReader), perPurchase, useProductSubclass);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public DocumentProvider<String> getDocumentProvider() {
		return documentProvider;
	}

	protected DocumentProvider<String> createDocumentProvider(
			BufferedReader reader, boolean perPurchase,
			boolean useProductSubclass) throws IOException {
		// Get over first 3 lines.
		reader.readLine();

		DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
		Map<String, DefaultDocumentProvider<String>.DefaultDocument> nameToDoc = new HashMap<String, DefaultDocumentProvider<String>.DefaultDocument>();

		DefaultDocumentProvider<String>.DefaultDocument d = null;

		String line = reader.readLine();
		int maxFr = 0;
		while (line != null) {
			String[] values = line.split(";");
			String date = values[0];
			String customerId = values[1].trim();
			if (perPurchase) {
				customerId += customerId + "_" + date;
			}
			String productId = values[useProductSubclass ? 4 : 5].trim();

			int fr = Integer.parseInt(values[6]);
			if (fr > maxFr) {
				maxFr = fr;
			}
			// Omit unlikely quantities
			if (fr > 0 && fr <= 50) {
				d = nameToDoc.get(customerId);
				if (d == null) {
					d = provider.newDocument();
					nameToDoc.put(customerId, d);
				}
				d.addWord(productId, fr);
			}
			line = reader.readLine();
		}
		System.out.println(maxFr);
		return provider;
	}

	public static void main(String[] args) throws IOException {
		TaFengGroceryParser uciParser = new TaFengGroceryParser(new File(
				"./src/test/resources/TaFengGrocery"), "D01_D02_D11_D12", -1,
				true, false);
		DocumentProvider<String> provider = uciParser.getDocumentProvider();
		System.out.println(provider.getDocuments().size());
		System.out.println(provider.getVocab().getNumberOfWords());

		StatsReport.report(provider, System.out);

	}
}
