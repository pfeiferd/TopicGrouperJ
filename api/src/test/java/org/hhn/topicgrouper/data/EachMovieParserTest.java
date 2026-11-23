package org.hhn.topicgrouper.data;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.data.EachMovieParser;
import org.junit.Test;

public class EachMovieParserTest {
	@Test
	public void testVotes() throws Exception {
		File dir = Files.createTempDirectory("eachmovie").toFile();
		File file = new File(dir, "Vote.txt");
		Files.write(file.toPath(), ("1\t1\t0.6\t1\t1997-04-07\n" + "1\t2\t1.0\t1\t1997-04-07\n"
				+ "1\t3\t0.2\t1\t1997-04-07\n" + "2\t1\t0.8\t1\t1997-04-08\n" + "3\t1\t0.0\t1\t1997-04-08\n"
				+ "3\t2\t0.6\t1\t1997-04-08\n" + "3\t4\t0.6\t1\t1997-04-08\n").getBytes(Charset.forName("US-ASCII")));

		DocumentProvider<String> p = new EachMovieParser(2, 0.6).getCorpusDocumentProvider(file);
		assertEquals(2, p.getDocuments().size()); // persons 1 and 3, person 2 has only one vote
		assertEquals(3, p.getVocab().getNumberOfWords()); // movies 1, 2, 4
		assertEquals(-1, p.getVocab().getIndex("3"));
		assertEquals(1, p.getWordFrequency(p.getVocab().getIndex("1")));
		assertEquals(2, p.getWordFrequency(p.getVocab().getIndex("2")));
		assertEquals(2, p.getDocuments().get(0).getSize());

		assertEquals(3, new EachMovieParser(1, 0.6).getCorpusDocumentProvider(file).getDocuments().size());
	}
}
