package org.hhn.topicgrouper.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

/**
 * Reads the Ta-Feng grocery shopping data set (transactions of a Taiwanese
 * supermarket, Nov 2000 - Feb 2001, four monthly files <code>D11, D12, D01,
 * D02</code>; the original ACM RecSys wiki download is gone, the data is
 * available from mirrors, e.g. on Kaggle). The expected format is a
 * <code>;</code>-separated file with one header line and the columns
 *
 * <pre>
 * Transaction date;Customer ID;Age;Residence area;Product subclass;Product ID;Amount;Asset;Sales price
 * 2000-11-01 00:00:00;00046855;D;E;110411;4710085120468;3;51;57
 * </pre>
 *
 * (the monthly files may simply be concatenated, with one header line). A
 * document is either a purchase (customer and date) or a customer (all his or
 * her purchases); the words are the product ids or, alternatively, the
 * product subclasses, each occurring as often as the purchased amount. Lines
 * with an amount of 0 or more than 50 are dropped as unlikely.
 */
public class TaFengGroceryParser {
	private final int maxDocs;
	private final boolean perPurchase;
	private final boolean useProductSubclass;

	/**
	 * Creates the parser.
	 *
	 * @param maxDocs
	 *            maximum number of documents, negative for all (once the
	 *            maximum is reached no further documents are created, but
	 *            lines belonging to already created documents are still
	 *            added)
	 * @param perPurchase
	 *            if true a document is a purchase (customer and date),
	 *            otherwise a customer
	 * @param useProductSubclass
	 *            if true the words are the product subclasses, otherwise the
	 *            product ids
	 */
	public TaFengGroceryParser(int maxDocs, boolean perPurchase,
			boolean useProductSubclass) {
		this.maxDocs = maxDocs;
		this.perPurchase = perPurchase;
		this.useProductSubclass = useProductSubclass;
	}

	/**
	 * Reads the transactions of the file.
	 *
	 * @param file
	 *            the <code>;</code>-separated file
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				return createDocumentProvider(reader);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parses the transaction lines (after the header line) into documents.
	 *
	 * @param reader
	 *            the file content
	 * @return the documents
	 * @throws IOException
	 *             if reading fails
	 */
	protected DocumentProvider<String> createDocumentProvider(
			BufferedReader reader) throws IOException {
		// Skip the header line.
		reader.readLine();

		DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
		Map<String, DefaultDocumentProvider<String>.DefaultDocument> nameToDoc = new HashMap<String, DefaultDocumentProvider<String>.DefaultDocument>();

		String line = reader.readLine();
		while (line != null) {
			String[] values = line.split(";");
			String date = values[0].trim();
			String key = values[1].trim();
			if (perPurchase) {
				key = key + "_" + date;
			}
			String productId = values[useProductSubclass ? 4 : 5].trim();

			int fr = Integer.parseInt(values[6].trim());
			// Omit unlikely quantities
			if (fr > 0 && fr <= 50) {
				DefaultDocumentProvider<String>.DefaultDocument d = nameToDoc
						.get(key);
				if (d == null) {
					if (maxDocs >= 0 && nameToDoc.size() >= maxDocs) {
						line = reader.readLine();
						continue;
					}
					d = provider.newDocument();
					nameToDoc.put(key, d);
				}
				d.addWord(productId, fr);
			}
			line = reader.readLine();
		}
		return provider;
	}
}
