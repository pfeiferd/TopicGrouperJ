package org.hhn.topicgrouper.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

/**
 * Reads the "Online Retail" data set of the UCI Machine Learning Repository
 * (transactions of a UK online retailer, Dec 2010 - Dec 2011). UCI ships the
 * data as an Excel file (<code>Online Retail.xlsx</code>); it must be exported
 * as a <code>;</code>-separated CSV file with a header line and the columns
 *
 * <pre>
 * InvoiceNo;StockCode;Description;Quantity;InvoiceDate;UnitPrice;CustomerID;Country
 * 536365;85123A;WHITE HANGING HEART T-LIGHT HOLDER;6;01.12.2010 08:26;2,55;17850;United Kingdom
 * </pre>
 *
 * A document is either an invoice (basket) or a customer (all of his or her
 * invoices); the words are the products as "<code>StockCode: Description</code>"
 * (the first description seen for a stock code is used throughout) and a word
 * occurs as often as the purchased quantity (or once, if quantities are
 * ignored).
 * <p>
 * Data cleaning: cancellations (invoice numbers starting with <code>C</code>)
 * are dropped together with the invoices they cancel (the same number without
 * the <code>C</code>), invoices with a line of quantity &lt; 1 are dropped
 * entirely, lines whose quantity exceeds the maximum quantity are dropped (the
 * quantities are highly skewed, some are way over 10000, which would mess up
 * the document sizes), as are lines whose stock code does not start with a
 * digit (postage, bank charges, manual entries etc.).
 */
public class OnlineRetailParser {
	private final boolean perBasketDocument;
	private final int maxQuantity;
	private final boolean ignoreQuantity;

	/**
	 * Creates the parser.
	 *
	 * @param perBasketDocument
	 *            if true a document is an invoice, otherwise a customer
	 * @param maxQuantity
	 *            lines with a larger quantity are dropped
	 * @param ignoreQuantity
	 *            if true every line counts as one occurrence of the product,
	 *            otherwise the quantity is the number of occurrences
	 */
	public OnlineRetailParser(boolean perBasketDocument, int maxQuantity,
			boolean ignoreQuantity) {
		this.perBasketDocument = perBasketDocument;
		this.maxQuantity = maxQuantity;
		this.ignoreQuantity = ignoreQuantity;
	}

	/**
	 * Reads the file twice: first to collect the cancelled and otherwise bad
	 * invoices, then to build the documents.
	 *
	 * @param file
	 *            the <code>;</code>-separated CSV file
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			Map<String, DefaultDocumentProvider<String>.DefaultDocument> idToBasket = new HashMap<String, DefaultDocumentProvider<String>.DefaultDocument>();
			Map<String, String> stockCodeToDescription = new HashMap<String, String>();

			// Read file once to collect suspicious invoices.
			Set<String> badInvoices = new HashSet<String>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				reader.readLine(); // Ignore header line.
				String line = reader.readLine();
				while (line != null) {
					String[] values = line.split(";", -1);
					String invoiceNo = values[0].trim();
					int quantity = Integer.valueOf(values[3].trim());
					if (invoiceNo.charAt(0) == 'C') {
						badInvoices.add(invoiceNo.substring(1));
					} else if (quantity < 1) {
						badInvoices.add(invoiceNo);
					}
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}

			DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
			reader = new BufferedReader(new FileReader(file));
			try {
				reader.readLine();
				String line = reader.readLine();
				while (line != null) {
					String[] values = line.split(";", -1);
					String invoiceNo = values[0].trim();
					if (invoiceNo.charAt(0) != 'C'
							&& !badInvoices.contains(invoiceNo)) {
						int quantity = Integer.valueOf(values[3].trim());
						String stockCode = values[1].trim();
						// Ignore too large, negative and zero quantities...
						// plus a bit of data cleaning
						if (quantity <= maxQuantity && quantity > 0
								&& !stockCode.isEmpty()
								&& Character.isDigit(stockCode.charAt(0))) {
							String description = values[2].trim();
							String customerId = values[6].trim();

							String sd = stockCodeToDescription.get(stockCode);
							if (sd == null) {
								sd = createCombinedStockCodeDescription(
										stockCode, description);
								stockCodeToDescription.put(stockCode, sd);
							}

							String key = perBasketDocument ? invoiceNo
									: customerId;
							DefaultDocumentProvider<String>.DefaultDocument d = idToBasket
									.get(key);
							if (d == null) {
								d = provider.newDocument();
								idToBasket.put(key, d);
							}
							d.addWord(sd, ignoreQuantity ? 1 : quantity);
						}
					}
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
			return provider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Builds the word that represents a product.
	 *
	 * @param stockCode
	 *            the stock code of the product
	 * @param description
	 *            its description
	 * @return the word for a product: "<code>stockCode: description</code>"
	 */
	protected String createCombinedStockCodeDescription(String stockCode,
			String description) {
		return stockCode + ": " + description;
	}
}
