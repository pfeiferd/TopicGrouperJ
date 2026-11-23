package org.hhn.topicgrouper.data;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultVocab;

import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.RegExTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Reads corpora in the UCI "bag of words" format (<code>vocab.NAME.txt</code> and
 * <code>docword.NAME.txt</code>), optionally merging words with the same Porter stem.
 */
public class UCIParser {
	private final DefaultVocab<String> orgVocab;
	private final TIntObjectMap<String> wordIndexToStem;
	private final DefaultVocab<String> stemmingVocab;
	private DocumentProvider<String> documentProvider;

	/**
	 * Reads the two files of a corpus.
	 *
	 * @param stemming whether words are stemmed (and words with equal stems merged)
	 * @param folder directory containing the two files
	 * @param name the corpus name, e.g. "nips"
	 * @param maxDocs maximum number of documents to read, negative for all
	 * @throws IOException if a file cannot be read
	 */
	public UCIParser(boolean stemming, File folder, String name, int maxDocs)
			throws IOException {

		orgVocab = new DefaultVocab<String>();
		if (stemming) {
			wordIndexToStem = new TIntObjectHashMap<String>();
			stemmingVocab = new DefaultVocab<String>();
		} else {
			wordIndexToStem = null;
			stemmingVocab = null;
		}

		FileReader vocabReader = new FileReader(new File(folder, "vocab."
				+ name + ".txt"));
		initVocab(new BufferedReader(vocabReader));

		FileReader docReader = new FileReader(new File(folder, "docword."
				+ name + ".txt"));
		documentProvider = createDocumentProvider(
				new BufferedReader(docReader), maxDocs);
	}

	/**
	 * Returns the documents read by the constructor.
	 *
	 * @return the parsed documents
	 */
	public DocumentProvider<String> getDocumentProvider() {
		return documentProvider;
	}

	/**
	 * Reads the vocabulary file (one word per line, the line number starting at
	 * 1 is the word id) and, with stemming, the stems.
	 *
	 * @param reader
	 *            the vocabulary file
	 * @throws IOException
	 *             if reading fails
	 */
	protected void initVocab(BufferedReader reader) throws IOException {
		int wordIndex = 1;
		String word = nextWord(reader);
		while (word != null && !word.isEmpty()) {
//			System.out.println(wordIndex + " " + word);
			orgVocab.addEntry(word);
			if (stemmingVocab != null) {
				String stem = wordIndexToStem.get(wordIndex);
				if (stem == null) {
					stem = stem(word);
					wordIndexToStem.put(wordIndex, stem);
					stemmingVocab.addEntry(stem);
				}
			}
			word = nextWord(reader);
			wordIndex++;
		}
	}

	/**
	 * Reads the next word of the vocabulary file.
	 *
	 * @param reader
	 *            the vocabulary file
	 * @return the trimmed line, null at the end of the file
	 * @throws IOException
	 *             if reading fails
	 */
	protected String nextWord(BufferedReader reader) throws IOException {
		String s = reader.readLine();
		return s == null ? null : s.trim();
	}

	/**
	 * Reads the document file: three header lines, then one line
	 * "<code>docId wordId count</code>" per word occurrence, grouped by document.
	 *
	 * @param reader
	 *            the document file
	 * @param maxDocs
	 *            maximum number of documents to read, negative for all
	 * @return the documents
	 * @throws IOException
	 *             if reading fails
	 */
	protected DocumentProvider<String> createDocumentProvider(
			BufferedReader reader, int maxDocs) throws IOException {
		// Get over first 3 lines.
		reader.readLine();
		reader.readLine();
		reader.readLine();

		DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>(
				stemmingVocab != null ? stemmingVocab : orgVocab);

		DefaultDocumentProvider<String>.DefaultDocument d = null;

		int docCounter = 0;
		int oldDocId = -1;
		String line = reader.readLine();
		int nextDocId = nextDocId(line);
		while (nextDocId != -1) {
			if (nextDocId != oldDocId) {
				if (maxDocs >= 0 && docCounter == maxDocs) {
					break;
				}
				d = provider.newDocument();
				docCounter++;
			}
			int wordId = nextWordId(line);
			int fr = nextFr(line);
			String word = orgVocab.getWord(wordId - 1);
			if (stemmingVocab != null) {
				word = wordIndexToStem.get(wordId);
			}
			else if (word == null) {
				throw new IllegalStateException();
			}
			d.addWord(word, fr);
			oldDocId = nextDocId;
			line = reader.readLine();
			nextDocId = nextDocId(line);
		}
		return provider;
	}

	/**
	 * Extracts the document id (first column) of a line of the document file.
	 *
	 * @param line
	 *            the line, may be null
	 * @return the document id, -1 if the line is null
	 */
	protected int nextDocId(String line) {
		if (line == null) {
			return -1;
		}
		int index = line.indexOf(' ');
		return Integer.parseInt(line.substring(0, index));
	}

	/**
	 * Extracts the word id (second column) of a line of the document file.
	 *
	 * @param line
	 *            the line
	 * @return the word id (1-based)
	 */
	protected int nextWordId(String line) {
		int index = line.indexOf(' ');
		int index2 = line.indexOf(' ', index + 1);

		return Integer.parseInt(line.substring(index + 1, index2));
	}

	/**
	 * Extracts the count (last column) of a line of the document file.
	 *
	 * @param line
	 *            the line
	 * @return the frequency of the word in the document
	 */
	protected int nextFr(String line) {
		int index = line.lastIndexOf(' ');

		return Integer.parseInt(line.substring(index + 1, line.length()));
	}

	private static final TokenizerFactory STEMMER = new PorterStemmerTokenizerFactory(
			new RegExTokenizerFactory("\\S+"));

	private static String stem(String word) {
		char[] cs = word.toCharArray();
		java.util.Iterator<String> it = STEMMER.tokenizer(cs, 0, cs.length).iterator();
		return it.hasNext() ? it.next() : word;
	}
}
