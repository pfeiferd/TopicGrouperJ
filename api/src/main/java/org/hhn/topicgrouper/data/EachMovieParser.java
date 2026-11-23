package org.hhn.topicgrouper.data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

/**
 * Reads the votes of the EachMovie collaborative filtering data set (DEC
 * Systems Research Center, 1997; withdrawn in 2004 and no longer distributed).
 * The expected file is <code>Vote.txt</code>, tab separated, one vote per line,
 * without header: person id, movie id, score (0 to 1 in steps of 0.2) and
 * further columns that are ignored, e.g.
 *
 * <pre>
 * 1	1	0.6	1	1997-04-07
 * </pre>
 *
 * A document is a person, the words are the ids of the movies (as strings) the
 * person rated with at least the minimal score; persons with fewer such votes
 * than the minimal number are dropped.
 */
public class EachMovieParser {
	private final int minVoteSize;
	private final double minRatingLevel;

	/**
	 * Creates the parser.
	 *
	 * @param minVoteSize
	 *            minimal number of (kept) votes of a person
	 * @param minRatingLevel
	 *            minimal score of a vote to be kept
	 */
	public EachMovieParser(int minVoteSize, double minRatingLevel) {
		this.minVoteSize = minVoteSize;
		this.minRatingLevel = minRatingLevel;
	}

	/**
	 * Reads the votes of the file.
	 *
	 * @param file
	 *            the votes file
	 * @return the documents
	 */
	public DocumentProvider<String> getCorpusDocumentProvider(File file) {
		try {
			Map<Integer, DefaultDocumentProvider<String>.DefaultDocument> userIdToRating = new LinkedHashMap<Integer, DefaultDocumentProvider<String>.DefaultDocument>();

			DefaultDocumentProvider<String> provider = new DefaultDocumentProvider<String>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			try {
				String line = reader.readLine();
				while (line != null) {
					String[] values = line.split("\t");
					int userId = Integer.valueOf(values[0].trim());
					int movieId = Integer.valueOf(values[1].trim());
					double rating = Double.valueOf(values[2].trim());
					// Just keep positive ratings;
					if (rating >= minRatingLevel) {
						DefaultDocumentProvider<String>.DefaultDocument d = userIdToRating
								.get(userId);
						if (d == null) {
							d = provider.newDocument();
							userIdToRating.put(userId, d);
						}
						d.addWord(String.valueOf(movieId));
					}
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
			DefaultDocumentProvider<String> provider2 = new DefaultDocumentProvider<String>();
			for (Document<String> d : userIdToRating.values()) {
				if (d.getSize() >= minVoteSize) {
					provider2.addDocument(d);
				}
			}
			return provider2;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
