package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.report.LDAPerplexityResultReporter;
import org.hhn.topicgrouper.validation.AbstractHyperparamOptGibbsLDA;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.validation.InDocumentHoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorAlt;

public class APExtractAlphBetaOptGibbsTester {
	public static void main(String[] args) throws Exception {
		DocumentProvider<String> documentProvider = new APParser(true)
				.getCorpusDocumentProvider(new File(
						"src/test/resources/ap-corpus/extract/ap.txt"));
		// DocumentProvider<String> documentProvider = new
		// TWCLDAPaperDocumentGenerator(
		// new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
		// 100, 30, 30, 0, null, 0.5, 0.8);

		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 10);

		final PrintStream pw = System.out; // new PrintStream(new File(
		// "./target/APExtractAlphBetaOptGibbsTester.csv"));

		AbstractHyperparamOptGibbsLDA<String> optimizer = new AbstractHyperparamOptGibbsLDA<String>(
				pw, splitter.getRest(), splitter.getHoldOut(), 50, 300,
				new double[][] { { 0d, 100d }, { 0d, 5d } }, new Random()) {
//			@Override
//			protected LDAPerplexityResultReporter<String> createSolutionReporter() {
//				return new LDAPerplexityResultReporter<String>(
//						testDocumentProvider, pw, iterations,
//						createPerplexityCalculator()) {
//					@Override
//					protected void perplexityComputed(int step, double value,
//							int topics) {
//						super.perplexityComputed(step, value, topics);
//						pw.println(value);
//					}
//				};
//			}

			@Override
			protected AbstractLDAPerplexityCalculator<String> createPerplexityCalculator() {
				return new LDAPerplexityCalculatorAlt<String>(false);
			}
		};
		optimizer.run();
		pw.close();
	}
}
