package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.validation.HoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorAlt;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorWithFoldIn;
import org.hhn.topicgrouper.validation.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.validation.TrueTopicAccuracyCalculator.FrequencyProvider;

public class TWCPerplexityErrorRateLDAGibbs {
	public TWCPerplexityErrorRateLDAGibbs() {
	}
	
	public void run() throws IOException {
		int iterations = 100;
		Random random = new Random(11);
		AbstractLDAPerplexityCalculator<String> calc1 = new LDAPerplexityCalculatorAlt<String>(
				false);
		AbstractLDAPerplexityCalculator<String> calc2 = new LDAPerplexityCalculatorWithFoldIn<String>(
				false, iterations);
		TrueTopicAccuracyCalculator<String> accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
		final LDAGibbsSampler<String>[] gibbsSampler = new LDAGibbsSampler[1];
		final HoldOutSplitter<String>[] holdOutSplitter = new HoldOutSplitter[1];
		FrequencyProvider frequencyProvider = new FrequencyProvider() {
			@Override
			public int getFrequency(int topic, int wordIndex) {
				return gibbsSampler[0].getTopicWordAssignmentCount(topic,
						wordIndex);
			}

			@Override
			public boolean isCorrectTopic(int topic, int index) {
				Integer w = Integer.valueOf(holdOutSplitter[0].getRest().getWord(
						index));
				return topic == w / 100;
			}
		};

		PrintStream out = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateLDAGibbs.csv")));

		out.print("alpha1;");
		out.print("perplexity;");
		out.print("perplexityFoldIn;");
		out.println("err;");

		int avgC = 100;
		for (int i = 1; i <= 10; i++) {
			double alpha1 = i * 0.5;
			double rest = 0.5;
			double perplexity1 = 0;
			double perplexity2 = 0;
			double acc = 0;
			for (int j = 0; j < avgC; j++) {
				DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(random);
				holdOutSplitter[0] = new HoldOutSplitter<String>(random,
						documentProvider, 0.1, 1);
				
				gibbsSampler[0] = new LDAGibbsSampler<String>(
						holdOutSplitter[0].getRest(), new double[] { alpha1, rest,
								rest, rest }, 0.5, random);
				gibbsSampler[0].solve(iterations,
						new BasicLDAResultReporter<String>(System.out, 10));

				perplexity1 += calc1.computePerplexity(
						holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);
				perplexity2 += calc2.computePerplexity(
						holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);
				acc += accuracyCalculator.computeAccuracy(
						holdOutSplitter[0].getRest(),
						gibbsSampler[0].getNTopics(), frequencyProvider);
			}
			out.print(alpha1);
			out.print("; ");
			out.print(perplexity1 / avgC);
			out.print("; ");
			out.print(perplexity2 / avgC);
			out.print("; ");
			out.print(1.0 - (acc / avgC));
			out.println("; ");
		}
		out.close();		
	}
	
	
	public static void main(String[] args) throws IOException {
		new TWCPerplexityErrorRateLDAGibbs().run();
	}
}
