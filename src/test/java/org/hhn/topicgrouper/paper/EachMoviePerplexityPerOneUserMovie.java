package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.DocumentSplitter;
import org.hhn.topicgrouper.doc.impl.EachWordDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.FiftyFiftyDocumentSplitter;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.EachMovieParser;

public class EachMoviePerplexityPerOneUserMovie extends
		APExtractPerplexityNTopics {
	public EachMoviePerplexityPerOneUserMovie(Random random, int gibbsIterations, double concAlpha,
			double concBeta, boolean fast) {
		super(random, gibbsIterations, concAlpha, concBeta, fast);
	}
	
	@Override
	protected DocumentSplitter<String> createDocumentSplitter() {
//		return new EachWordDocumentSplitter<String>(true);
		return new FiftyFiftyDocumentSplitter<String>(new Random(42));
	}

	@Override
	protected DocumentProvider<String> initBasicDocumentProvider() {
		EachMovieParser<String> parser = new EachMovieParser<String>(100, 0.6) {
			protected String convertMovieId(int movieId) {
				return String.valueOf(movieId);
			}
		};
		return parser.getCorpusDocumentProvider(new File(
				"src/test/resources/EachMovie/Vote.txt"));
	}
	
	@Override
	protected String createTGCSVBaseFileName() {
		return "EachMoviePerplexityNTopicsTG";
	}
	
	@Override
	protected String createLDACSVBaseFileName() {
		return "EachMoviePerplexityNTopicsLDA";		
	}
	
	@Override
	protected HoldOutSplitter<String> createHoldoutSplitter(DocumentProvider<String> documentProvider, int step, int repeat) {
		if (holdOutSplitter == null) {
			holdOutSplitter = new HoldOutSplitter<String>(random,
					documentProvider, 390, 1);
		}
		return holdOutSplitter;
	}

	@Override
	protected String getSerializationFileName() {
		return "./target/EachMovie.ser";
	}

	protected int nTopicFromStep(int step) {
		return step == 0 ? 2 : step == 1 ? 5 : (step - 1) * 10;
	}
	
//	@Override
//	protected void runTopicGrouper(PrintStream pw3, int step, int repeat,
//			DocumentProvider<String> documentProvider,
//			DocumentProvider<String> testDocumentProvider,
//			double[] tgPerplexity, double[] tgAcc) {
//	}
	
	public static void main(String[] args) throws IOException {
		new EachMoviePerplexityPerOneUserMovie(new Random(42), 100, 50, 300, true).run(20, 1);
	}	
}
