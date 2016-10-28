package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;

import com.aliasi.classify.Classification;
import com.aliasi.corpus.ClassificationHandler;
import com.aliasi.corpus.Corpus;
import com.aliasi.corpus.parsers.Reuters21578ParserExt;
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

	public LabelingDocumentProvider<String, String> getCorpusDocumentProvider(File directory,
			boolean train, boolean test) {
		try {
			final DefaultLabelingDocumentProvider<String, String> documentProvider = new DefaultLabelingDocumentProvider<String, String>();

			Corpus<ClassificationHandler<CharSequence, Classification>> corpus = Reuters21578ParserExt
					.corpus(directory);
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
					DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument entry = documentProvider.newLabeledDocument(arg1.bestCategory());
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

			return documentProvider;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		LabelingDocumentProvider<String,String> entryProvider = new Reuters21578(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/reuters21578"), true, true);
		System.out.println(entryProvider.getAllLabels());
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getVocab().getNumberOfWords());
	}
}
