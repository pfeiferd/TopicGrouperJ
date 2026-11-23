package org.hhn.topicgrouper.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.hhn.topicgrouper.doc.LabelingDocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultLabelingDocumentProvider;

/**
 * Reads the OHSUMED subset "ohscal" (11162 MEDLINE abstracts of 10
 * cardiovascular disease categories) as the Weka ARFF file
 * <code>ohscal.wc.arff</code> of George Forman's collection of text data sets
 * in bag-of-words form ("19MclassTextWc"). Word counts and the class are
 * attributes:
 *
 * <pre>
 * &#64;relation ohscal.wc
 * &#64;attribute patient numeric
 * &#64;attribute blood numeric
 * ...
 * &#64;attribute class {Antibodies,Carcinoma,...}
 * &#64;data
 * {0 3, 1 1, ..., 11465 Carcinoma}
 * </pre>
 *
 * The words are the names of the numeric attributes, the counts their values
 * and the label is the value of the nominal class attribute, which must be the
 * last attribute. The instances are usually sparse (<code>{index value, ...}</code>
 * sorted by index); dense comma separated rows (one value per attribute) are
 * accepted as well. As in Weka, an attribute that is missing from a sparse
 * instance has the value 0, which for the class means its first value. Values
 * are truncated to integers, zero counts are skipped. Comment lines
 * (<code>%</code>) and blank lines are ignored, attribute names may be quoted.
 * The file is read as UTF-8.
 */
public class OHSUMEDParser {
	/** Creates the parser; it has no settings. */
	public OHSUMEDParser() {
	}

	/**
	 * Reads the labeled documents of the file.
	 *
	 * @param fileName
	 *            the path of the ARFF file
	 * @return the labeled documents
	 */
	public LabelingDocumentProvider<String, String> getCorpusDocumentProvider(
			String fileName) {
		return getCorpusDocumentProvider(new File(fileName));
	}

	/**
	 * Reads the labeled documents of the file.
	 *
	 * @param file
	 *            the ARFF file
	 * @return the labeled documents
	 */
	public LabelingDocumentProvider<String, String> getCorpusDocumentProvider(
			File file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), Charset.forName("UTF-8")));
			try {
				return createDocumentProvider(reader);
			} finally {
				reader.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Parses the ARFF header and data section (see the class comment).
	 *
	 * @param reader
	 *            the ARFF content
	 * @return the labeled documents
	 * @throws IOException
	 *             if reading fails
	 */
	protected LabelingDocumentProvider<String, String> createDocumentProvider(
			BufferedReader reader) throws IOException {
		List<String> attributes = new ArrayList<String>();
		List<String> classValues = null;

		// Header
		String line = reader.readLine();
		while (line != null) {
			String trimmed = line.trim();
			String lower = trimmed.toLowerCase();
			if (lower.startsWith("@attribute")) {
				String rest = trimmed.substring("@attribute".length()).trim();
				// Name may be quoted.
				String name;
				String type;
				if (rest.startsWith("'") || rest.startsWith("\"")) {
					int end = rest.indexOf(rest.charAt(0), 1);
					name = rest.substring(1, end);
					type = rest.substring(end + 1).trim();
				} else {
					int end = indexOfWhitespace(rest);
					name = end == -1 ? rest : rest.substring(0, end);
					type = end == -1 ? "" : rest.substring(end).trim();
				}
				attributes.add(name);
				if (type.startsWith("{")) {
					classValues = new ArrayList<String>();
					for (String value : type.substring(1, type.lastIndexOf('}'))
							.split(",")) {
						classValues.add(unquote(value.trim()));
					}
				} else {
					classValues = null;
				}
			} else if (lower.startsWith("@data")) {
				break;
			}
			line = reader.readLine();
		}
		if (attributes.isEmpty()) {
			throw new IllegalArgumentException("no @attribute declarations");
		}
		if (classValues == null) {
			throw new IllegalArgumentException(
					"last attribute must be the nominal class attribute");
		}
		int classIndex = attributes.size() - 1;

		// Data
		DefaultLabelingDocumentProvider<String, String> provider = new DefaultLabelingDocumentProvider<String, String>();
		line = reader.readLine();
		while (line != null) {
			String trimmed = line.trim();
			if (!trimmed.isEmpty() && !trimmed.startsWith("%")) {
				List<int[]> indices = new ArrayList<int[]>();
				List<String> values = new ArrayList<String>();
				if (trimmed.startsWith("{")) {
					// Sparse instance
					String inner = trimmed.substring(1, trimmed.lastIndexOf('}'))
							.trim();
					if (!inner.isEmpty()) {
						for (String entry : inner.split(",")) {
							entry = entry.trim();
							int end = indexOfWhitespace(entry);
							if (end == -1) {
								throw new IllegalArgumentException(
										"bad sparse entry: " + entry);
							}
							indices.add(new int[] { Integer.parseInt(entry
									.substring(0, end)) });
							values.add(unquote(entry.substring(end).trim()));
						}
					}
				} else {
					// Dense instance
					String[] fields = trimmed.split(",");
					if (fields.length != attributes.size()) {
						throw new IllegalArgumentException("expected "
								+ attributes.size() + " values: " + trimmed);
					}
					for (int i = 0; i < fields.length; i++) {
						indices.add(new int[] { i });
						values.add(unquote(fields[i].trim()));
					}
				}
				// Weka semantics: an omitted nominal value is the first one.
				String label = classValues.get(0);
				for (int i = 0; i < indices.size(); i++) {
					if (indices.get(i)[0] == classIndex) {
						label = values.get(i);
					}
				}
				DefaultLabelingDocumentProvider<String, String>.DefaultLabeledDocument d = provider
						.newLabeledDocument(label);
				for (int i = 0; i < indices.size(); i++) {
					int index = indices.get(i)[0];
					if (index != classIndex) {
						if (index < 0 || index >= attributes.size()) {
							throw new IllegalArgumentException(
									"bad attribute index " + index);
						}
						String value = values.get(i);
						int fr = value.equals("?") ? 0 : (int) Double
								.parseDouble(value);
						if (fr != 0) {
							d.addWord(attributes.get(index), fr);
						}
					}
				}
			}
			line = reader.readLine();
		}
		return provider;
	}

	private static int indexOfWhitespace(String s) {
		for (int i = 0; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	private static String unquote(String s) {
		if (s.length() >= 2
				&& (s.charAt(0) == '\'' || s.charAt(0) == '"')
				&& s.charAt(s.length() - 1) == s.charAt(0)) {
			return s.substring(1, s.length() - 1);
		}
		return s;
	}
}
