package org.hhn.topicgrouper.eval;

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
import com.aliasi.tokenizer.PunctuationStopListTokenizer;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class TwentyNGParser {
	private final TokenizerFactory factory;
	private final PrintStream pw;

	public TwentyNGParser(PrintStream pw, boolean removeStopWords) {
		this(pw, removeStopWords, true);
	}

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

	protected void extendDocument(
			DefaultDocumentProvider<String>.DefaultDocument entry, char[] cs,
			int start, int length) {
		Tokenizer t = new PunctuationStopListTokenizer(factory.tokenizer(cs,
				start, length));
		Iterator<String> it = t.iterator();
		while (it.hasNext()) {
			String word = it.next();
			if (word.length() > 1) {
				char c = word.charAt(0);
				if (Character.isLetter(c)) {
					// System.out.println(word);
					entry.addWord(word);
				}
			}
		}
	}

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

	public static void main(String[] args) {
		DocumentProvider<String> entryProvider = new TwentyNGParser(System.out,
				true).getCorpusDocumentProvider(new File(
				"src/test/resources/20news-18828"), 0.1d);
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getVocab().getNumberOfWords());
	}

}
