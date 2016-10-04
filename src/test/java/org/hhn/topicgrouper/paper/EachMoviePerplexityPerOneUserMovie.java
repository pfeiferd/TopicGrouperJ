package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.eval.EachMovieParser;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorAlt;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorWithFoldIn;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;

public class EachMoviePerplexityPerOneUserMovie extends
		APExtractPerplexityNTopics {
	public EachMoviePerplexityPerOneUserMovie(Random random, double concAlpha,
			double concBeta, boolean fast) {
		super(random, concAlpha, concBeta, fast);
	}

// TODO	
//	@Override
//	protected TGPerplexityCalculator<String> initPerplexityCalculator() {
//		return new TGPerplexityCalculator<String>(0.01);
//	}

// TODO	
//	@Override
//	protected AbstractLDAPerplexityCalculator<String> initLDAPerplexityCalculator1() {
//		return new LDAPerplexityCalculatorAlt<String>(false) {
//			@Override
//			protected AbstractLDAPerplexityCalculator<String>.ComputationHelper initComputationHelper() {
//				return new OneWordComputationHelper() {
//					@Override
//					protected Random getRandom() {
//						return random;
//					}
//				};
//			}
//		};
//	}

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
	protected AbstractLDAPerplexityCalculator<String> createLDAPerplexityCalculator2(
			int gibbsIterations) {
		return new LDAPerplexityCalculatorWithFoldIn<String>(false, gibbsIterations) {
			@Override
			protected AbstractLDAPerplexityCalculator<String>.ComputationHelper initComputationHelper() {
				return new OneWordComputationHelper() {
					@Override
					protected Random getRandom() {
						return random;
					}
				};
			}
		};
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
	protected HoldOutSplitter<String> createHoldoutSplitter(int step,
			DocumentProvider<String> documentProvider) {
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
		new EachMoviePerplexityPerOneUserMovie(new Random(42), 50, 300, true).run(
				100, 20, 1);
	}	
}
