package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.util.StatsReport;

public class OnlineRetailParser {
	public DocumentProvider<String> getCorpusDocumentProvider(File file,
			boolean perBasketDocument, int maxQuantity, boolean ignoreQuantity) {
		try {
			Map<String, DefaultDocumentProvider<String>.DefaultDocument> idToBasket = new HashMap<String, DefaultDocumentProvider<String>.DefaultDocument>();

			Map<String, String> stockCodeToDescription = new HashMap<String, String>();

			Set<String> badInvoices = new HashSet<String>();
			int badInvoicesWithLargQs = 0;

			// Read file once to collect suspicious invoices. --> Doesn't make any difference!!
			LineNumberReader lineNumberReader = new LineNumberReader(
					new FileReader(file));
			lineNumberReader.readLine(); // Ignore first line.
			String line = lineNumberReader.readLine();
			while (line != null) {
				String[] values = line.split(";");
				String invoiceNo = values[0].trim();
				int quantity = Integer.valueOf(values[3]);
				if (invoiceNo.charAt(0) == 'C') {
					badInvoices.add(invoiceNo.substring(1));
				} else if (quantity < 1) {
					badInvoices.add(invoiceNo);
//				} else if (quantity > maxQuantity) {
//					badInvoices.add(invoiceNo);
//					badInvoicesWithLargQs++;
				}
				line = lineNumberReader.readLine();
			}
			lineNumberReader.close();
			System.out.println("Excluded erroneous invoices: " + badInvoices.size());
			System.out.println("Excluded invoices with too large quantities: " + badInvoicesWithLargQs);

			int reducedQuantity = 0;
			int allQuantites = 0;
			DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
			lineNumberReader = new LineNumberReader(new FileReader(file));
			lineNumberReader.readLine();
			line = lineNumberReader.readLine();
			while (line != null) {
				String[] values = line.split(";");
				String invoiceNo = values[0].trim();
				if (invoiceNo.charAt(0) != 'C'
						&& !badInvoices.contains(invoiceNo)) {
					int quantity = Integer.valueOf(values[3]);
					String stockCode = values[1].trim();
					// Shorten by max quantity cause quantities are highly
					// skewed.
					// Most are below 100 but some are way over 10000. That
					// would mess up document size and topic modeling
					// performance.
					if (quantity > maxQuantity) {
						reducedQuantity++;
					}
					// Ignore negative and zero quantities...
					// plus a bit of data cleaning
					else if (quantity > 0 && !stockCode.isEmpty()
							&& Character.isDigit(stockCode.charAt(0))) {
						String description = values[2].trim();
						String customerId = values[6].trim();

						String sd = stockCodeToDescription.get(stockCode);
						if (sd == null) {
							sd = createCombinedStockCodeDescription(stockCode,
									description);
							stockCodeToDescription.put(stockCode, sd);
						}

						DefaultDocumentProvider<String>.DefaultDocument d = idToBasket
								.get(perBasketDocument ? invoiceNo : customerId);
						if (d == null) {
							d = provider.newDocument();
							idToBasket.put(perBasketDocument ? invoiceNo
									: customerId, d);
						}
						d.addWord(sd, ignoreQuantity ? 1 : quantity);
					}					
					allQuantites++;
				}
//				else {
//					if (badInvoices.contains(invoiceNo)) {
//						System.out.println(invoiceNo);
//					}
//				}
				line = lineNumberReader.readLine();
			}
			lineNumberReader.close();
			
			System.out.println(reducedQuantity);
			System.out.println(allQuantites);
			System.out.println(((double)reducedQuantity) / allQuantites);
			System.out.println(allQuantites- reducedQuantity);
			

			return provider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected String createCombinedStockCodeDescription(String stockCode,
			String description) {
		return stockCode + ": " + description;
	}

	public static void main(String[] args) throws IOException {
		OnlineRetailParser parser = new OnlineRetailParser();
		DocumentProvider<String> entryProvider = parser
				.getCorpusDocumentProvider(new File(
						"src/test/resources/OnlineRetail/Online Retail.csv"),
						true, 25, false);
		StatsReport.report(entryProvider, System.out);
	}
}
