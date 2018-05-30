package org.hhn.topicgrouper.eval;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;

public class USSupermarketParser {
	private final TIntObjectMap<String> wordIndexToStem;
	private DocumentProvider<String> documentProvider;

	public USSupermarketParser(File folder, String name,
			int maxDocs, boolean perPurchase) {
		try {
			wordIndexToStem = new TIntObjectHashMap<String>();
			FileReader vocabReader = new FileReader(new File(folder, "vocab."
					+ name + ".csv"));
			initVocab(new BufferedReader(vocabReader));

			FileReader docReader = new FileReader(new File(folder, "docword."
					+ name + ".csv"));
			documentProvider = createDocumentProvider(new BufferedReader(
					docReader), maxDocs, perPurchase);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	public DocumentProvider<String> getDocumentProvider() {
		return documentProvider;
	}

	protected void initVocab(BufferedReader reader) throws IOException {
		// Skip first line
		reader.readLine();
		String s = reader.readLine();
		while (s != null) {
			String[] values = s.split(",");
			int productId = Integer.parseInt(values[0]);
			String h = values[2].trim();
			String productName = h.substring(1, h.length() - 1);

			wordIndexToStem.put(productId, productId + "|" + values[1].trim()
					+ "|" + productName);
			s = reader.readLine();
		}
	}

	protected DocumentProvider<String> createDocumentProvider(
			BufferedReader reader, int maxDocs, boolean perPurchase)
			throws IOException {
		// Get over first 3 lines.
		reader.readLine();

		DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();

		DefaultDocumentProvider<String>.DefaultDocument d = null;

		int docCounter = 0;
		String oldDocId = null;
		String line = reader.readLine();
		while (line != null) {
			String[] values = line.split(",");
			String nextDocId = values[0];
			if (perPurchase) {
				nextDocId += "_" + values[1];
			}
			if (!nextDocId.equals(oldDocId)) {
				if (maxDocs >= 0 && docCounter == maxDocs) {
					break;
				}
				d = provider.newDocument();
				docCounter++;
			}
			int wordId = Integer.parseInt(values[2]);
			int fr = (int) Double.parseDouble(values[3]);
			String word = wordIndexToStem.get(wordId);
			d.addWord(word, fr);
			oldDocId = nextDocId;
			line = reader.readLine();
		}
		return provider;
	}

	public static void main(String[] args) throws IOException {
		USSupermarketParser uciParser = new USSupermarketParser(new File(
				"./src/test/resources/USSupermarket97"), "uss", -1, true);
		DocumentProvider<String> provider = uciParser.getDocumentProvider();
		System.out.println(provider.getDocuments().size());
		System.out.println(provider.getVocab().getNumberOfWords());

	}
}
