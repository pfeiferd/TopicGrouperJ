package org.hhn.topicgrouper.eval;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;

import org.hhn.topicgrouper.doc.Document;
import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.DefaultDocumentProvider;

public class EachMovieParser {
	private final int minVoteSize;
	private final double minRatingLevel;

	public EachMovieParser(int minVoteSize, double minRatingLevel) {
		this.minVoteSize = minVoteSize;
		this.minRatingLevel = minRatingLevel;
	}

	public DocumentProvider<Integer> getCorpusDocumentProvider(File file)
			throws IOException {
		Map<Integer, DefaultDocumentProvider<Integer>.DefaultDocument> userIdToRating = new HashMap<Integer, DefaultDocumentProvider<Integer>.DefaultDocument>();

		DefaultDocumentProvider<Integer> provider = new DefaultDocumentProvider<Integer>();
		LineNumberReader lineNumberReader = new LineNumberReader(
				new FileReader(file));
		String line = lineNumberReader.readLine();
		while (line != null) {
			String[] values = line.split("\t");
			int userId = Integer.valueOf(values[0]);
			int movieId = Integer.valueOf(values[1]);
			double rating = Double.valueOf(values[2]);
			// Just keep positive ratings;
			if (rating >= minRatingLevel) {
				DefaultDocumentProvider<Integer>.DefaultDocument d = userIdToRating
						.get(userId);
				if (d == null) {
					d = provider.newDocument();
					userIdToRating.put(userId, d);
				}
				d.addWord(movieId);
			}
			line = lineNumberReader.readLine();
		}
		DefaultDocumentProvider<Integer> provider2 = new DefaultDocumentProvider<Integer>();
		for (Document<Integer> d : userIdToRating.values()) {
			if (d.getSize() >= minVoteSize) {
				provider2.addDocument(d);
			}
		}

		lineNumberReader.close();

		return provider2;
	}

	public static void main(String[] args) throws IOException {
		EachMovieParser parser = new EachMovieParser(100, 0.6);
		DocumentProvider<Integer> entryProvider = parser
				.getCorpusDocumentProvider(new File(
						"src/test/resources/EachMovie/Vote.txt"));
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getNumberOfWords());
	}
}
