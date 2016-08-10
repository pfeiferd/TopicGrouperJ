package org.hhn.topicgrouper.figures;

import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.validation.AbstractMultiTopicLDAGibbs;
import org.hhn.topicgrouper.validation.HoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorAlt;

public class APExtractMutliTopicTester {
	public static void main(String[] args) throws Exception {
		final PrintStream pw = System.out; // new PrintStream(new File(
		// "./target/APExtractAlphBetaOptGibbsTester.csv"));

//		DocumentProvider<String> documentProvider = new APLargeParser(pw, true,
//				false).getCorpusDocumentProvider(new File(
//				"src/test/resources/ap-corpus/full"), 16333);
//		pw.println("Number of words: " + documentProvider.getNumberOfWords());
		// DocumentProvider<String> documentProvider = new APParser(true, false)
		// .getCorpusDocumentProvider(new File(
		// "src/test/resources/ap-corpus/extract/ap.txt"));
		 DocumentProvider<String> documentProvider = new
		 TWCLDAPaperDocumentGenerator(
		 new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
		 100, 30, 30, 0, null, 0.5, 0.8);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 2);

		AbstractMultiTopicLDAGibbs<String> optimizer = new AbstractMultiTopicLDAGibbs<String>(
				pw, splitter.getRest(), splitter.getHoldOut(), 300,
				new Random()) {
			@Override
			protected AbstractLDAPerplexityCalculator<String> createPerplexityCalculator() {
				return new LDAPerplexityCalculatorAlt<String>(false);
			}
		};
		optimizer.solve(10, 10, 20, 0.0488, 0.00244);
		pw.close();
	}
}
