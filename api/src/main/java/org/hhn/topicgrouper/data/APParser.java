package org.hhn.topicgrouper.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

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
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Reads the extract of the Associated Press corpus (2246 documents) that David
 * Blei ships with his LDA-C implementation (<code>ap.tgz</code>, file
 * <code>ap/ap.txt</code>). The file is a sequence of SGML documents without a
 * root element:
 *
 * <pre>
 * &lt;DOC&gt;
 * &lt;DOCNO&gt; AP881218-0003 &lt;/DOCNO&gt;
 * &lt;TEXT&gt;
 *  A 16-year-old student at a private Baptist school ...
 * &lt;/TEXT&gt;
 * &lt;/DOC&gt;
 * </pre>
 *
 * Only the content of the <code>TEXT</code> elements is used, one document per
 * element. The file is wrapped into a root element and every <code>&amp;</code>
 * is replaced by a space before it is handed to an XML parser. Text is lower
 * cased and tokenized, optionally freed from English stop words and stemmed;
 * only words with at least two characters starting with a letter are kept.
 */
public class APParser {
	private final TokenizerFactory factory;

	/**
	 * Creates the parser and its tokenizer chain.
	 *
	 * @param removeStopWords
	 *            whether English stop words are removed
	 * @param stemming
	 *            whether words are stemmed
	 */
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

	/**
	 * Reads the documents of the file.
	 *
	 * @param file
	 *            the <code>ap.txt</code> file
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			final DefaultDocumentProvider<String> documentProvider = new DefaultDocumentProvider<String>();

			SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(false);
			SAXParser parser = factory.newSAXParser();

			final FileInputStream inputStream = new FileInputStream(file);
			try {
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

				parser.parse(new InputSource(fixInputStream),
						new DefaultHandler() {
							boolean inText = false;
							char[] buffer = new char[512];
							int pos = 0;

							@Override
							public void startElement(String uri,
									String localName, String qName,
									Attributes attributes) throws SAXException {
								if (qName.equals("TEXT")) {
									inText = true;
									pos = 0;
								}
							}

							@Override
							public void characters(char[] ch, int start,
									int length) throws SAXException {
								if (inText) {
									if (length + pos > buffer.length) {
										char[] newBuffer = new char[Math.max(
												buffer.length * 2, length + pos)];
										System.arraycopy(buffer, 0, newBuffer,
												0, pos);
										buffer = newBuffer;
									}
									System.arraycopy(ch, start, buffer, pos,
											length);
									pos += length;
								}
							}

							@Override
							public void endElement(String uri,
									String localName, String qName)
									throws SAXException {
								if (qName.equals("TEXT")) {
									inText = false;
									fillDocument(documentProvider.newDocument(),
											buffer, pos);
								}
							}
						});
			} finally {
				inputStream.close();
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
	 * Tokenizes a text and adds its words (at least two characters, starting
	 * with a letter) to a document.
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
			if (word.length() > 1 && Character.isLetter(word.charAt(0))) {
				entry.addWord(word);
			}
		}
	}
}
