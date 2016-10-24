package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
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
	private final TokenizerFactory factory;
	private final PrintStream pw;
	
	public APLargeParser(PrintStream pw, boolean removeStopWords) {
		this(pw, removeStopWords, true);
	}

	public APLargeParser(PrintStream pw, boolean removeStopWords,
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
				DefaultDocumentProvider<String>.DefaultDocument entry = null;

				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equals("TEXT")) {
						if (documents[0] < maxDocuments) {
							entry = documentProvider.newDocument();
							documents[0]++;
						}
					} else {
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
						// System.out.println("---------------");
						entry = null;
					}
				}
			});

			for (File apFile : files) {
				if (documents[0] < maxDocuments /* && afterCriticalFile */) {
					if (pw != null) {
						pw.println("AP Parser Reading file: " + apFile);
						pw.println("Total read documents: " + documents[0]);
					}
					final FileInputStream inputStream = new FileInputStream(
							apFile);
					// Fix the stream by wrapping the file with a root tag
					// and fixing some unfortunate character sequences.
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
							} else {
								res = lookahead;
							}
							lookahead = inputStream.read();
							if (res != -1) {
								if (res == '&'
								// This is for screwed up ampersand characters
								// in the AP corpus
										&& (Character.isWhitespace(lookahead)
												||
												// This is for SGML escape
												// sequences that XML parser
												// does not like
												lookahead == 'l'
												|| lookahead == 'r'
												|| lookahead == 'p' || lookahead == 'e')) {
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
					// It is not UTF-8: will get errors of because of bad byte
					// sequences otherwise.
					inputSource.setEncoding("ISO-8859-1");
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

	protected void extendDocument(DefaultDocumentProvider<String>.DefaultDocument entry,
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
					// System.out.println(word);
					entry.addWord(word);
				}
			}
		}
	}

	public static void main(String[] args) {
		DocumentProvider<String> entryProvider = new APLargeParser(System.out,
				true).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/full"), Integer.MAX_VALUE);
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getVocab().getNumberOfWords());
	}
}
