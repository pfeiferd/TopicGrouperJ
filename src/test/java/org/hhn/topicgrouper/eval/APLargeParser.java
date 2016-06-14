package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hhn.topicgrouper.base.DefaultDocumentProvider;
import org.hhn.topicgrouper.base.DefaultDocumentProvider.DefaultDocument;
import org.hhn.topicgrouper.base.DocumentProvider;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.PunctuationStopListTokenizer;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class APLargeParser {
	private TokenizerFactory factory;

	public APLargeParser(boolean removeStopWords) {
		TokenizerFactory baseFactory = new LowerCaseTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		factory = new PorterStemmerTokenizerFactory(
				removeStopWords ? new EnglishStopTokenizerFactory(baseFactory)
						: baseFactory);
	}

	public DocumentProvider<String> getCorpusDocumentProvider(File folder,
			final int maxDocuments) {
		try {
			final DefaultDocumentProvider<String> documentProvider = new DefaultDocumentProvider<String>();
			final int[] documents = new int[1];

			File[] files = folder.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return pathname.getName().startsWith("ap");
				}
			});
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();
			XMLReader xmlReader = parser.getXMLReader();
			xmlReader.setContentHandler(new DefaultHandler() {
				DefaultDocument entry = null;

				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equals("TEXT")) {
						if (documents[0] < maxDocuments) {
							entry = documentProvider.newDocument();
							documents[0]++;
						}
					}
					else {
						entry = null;
					}

				}

				@Override
				public void characters(char[] ch, int start, int length)
						throws SAXException {
					if (entry != null) {
						extendDocument(entry, documentProvider, ch, start,
								length);
					}
				}

				@Override
				public void endElement(String uri, String localName,
						String qName) throws SAXException {
					if (qName.equals("TEXT")) {
//						System.out.println("---------------");	
						entry = null;
					}
				}
			});

			for (File apFile : files) {
				if (documents[0] < maxDocuments) {
					System.out.println(apFile);
					System.out.println(documents[0]);
					final FileInputStream inputStream = new FileInputStream(
							apFile);
					InputStream fixInputStream = new InputStream() {
						private final String rootOpen = "<root>";
						private final String rootClose = "</root>";

						int counter = 0;
						int counter2 = 0;
						
						int lookahead = -2;

						@Override
						public int read() throws IOException {
							if (counter < rootOpen.length()) {
								return rootOpen.charAt(counter++);
							}
							int res;
							if (lookahead == -2) {
								res = inputStream.read();
							}
							else {
								res = lookahead;
							}
							lookahead = inputStream.read();
							if (res != -1) {
								if (res == '&' && Character.isWhitespace(lookahead)) {
									res = ' ';
									lookahead = -2;
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
					xmlReader.parse(inputSource);
					inputStream.close();
				}
			}

			return documentProvider;
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected void extendDocument(DefaultDocument entry,
			DefaultDocumentProvider<String> documentProvider, char[] cs,
			int start, int length) {
		Tokenizer t = new PunctuationStopListTokenizer(factory.tokenizer(cs,
				start, length));
		Iterator<String> it = t.iterator();
		while (it.hasNext()) {
			String word = it.next();
			if (word.length() > 1) {
				char c = word.charAt(0);
				if (Character.isLetter(c)) {
//					System.out.println(word);					
					entry.addWord(word);
				}
			}
		}
	}

	public static void main(String[] args) {
		DocumentProvider<String> entryProvider = new APLargeParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/full"), 1000000000);
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getNumberOfWords());
	}
}