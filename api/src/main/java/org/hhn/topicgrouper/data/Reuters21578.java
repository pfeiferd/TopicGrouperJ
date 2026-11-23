package org.hhn.topicgrouper.data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;

import com.aliasi.tokenizer.EnglishStopTokenizerFactory;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.LowerCaseTokenizerFactory;
import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Reader for the Reuters-21578 (Distribution 1.0) collection, i.e. the
 * <code>reut2-*.sgm</code> files, using the ModApte split.
 * Only documents with at least one of the ten largest topics ({@link #TOPICS})
 * are used; as in the original experiments a document that has several of these
 * topics is added once per topic (with that topic as its label). Text (title,
 * dateline and body) is lower cased, tokenized, (optionally) freed
 * from English stop words and stemmed; only words with at least two characters
 * starting with a letter are kept.
 */
public class Reuters21578 {
	private static final Pattern DOC = Pattern.compile("<REUTERS ([^>]*)>(.*?)</REUTERS>", Pattern.DOTALL);
	private static final Pattern SPLIT = Pattern.compile("LEWISSPLIT=\"([^\"]*)\"");
	private static final Pattern TOPICS_TAG = Pattern.compile("<TOPICS>(.*?)</TOPICS>", Pattern.DOTALL);
	private static final Pattern D = Pattern.compile("<D>(.*?)</D>", Pattern.DOTALL);
	private static final Pattern TEXT = Pattern.compile("<(TITLE|DATELINE|BODY)>(.*?)</\\1>", Pattern.DOTALL);
	private static final Pattern BOILERPLATE = Pattern.compile("\\s*Reuter\\s*(&#3;)?\\s*$");
	private static final Pattern ENTITY = Pattern.compile("&#?\\w+;");

	/** The ten most frequent topics of the collection. */
	public static final String[] TOPICS = { "earn", "acq", "money-fx", "grain", "crude",
			"trade", "interest", "wheat", "ship", "corn" };

	private final TokenizerFactory factory;

	/**
	 * Creates the reader and its tokenizer chain (always stemming).
	 *
	 * @param removeStopWords
	 *            whether English stop words are removed
	 */
	public Reuters21578(boolean removeStopWords) {
		TokenizerFactory baseFactory = new LowerCaseTokenizerFactory(
				IndoEuropeanTokenizerFactory.INSTANCE);
		factory = new PorterStemmerTokenizerFactory(
				removeStopWords ? new EnglishStopTokenizerFactory(baseFactory)
						: baseFactory);
	}

	/**
	 * Reads the <code>.sgm</code> files of a directory (in the order of their names).
	 *
	 * @param directory
	 *            directory with the <code>.sgm</code> files
	 * @param train
	 *            include the ModApte training documents
	 * @param test
	 *            include the ModApte test documents
	 * @return the labeled documents
	 */
	public LabelingDocumentProvider<String, String> getCorpusDocumentProvider(
			File directory, boolean train, boolean test) {
		File[] files = directory.listFiles();
		if (files == null) {
			throw new IllegalArgumentException("not a directory: " + directory);
		}
		Arrays.sort(files);
		DefaultLabelingDocumentProvider<String, String> provider = new DefaultLabelingDocumentProvider<String, String>();
		try {
			for (File file : files) {
				if (file.getName().endsWith(".sgm")) {
					String content = new String(Files.readAllBytes(file.toPath()), Charset.forName("ISO-8859-1"));
					Matcher m = DOC.matcher(content);
					while (m.find()) {
						addDocument(provider, m.group(1), m.group(2), train, test);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return provider;
	}

	private void addDocument(DefaultLabelingDocumentProvider<String, String> provider,
			String attributes, String content, boolean train, boolean test) {
		Matcher split = SPLIT.matcher(attributes);
		if (!split.find()) {
			return;
		}
		String s = split.group(1);
		if (!((train && s.equals("TRAIN")) || (test && s.equals("TEST")))) {
			return;
		}
		Matcher topics = TOPICS_TAG.matcher(content);
		if (!topics.find()) {
			return;
		}
		Matcher d = D.matcher(topics.group(1));
		java.util.Set<String> docTopics = new java.util.HashSet<String>();
		while (d.find()) {
			docTopics.add(d.group(1).trim());
		}
		StringBuilder text = new StringBuilder();
		Matcher parts = TEXT.matcher(content);
		while (parts.find()) {
			String part = parts.group(2);
			if (parts.group(1).equals("BODY")) {
				part = BOILERPLATE.matcher(part).replaceAll("");
			}
			text.append(ENTITY.matcher(part).replaceAll(" ")).append('\n');
		}
		addLabeled(provider, docTopics, text.toString());
	}

	/** Adds one copy of the text per topic of {@link #TOPICS} it has. */
	private void addLabeled(DefaultLabelingDocumentProvider<String, String> provider,
			java.util.Set<String> docTopics, String text) {
		for (String topic : TOPICS) {
			if (docTopics.contains(topic)) {
				addWords(provider.newLabeledDocument(topic), text);
			}
		}
	}

	private void addWords(DefaultDocumentProvider<String>.DefaultDocument document, String text) {
		char[] cs = text.toCharArray();
		Iterator<String> it = factory.tokenizer(cs, 0, cs.length).iterator();
		while (it.hasNext()) {
			String word = it.next();
			if (word.length() > 1 && Character.isLetter(word.charAt(0))) {
				document.addWord(word);
			}
		}
	}

	/**
	 * Reads the ApteMod version of the collection as distributed with NLTK
	 * (<code>reuters.zip</code>: <code>reuters/cats.txt</code> and one text file
	 * per document in <code>reuters/training/</code> and <code>reuters/test/</code>).
	 * The ApteMod training and test documents are those of the ModApte split that
	 * have at least one topic.
	 *
	 * @param zip
	 *            the zip file
	 * @param train
	 *            include the training documents
	 * @param test
	 *            include the test documents
	 * @param labeled
	 *            if true one copy per topic of {@link #TOPICS} (as
	 *            {@link #getCorpusDocumentProvider(File, boolean, boolean)}), otherwise
	 *            every document exactly once, labeled with its first topic
	 * @return the documents
	 */
	public LabelingDocumentProvider<String, String> getNltkDocumentProvider(File zip,
			boolean train, boolean test, boolean labeled) {
		DefaultLabelingDocumentProvider<String, String> provider = new DefaultLabelingDocumentProvider<String, String>();
		try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(zip)) {
			java.util.zip.ZipEntry cats = z.getEntry("reuters/cats.txt");
			if (cats == null) {
				throw new IllegalArgumentException("no reuters/cats.txt in " + zip);
			}
			String[] lines = new String(readAll(z, cats), Charset.forName("ISO-8859-1")).split("\\r?\\n");
			for (String line : lines) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length < 2) {
					continue;
				}
				String id = parts[0];
				if (!((train && id.startsWith("training/")) || (test && id.startsWith("test/")))) {
					continue;
				}
				java.util.zip.ZipEntry e = z.getEntry("reuters/" + id);
				if (e == null) {
					continue;
				}
				String text = ENTITY.matcher(new String(readAll(z, e), Charset.forName("ISO-8859-1")))
						.replaceAll(" ");
				if (labeled) {
					addLabeled(provider, new java.util.HashSet<String>(
							Arrays.asList(parts).subList(1, parts.length)), text);
				} else {
					addWords(provider.newLabeledDocument(parts[1]), text);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return provider;
	}

	private static byte[] readAll(java.util.zip.ZipFile z, java.util.zip.ZipEntry e) throws IOException {
		try (java.io.InputStream in = z.getInputStream(e)) {
			java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) > 0) {
				out.write(buf, 0, n);
			}
			return out.toByteArray();
		}
	}
}
