package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
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
	private final TokenizerFactory factory;

	public APParser(boolean removeStopWords, boolean stemming) {
		TokenizerFactory baseFactory = new LowerCaseTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		if (removeStopWords) {
			baseFactory = new EnglishStopTokenizerFactory(baseFactory);
		}
		if (stemming) {
			baseFactory = new PorterStemmerTokenizerFactory(baseFactory);
		}
		factory = baseFactory;
	}

	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			final DefaultDocumentProvider<String> documentProvider = new DefaultDocumentProvider<String>();

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();

			final FileInputStream inputStream = new FileInputStream(file);
			// Fix the stream by wrapping the file with a root tag
			// and fixing some unfortunate character sequences.
			InputStream fixInputStream = new InputStream() {
				private final String rootOpen = "<root>";
				private final String rootClose = "</root>";

				int counter = 0;
				int counter2 = 0;

				@Override
				public int read() throws IOException {
					if (counter < rootOpen.length()) {
						return rootOpen.charAt(counter++);
					}
					int res = inputStream.read();
					if (res != -1) {
						if (res == '&') {
							res = ' ';
						}
						return res;
					}
					if (counter2 < rootClose.length()) {
						return rootClose.charAt(counter2++);
					}
					return -1;
				}
			};

			InputSource inputSource = new InputSource(fixInputStream);

			parser.parse(inputSource, new DefaultHandler() {
				boolean inText = false;
				char[] buffer = new char[512];
				int pos = 0;
				
				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equals("TEXT")) {
						inText = true;
						pos = 0;
					}
				}

				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {
					if (inText) {
						if (length + pos > buffer.length) {
							char[] newBuffer = new char[Math.max(
									buffer.length * 2, length + pos)];
							System.arraycopy(buffer, 0, newBuffer, 0, pos);
							buffer = newBuffer;
						}
						System.arraycopy(ch, start, buffer, pos, length);
						pos += length;
					}
				}

				@Override
				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equals("TEXT")) {
						inText = false;
						fillDocument(documentProvider.newDocument(), buffer,
								pos);
					}
				}
			});
			inputStream.close();

			return documentProvider;
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void fillDocument(
			DefaultDocumentProvider<String>.DefaultDocument entry, char[] cs,
			int length) {
		Tokenizer t = new PunctuationStopListTokenizer(factory.tokenizer(cs, 0,
				length));
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

	public static void main(String[] args) {
		DocumentProvider<String> entryProvider = new APParser(true, true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getVocab().getNumberOfWords());
	}
}
