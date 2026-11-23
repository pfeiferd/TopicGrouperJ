package org.hhn.topicgrouper.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

/**
 * Reads the anonymous Belgian retail store market basket data of Tom Brijs
 * (<code>retail.dat</code> of the FIMI repository, 88162 baskets). Every line
 * is a basket: the ids of the purchased items separated by spaces, e.g.
 *
 * <pre>
 * 30 31 32
 * 33 34 35
 * 36 37 38 39 40 41 42 43 44 45 46
 * </pre>
 *
 * A document is a basket, the words are the item ids (as strings), each
 * occurring once. Blank lines are skipped.
 */
public class BelgianRetailStoreParser {
	/** Creates the parser; it has no settings. */
	public BelgianRetailStoreParser() {
	}

	/**
	 * Reads the baskets of the file.
	 *
	 * @param file
	 *            the <code>retail.dat</code> file
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				String line = reader.readLine();
				while (line != null) {
					if (!line.trim().isEmpty()) {
						String[] values = line.split(" ");
						DefaultDocumentProvider<String>.DefaultDocument d = provider
								.newDocument();
						for (int i = 0; i < values.length; i++) {
							String word = values[i].trim();
							if (!word.isEmpty()) {
								d.addWord(word);
							}
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
}
