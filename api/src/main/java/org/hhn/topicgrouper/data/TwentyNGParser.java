package org.hhn.topicgrouper.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;

import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Reads the "20 Newsgroups" corpus (one directory per group, one file per message).
 * The group name is the label. Words are lower cased, optionally stop-word filtered and
 * stemmed, and only purely alphabetic words with more than two characters are kept.
 */
public class TwentyNGParser {
	private final TokenizerFactory factory;
	private final PrintStream pw;

	/**
	 * Parser with stemming.
	 *
	 * @param pw unused (may be null)
	 * @param removeStopWords whether English stop words are removed
	 */
	public TwentyNGParser(PrintStream pw, boolean removeStopWords) {
		this(pw, removeStopWords, true);
	}

	/**
	 * Parser with optional stemming.
	 *
	 * @param pw unused (may be null)
	 * @param removeStopWords whether English stop words are removed
	 * @param stemming whether words are stemmed
	 */
	public TwentyNGParser(PrintStream pw, boolean removeStopWords,
			boolean stemming) {
		TokenizerFactory baseFactory = new LowerCaseTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		if (removeStopWords) {
			baseFactory = new EnglishStopTokenizerFactory(baseFactory);
		}
		if (stemming) {
			baseFactory = new PorterStemmerTokenizerFactory(baseFactory);
		}
		factory = baseFactory;
		this.pw = pw;
	}

	/**
	 * Tokenizes a text and adds its proper words to a document.
	 *
	 * @param entry
	 *            the document the words are added to
	 * @param cs
	 *            the text
	 * @param start
	 *            index of the first character of the text
	 * @param length
	 *            number of characters of the text
	 */
	protected void extendDocument(
			DefaultDocumentProvider<String>.DefaultDocument entry, char[] cs,
			int start, int length) {
		Tokenizer t = factory.tokenizer(cs, start, length);
		Iterator<String> it = t.iterator();
		while (it.hasNext()) {
			String word = it.next();
			if (isProperWord(word)) {
				entry.addWord(word);				
			}
// Original code:
//			if (word.length() > 2) {
//				char c = word.charAt(0);
//				if (Character.isLetter(c)) {
//					entry.addWord(word);
//					// System.out.println(word);
//				}
//			}
		}
	}
	
	/**
	 * The word filter of the text parsers.
	 *
	 * @param word a token
	 * @return whether the word has more than two characters and consists of letters only.
	 */
	public static boolean isProperWord(String word) {
		if (word.length() <= 2) {
			return false;
		}
		for (int i = 0; i < word.length(); i++) {
			char c = word.charAt(i);
			if (!Character.isAlphabetic(c) || Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Reads the messages of all groups.
	 *
	 * @param folder the directory with one sub directory per group
	 * @param partRatio the share (0..1) of the messages of each group that is read
	 * @return the labeled documents
	 */
	public LabelingDocumentProvider<String, String> getCorpusDocumentProvider(File folder,
			double partRatio) {
		try {
			DefaultLabelingDocumentProvider<String, String> documentProvider = new DefaultLabelingDocumentProvider<String, String>();
			char[] buffer = new char[2048];

			File[] files = folder.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					String label = files[i].getName();
					File[] news = files[i].listFiles();
					for (int j = 0; j < news.length * partRatio; j++) {
						int offset = 0;
						DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = documentProvider
								.newLabeledDocument(label);
						BufferedReader reader = new BufferedReader(
								new FileReader(news[j]));
						int read = reader.read(buffer, offset, buffer.length
								- offset);
						while (read != -1) {
							int pos = read - 1  + offset;
							for (; pos >= 0
									&& !Character.isWhitespace(buffer[pos]); pos--) {
							}
							extendDocument(d, buffer, 0, pos);
							read += offset;
							for (offset = 0; pos < read; offset++, pos++) {
								buffer[offset] = buffer[pos];
							}
							read = reader.read(buffer, offset, buffer.length
									- offset);
						}
						reader.close();
					}
				}
			}
			return documentProvider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
