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

import com.aliasi.tokenizer.PorterStemmer;

public class UCIParser {
	private final DefaultVocab<String> orgVocab;
	private final TIntObjectMap<String> wordIndexToStem;
	private final DefaultVocab<String> stemmingVocab;
	private DocumentProvider<String> documentProvider;

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

	public DocumentProvider<String> getDocumentProvider() {
		return documentProvider;
	}

	protected void initVocab(BufferedReader reader) throws IOException {
		int wordIndex = 1;
		String word = nextWord(reader);
		while (word != null && !word.isEmpty()) {
//			System.out.println(wordIndex + " " + word);
			orgVocab.addEntry(word);
			if (stemmingVocab != null) {
				String stem = wordIndexToStem.get(wordIndex);
				if (stem == null) {
					stem = PorterStemmer.stem(word);
					wordIndexToStem.put(wordIndex, stem);
					stemmingVocab.addEntry(stem);
				}
			}
			word = nextWord(reader);
			wordIndex++;
		}
	}

	protected String nextWord(BufferedReader reader) throws IOException {
		String s = reader.readLine();
		return s == null ? null : s.trim();
	}

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

	protected int nextDocId(String line) {
		if (line == null) {
			return -1;
		}
		int index = line.indexOf(' ');
		return Integer.parseInt(line.substring(0, index));
	}

	protected int nextWordId(String line) {
		int index = line.indexOf(' ');
		int index2 = line.indexOf(' ', index + 1);

		return Integer.parseInt(line.substring(index + 1, index2));
	}

	protected int nextFr(String line) {
		int index = line.lastIndexOf(' ');

		return Integer.parseInt(line.substring(index + 1, line.length()));
	}
	
	public static void main(String[] args) throws IOException {
		UCIParser uciParser = new UCIParser(true, new File("./src/test/resources/kos"), "kos", 100);
		DocumentProvider<String> provider = uciParser.getDocumentProvider();
		System.out.println(provider.getDocuments().size());
		System.out.println(provider.getVocab().getNumberOfWords());

	}
}
