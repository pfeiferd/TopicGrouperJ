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

public class EachMovieParser<T> {
	private final int minVoteSize;
	private final double minRatingLevel;

	public EachMovieParser(int minVoteSize, double minRatingLevel) {
		this.minVoteSize = minVoteSize;
		this.minRatingLevel = minRatingLevel;
	}

	public DocumentProvider<T> getCorpusDocumentProvider(File file) {
		try {
			Map<Integer, DefaultDocumentProvider<T>.DefaultDocument> userIdToRating = new HashMap<Integer, DefaultDocumentProvider<T>.DefaultDocument>();

			DefaultDocumentProvider<T> provider = new DefaultDocumentProvider<T>();
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
					DefaultDocumentProvider<T>.DefaultDocument d = userIdToRating
							.get(userId);
					if (d == null) {
						d = provider.newDocument();
						userIdToRating.put(userId, d);
					}
					d.addWord(convertMovieId(movieId));
				}
				line = lineNumberReader.readLine();
			}
			DefaultDocumentProvider<T> provider2 = new DefaultDocumentProvider<T>();
			for (Document<T> d : userIdToRating.values()) {
				if (d.getSize() >= minVoteSize) {
					provider2.addDocument(d);
				}
			}

			lineNumberReader.close();

			return provider2;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	protected T convertMovieId(int movieId) {
		return (T) Integer.valueOf(movieId);
	}

	public static void main(String[] args) throws IOException {
		EachMovieParser<Integer> parser = new EachMovieParser<Integer>(100, 0.6);
		DocumentProvider<Integer> entryProvider = parser
				.getCorpusDocumentProvider(new File(
						"src/test/resources/EachMovie/Vote.txt"));
		System.out.println(entryProvider.getDocuments().size());
		System.out.println(entryProvider.getNumberOfWords());
	}
}
