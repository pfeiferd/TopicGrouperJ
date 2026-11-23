package org.hhn.topicgrouper.data;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

/**
 * Reads a US supermarket purchase data set (1997) that was provided as two
 * comma separated files in the style of the UCI "bag of words" corpora; the
 * origin of the data set is unknown. Both files have one header line.
 * <code>vocab.NAME.csv</code> lists the products (id, category, quoted name):
 *
 * <pre>
 * product_id,category,product_name
 * 1,Baking Goods,"Washington Berry Juice"
 * </pre>
 *
 * and <code>docword.NAME.csv</code> the purchased quantities, grouped by
 * customer (and purchase):
 *
 * <pre>
 * customer_id,purchase_id,product_id,quantity
 * 3,1,1,2.0
 * </pre>
 *
 * A document is a purchase (customer and purchase id) or a customer; the lines
 * of a document must be consecutive. The words are the products as
 * "<code>id|category|name</code>", each occurring as often as the (truncated)
 * quantity.
 */
public class USSupermarketParser {
	private final int maxDocs;
	private final boolean perPurchase;

	/**
	 * Creates the parser.
	 *
	 * @param maxDocs
	 *            maximum number of documents to read, negative for all
	 * @param perPurchase
	 *            if true a document is a purchase, otherwise a customer
	 */
	public USSupermarketParser(int maxDocs, boolean perPurchase) {
		this.maxDocs = maxDocs;
		this.perPurchase = perPurchase;
	}

	/**
	 * Reads the two files of a corpus from a directory.
	 *
	 * @param folder
	 *            directory containing the two files
	 * @param name
	 *            the corpus name, e.g. "uss" for <code>vocab.uss.csv</code>
	 *            and <code>docword.uss.csv</code>
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File folder,
			String name) {
		return getCorpusDocumentProvider(new File(folder, "vocab." + name
				+ ".csv"), new File(folder, "docword." + name + ".csv"));
	}

	/**
	 * Reads the product file and then the purchases file.
	 *
	 * @param vocabFile
	 *            the product file
	 * @param docwordFile
	 *            the purchases file
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File vocabFile,
			File docwordFile) {
		try {
			TIntObjectMap<String> productIdToWord = new TIntObjectHashMap<String>();
			BufferedReader reader = new BufferedReader(new FileReader(vocabFile));
			try {
				initVocab(reader, productIdToWord);
			} finally {
				reader.close();
			}
			reader = new BufferedReader(new FileReader(docwordFile));
			try {
				return createDocumentProvider(reader, productIdToWord);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Reads the product file (after the header line) and builds the word of
	 * every product.
	 *
	 * @param reader
	 *            the product file
	 * @param productIdToWord
	 *            receives the word per product id
	 * @throws IOException
	 *             if reading fails
	 */
	protected void initVocab(BufferedReader reader,
			TIntObjectMap<String> productIdToWord) throws IOException {
		// Skip header line
		reader.readLine();
		String s = reader.readLine();
		while (s != null) {
			String[] values = s.split(",");
			int productId = Integer.parseInt(values[0].trim());
			String h = values[2].trim();
			String productName = h.substring(1, h.length() - 1);

			productIdToWord.put(productId, productId + "|" + values[1].trim()
					+ "|" + productName);
			s = reader.readLine();
		}
	}

	/**
	 * Reads the purchases file (after the header line); a new document starts
	 * whenever the customer (and purchase) id changes.
	 *
	 * @param reader
	 *            the purchases file
	 * @param productIdToWord
	 *            the word per product id
	 * @return the documents
	 * @throws IOException
	 *             if reading fails
	 */
	protected DocumentProvider<String> createDocumentProvider(
			BufferedReader reader, TIntObjectMap<String> productIdToWord)
			throws IOException {
		// Skip header line
		reader.readLine();

		DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();

		DefaultDocumentProvider<String>.DefaultDocument d = null;

		int docCounter = 0;
		String oldDocId = null;
		String line = reader.readLine();
		while (line != null) {
			String[] values = line.split(",");
			String nextDocId = values[0].trim();
			if (perPurchase) {
				nextDocId += "_" + values[1].trim();
			}
			if (!nextDocId.equals(oldDocId)) {
				if (maxDocs >= 0 && docCounter == maxDocs) {
					break;
				}
				d = provider.newDocument();
				docCounter++;
			}
			int wordId = Integer.parseInt(values[2].trim());
			int fr = (int) Double.parseDouble(values[3].trim());
			String word = productIdToWord.get(wordId);
			if (word == null) {
				throw new IllegalStateException("unknown product id " + wordId);
			}
			d.addWord(word, fr);
			oldDocId = nextDocId;
			line = reader.readLine();
		}
		return provider;
	}
}
