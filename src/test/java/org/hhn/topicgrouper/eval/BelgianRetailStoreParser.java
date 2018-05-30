package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

public class BelgianRetailStoreParser {
	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
			LineNumberReader lineNumberReader = new LineNumberReader(
					new FileReader(file));
			String line = lineNumberReader.readLine();
			while (line != null) {
				String[] values = line.split(" ");
				if (values.length > 0) {
					DefaultDocumentProvider<String>.DefaultDocument d = provider
							.newDocument();
					for (int i = 0; i < values.length; i++) {
						// TODO: Better work with Integer instead?
						String word = values[i].trim();
						if (!word.isEmpty()) {
							d.addWord(word);
						}
						// int id = Integer.valueOf(values[i]);
						// d.addWord(id);
					}
				}
				line = lineNumberReader.readLine();
			}

			lineNumberReader.close();

			return provider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws IOException {
		BelgianRetailStoreParser parser = new BelgianRetailStoreParser();
		DocumentProvider<String> entryProvider = parser
				.getCorpusDocumentProvider(new File(
						"src/test/resources/BelgianRetailStore/retail.dat"));
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getVocab().getNumberOfWords());
	}
}
