package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.APLargeParser;
import org.hhn.topicgrouper.validation.AbstractHyperparamOptLDAGibbs;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.validation.HoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorAlt;

public class APExtractAlphBetaOptTester {
	public static void main(String[] args) throws Exception {
		final PrintStream pw = System.out; // new PrintStream(new File(
		// "./target/APExtractAlphBetaOptGibbsTester.csv"));

		DocumentProvider<String> documentProvider = new APLargeParser(pw, true,
				false).getCorpusDocumentProvider(new File(
				"src/test/resources/ap-corpus/full"), 16333);
		pw.println("Number of words: " + documentProvider.getNumberOfWords());
		// DocumentProvider<String> documentProvider = new APParser(true, false)
		// .getCorpusDocumentProvider(new File(
		// "src/test/resources/ap-corpus/extract/ap.txt"));
		// DocumentProvider<String> documentProvider = new
		// TWCLDAPaperDocumentGenerator(
		// new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
		// 100, 30, 30, 0, null, 0.5, 0.8);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 2);

		AbstractHyperparamOptLDAGibbs<String> optimizer = new AbstractHyperparamOptLDAGibbs<String>(
				pw, splitter.getRest(), splitter.getHoldOut(), 300,
				new Random(), 50, new double[][] { { 0d, 100d }, { 0d, 5d } }) {
			@Override
			protected AbstractLDAPerplexityCalculator<String> createPerplexityCalculator() {
				return new LDAPerplexityCalculatorAlt<String>(false);
			}
		};
		optimizer.run();
		pw.close();
	}
}
