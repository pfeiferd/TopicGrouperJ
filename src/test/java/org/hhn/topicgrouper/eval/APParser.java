package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hhn.topicgrouper.base.DefaultDocumentProvider;
import org.hhn.topicgrouper.base.DefaultDocumentProvider.DefaultDocument;
import org.hhn.topicgrouper.base.Document;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.PunctuationStopListTokenizer;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class APParser {
	private TokenizerFactory factory;

	public APParser(boolean removeStopWords) {
		TokenizerFactory baseFactory = new LowerCaseTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		factory = new PorterStemmerTokenizerFactory(
				removeStopWords ? new EnglishStopTokenizerFactory(baseFactory)
						: baseFactory);
	}

	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			final DefaultDocumentProvider<String> documentProvider = new DefaultDocumentProvider<String>();

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();
			parser.parse(file, new DefaultHandler() {
				boolean inText = false;
				
				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equals("TEXT")) {
						inText = true;
					}
				}
				
				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {
					if (inText) {
						createDocument(documentProvider, ch, start, length);
					}
				}
				
				@Override
				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equals("TEXT")) {
						inText = false;
					}
				}
			});

			return documentProvider;
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected Document<String> createDocument(
			DefaultDocumentProvider<String> documentProvider, char[] cs, int start, int length) {
		DefaultDocument entry = documentProvider.newDocument();
		Tokenizer t = new PunctuationStopListTokenizer(factory.tokenizer(cs, start, length));
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
		return entry;
	}

	public static void main(String[] args) {
		DocumentProvider<String> entryProvider = new APParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getNumberOfWords());
	}
}
