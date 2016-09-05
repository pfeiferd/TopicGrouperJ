package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider.DefaultDocument;

import com.aliasi.classify.Classification;
import com.aliasi.corpus.ClassificationHandler;
import com.aliasi.corpus.Corpus;
import com.aliasi.corpus.parsers.Reuters21578Parser;
import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.PunctuationStopListTokenizer;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class Reuters21578 {
	private TokenizerFactory factory;

	public Reuters21578(boolean removeStopWords) {
		TokenizerFactory baseFactory = new LowerCaseTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		factory = new PorterStemmerTokenizerFactory(
				removeStopWords ? new EnglishStopTokenizerFactory(baseFactory)
						: baseFactory);
	}

	public DocumentProvider<String> getCorpusDocumentProvider(File directory,
			String[] topics, boolean train, boolean test) {
		try {
			final DefaultDocumentProvider<String> documentProvider = new DefaultDocumentProvider<String>();

			for (final String topic : topics) {
				Corpus<ClassificationHandler<CharSequence, Classification>> corpus = Reuters21578Parser
						.corpus(topic, directory);
				ClassificationHandler<CharSequence, Classification> ch = new ClassificationHandler<CharSequence, Classification>() {
					@Override
					public void handle(CharSequence arg0, Classification arg1) {
						// if (nextIndex[0] > 10000) {
						// return;
						// }
						char[] cs = new char[arg0.length()];
						for (int i = 0; i < cs.length; i++) {
							cs[i] = arg0.charAt(i);
						}
						DefaultDocument entry = documentProvider.newDocument();
						Tokenizer t = new PunctuationStopListTokenizer(
								factory.tokenizer(cs, 0, arg0.length()));
						Iterator<String> it = t.iterator();
						while (it.hasNext()) {
							String word = it.next();
							if (word.length() > 1) {
								char c = word.charAt(0);
								if (Character.isLetter(c)) {
									entry.addWord(word);
								}
							}
						}
					}
				};
				if (train && test) {
					corpus.visitCorpus(ch);
				} else if (train) {
					corpus.visitTrain(ch);
				} else if (test) {
					corpus.visitTest(ch);
				}
			}

			return documentProvider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		String topics[] = Reuters21578Parser.availableTopics();
		DocumentProvider<String> entryProvider = new Reuters21578(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"),
						new String[] { topics[0] }, true, true);
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getNumberOfWords());
	}
}
