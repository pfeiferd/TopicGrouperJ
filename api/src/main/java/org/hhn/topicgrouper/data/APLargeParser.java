package org.hhn.topicgrouper.data;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
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
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Reads the full Associated Press newswire collection of TREC (AP88-90, part
 * of the TREC Text Research Collection, distributed by the LDC and not
 * publicly available). Every file of the directory whose name starts with
 * <code>ap</code> (e.g. <code>ap880212</code>) is read as ISO-8859-1 encoded
 * SGML:
 *
 * <pre>
 * &lt;DOC&gt;
 * &lt;DOCNO&gt; AP880212-0001 &lt;/DOCNO&gt;
 * &lt;FILEID&gt;AP-NR-02-12-88 2344EST&lt;/FILEID&gt;
 * &lt;HEAD&gt;Reports Former Saigon Officials Released from Reeducation Camp&lt;/HEAD&gt;
 * &lt;TEXT&gt;
 *    BANGKOK, Thailand (AP) ...
 * &lt;/TEXT&gt;
 * &lt;/DOC&gt;
 * </pre>
 *
 * One document is created per <code>DOC</code> element from the text of its
 * <code>HEAD</code> and <code>TEXT</code> elements (there may be several of
 * each). Each file is wrapped into a root element and an <code>&amp;</code>
 * followed by white space or by one of the letters <code>l, r, p, e</code>
 * (SGML entities the XML parser does not know) is replaced by a space before
 * the file is handed to an XML parser. Text is lower cased and tokenized,
 * optionally freed from English stop words and stemmed; only words with more
 * than two characters consisting of letters only are kept (see
 * {@link TwentyNGParser#isProperWord(String)}).
 */
public class APLargeParser {
	private final TokenizerFactory factory;
	private final PrintStream pw;
	private char[] buffer = new char[512];

	/**
	 * Parser with stemming.
	 *
	 * @param pw
	 *            stream for progress messages (one per file), may be null
	 * @param removeStopWords
	 *            whether English stop words are removed
	 */
	public APLargeParser(PrintStream pw, boolean removeStopWords) {
		this(pw, removeStopWords, true);
	}

	/**
	 * Parser with optional stemming.
	 *
	 * @param pw
	 *            stream for progress messages (one per file), may be null
	 * @param removeStopWords
	 *            whether English stop words are removed
	 * @param stemming
	 *            whether words are stemmed
	 */
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

	/**
	 * Reads the first documents of the corpus.
	 *
	 * @param folder
	 *            the directory with the <code>ap*</code> files (read in the
	 *            order of their names)
	 * @param maxDocuments
	 *            the maximum number of documents to read
	 * @return the documents
	 */
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
			if (files == null) {
				throw new IllegalArgumentException("not a directory: "
						+ folder);
			}
			Arrays.sort(files);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();
			XMLReader xmlReader = parser.getXMLReader();
			xmlReader.setContentHandler(new DefaultHandler() {
				boolean inText = false;
				// Whether the current DOC counts (maxDocuments not yet reached).
				boolean inDoc = false;
				int pos = 0;

				@Override
				public void startElement(String uri, String localName,
						String qName, Attributes attributes)
						throws SAXException {
					if (qName.equals("DOC")) {
						inDoc = documents[0] < maxDocuments;
						if (inDoc) {
							documents[0]++;
							pos = 0;
						}
					} else if (inDoc) {
						inText = qName.equals("TEXT") || qName.equals("HEAD");
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
					if (qName.equals("DOC")) {
						if (inDoc) {
							fillDocument(documentProvider.newDocument(), buffer,
									pos);
							inDoc = false;
						}
					} else if (qName.equals("TEXT") || qName.equals("HEAD")) {
						if (1 + pos > buffer.length) {
							char[] newBuffer = new char[Math.max(
									buffer.length * 2, 1 + pos)];
							System.arraycopy(buffer, 0, newBuffer, 0, pos);
							buffer = newBuffer;
						}
						buffer[pos] = ' ';
						pos++;
						inText = false;
					}
				}
			});

			for (File apFile : files) {
				if (documents[0] < maxDocuments) {
					if (pw != null) {
						pw.println("AP Parser Reading file: " + apFile);
						pw.println("Total read documents: " + documents[0]);
						pw.println("Vocab size: "
								+ documentProvider.getVocab()
										.getNumberOfWords());
					}
					final FileInputStream inputStream = new FileInputStream(
							apFile);
					try {
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
									// This is for screwed up ampersand
									// characters in the AP corpus
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

						InputSource inputSource = new InputSource(
								fixInputStream);
						// It is not UTF-8: will get errors of because of bad
						// byte sequences otherwise.
						inputSource.setEncoding("ISO-8859-1");
						xmlReader.parse(inputSource);
					} finally {
						inputStream.close();
					}
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

	/**
	 * Tokenizes a text and adds its proper words to a document.
	 *
	 * @param entry
	 *            the document the words are added to
	 * @param cs
	 *            the text
	 * @param length
	 *            number of characters of the text (starting at index 0)
	 */
	protected void fillDocument(
			DefaultDocumentProvider<String>.DefaultDocument entry, char[] cs,
			int length) {
		Tokenizer t = factory.tokenizer(cs, 0, length);
		Iterator<String> it = t.iterator();
		while (it.hasNext()) {
			String word = it.next();
			if (TwentyNGParser.isProperWord(word)) {
				entry.addWord(word);
			}
		}
	}
}
