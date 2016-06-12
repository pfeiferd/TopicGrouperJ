package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.ldagibbs.AbstractGibbsSamplingLDAWithPerplexity;
import org.hhn.topicgrouper.ldagibbs.BasicGibbsSolutionReporter;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityInDoc;
import org.hhn.topicgrouper.ldagibbs.MultiTopicGibbsLDAPerplixityAlt;
import org.hhn.topicgrouper.validation.InDocumentHoldOutSplitter;

public class APExtractMultiTopicGibbsInDocHoldoutTester {
	public static void main(String[] args) throws Exception {
		 DocumentProvider<String> documentProvider = new APParser(true)
		 .getCorpusDocumentProvider(new File(
		 "src/test/resources/ap-corpus/extract/ap.txt"));

		InDocumentHoldOutSplitter<String> splitter = new InDocumentHoldOutSplitter<String>(
				new Random(42), documentProvider, 0.1, 10);
		
		final PrintStream pw = new PrintStream(new File("./target/APExtractMultiTopicGibbsInDocHoldoutTester.csv"));

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

			@Override
			protected double createBeta(int topics) {
				return 0.00244;
			}
			
			
			@Override
			protected AbstractGibbsSamplingLDAWithPerplexity createSampler(int topics,
					double[] alpha, double beta, int iterations) throws Exception {
				return new GibbsSamplingLDAWithPerplexityInDoc(getSolutionReporter(),
						getTrainingDocumentProvider(), alpha, beta, iterations, 10,
						createFileName(topics, alpha, beta, iterations), "", 0,
						getTestDocumentProvider(), iterations);
			}
		};
		multiTopicGibbsLDAPerplixityAlt.inference(10, 10, 30, 300);
		pw.close();
	}
}
