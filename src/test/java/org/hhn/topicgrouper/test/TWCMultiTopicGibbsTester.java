package org.hhn.topicgrouper.test;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.ldagibbs.AbstractGibbsSamplingLDAWithPerplexity;
import org.hhn.topicgrouper.ldagibbs.BasicGibbsSolutionReporter;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityAlt;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAlt;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAltWithTGAlpha;
import org.hhn.topicgrouper.validation.HoldOutSplitter;

public class TWCMultiTopicGibbsTester {
	public static void main(String[] args) throws Exception {
		 DocumentProvider<String> documentProvider = new APParser(true)
		 .getCorpusDocumentProvider(new File(
		 "src/test/resources/ap-corpus/extract/ap.txt"));
//		DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
//				new Random(45), new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100,
//				100, 30, 30, 0, null, 0.5, 0.8);

		HoldOutSplitter<String> splitter = new HoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 10);
		
		final PrintStream pw = new PrintStream(new File("./target/GibbsOnAPExtractMinWordFr10Holdout0.1.csv"));

		MultiTopicGibbsLDAPerplixityAlt multiTopicGibbsLDAPerplixityAlt = new MultiTopicGibbsLDAPerplixityAlt(
				splitter.getRest(), splitter.getHoldOut()) {
			protected BasicGibbsSolutionReporter createSolutionReporter() {
				return new BasicGibbsSolutionReporter(System.out) {
					@Override
					public void perplexityComputed(int step, double value, int topics) {
						super.perplexityComputed(step, value, topics);
						pw.print(topics);
						pw.print("; ");
						pw.print(value);
						pw.println();
					}
				};
			}
			
			@Override
			protected double[] createAlpha(int topics) {
				return AbstractGibbsSamplingLDAWithPerplexity.symmetricAlpha(
						0.0488 / topics, topics);
			}
			
//			protected double[] reworkAlpha(double[] alpha) {
//				for (int i = 0; i < alpha.length; i++) {
//					alpha[i] *= 0.0488;
//				}
//				return alpha;
//			}

			@Override
			protected double createBeta(int topics) {
				return 0.00244;
			}
			
			
			@Override
			protected AbstractGibbsSamplingLDAWithPerplexity createSampler(int topics,
					double[] alpha, double beta, int iterations) throws Exception {
				return new GibbsSamplingLDAWithPerplexityAlt(getSolutionReporter(),
						getTrainingDocumentProvider(), alpha, beta, iterations, 10,
						createFileName(topics, alpha, beta, iterations), "", 0,
						getTestDocumentProvider(), iterations);
			}
		};
		multiTopicGibbsLDAPerplixityAlt.inference(10, 10, 30, 300);
		pw.close();
	}
}
