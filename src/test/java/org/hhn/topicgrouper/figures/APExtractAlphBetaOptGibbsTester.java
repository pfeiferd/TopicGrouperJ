package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.APParser;
import org.hhn.topicgrouper.ldagibbs.AbstractGibbsSamplingLDAWithPerplexity;
import org.hhn.topicgrouper.ldagibbs.AbstractHyperparamOptGibbsLDA;
import org.hhn.topicgrouper.ldagibbs.BasicGibbsSolutionReporter;
import org.hhn.topicgrouper.ldagibbs.GibbsSamplingLDAWithPerplexityInDoc;
import org.hhn.topicgrouper.validation.InDocumentHoldOutSplitter;

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

		final PrintStream pw = new PrintStream(new File(
				"./target/APExtractAlphBetaOptGibbsTester.csv"));

		AbstractHyperparamOptGibbsLDA optimizer = new AbstractHyperparamOptGibbsLDA(
				splitter.getRest(), splitter.getHoldOut(), 50, 300,
				new double[][] { { 0d, 100d }, { 0d, 5d } }) {
			protected BasicGibbsSolutionReporter createSolutionReporter() {
				return new BasicGibbsSolutionReporter(System.out) {
					@Override
					public void perplexityComputed(int step, double value,
							int topics) {
						super.perplexityComputed(step, value, topics);
						pw.print(topics);
						pw.print("; ");
						pw.print(value);
						pw.println();
					}
				};
			}

			protected void checkFor(double alpha, double beta) {
				super.checkFor(alpha, beta);
				pw.print(alpha);
				pw.print("; ");
				pw.print(beta);
				pw.print("; ");
			}
			
			@Override
			protected AbstractGibbsSamplingLDAWithPerplexity createSampler(
					int topics, double[] alpha, double beta, int iterations)
					throws Exception {
				return new GibbsSamplingLDAWithPerplexityInDoc(
						getSolutionReporter(), getTrainingDocumentProvider(),
						alpha, beta, iterations, 10, createFileName(topics,
								alpha, beta, iterations), "", 0,
						getTestDocumentProvider(), iterations);
			}
		};
		optimizer.run();
		pw.close();
	}
}
