package org.hhn.topicgrouper.figures;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.util.TwoParameterSearcher;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.validation.HoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorAlt;
import org.hhn.topicgrouper.validation.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.validation.TrueTopicAccuracyCalculator.FrequencyProvider;

public class TWCAlphBetaOpt {
	public TWCAlphBetaOpt() {
	}

	public void run(String fileName, final boolean optByAcc) throws IOException {
		final PrintStream pw = new PrintStream(new File(
				"./target/" + fileName + ".csv"));

		final int nTopics = 4;
		final int iterations = 100;
		final int avgC = 10;

		final Random random = new Random(11);
		final LDAGibbsSampler<String>[] gibbsSampler = new LDAGibbsSampler[1];
		final HoldOutSplitter<String>[] holdOutSplitter = new HoldOutSplitter[1];
		final AbstractLDAPerplexityCalculator<String> calc1 = new LDAPerplexityCalculatorAlt<String>(
				false);

		final TrueTopicAccuracyCalculator<String> accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
		final FrequencyProvider frequencyProvider = new FrequencyProvider() {
			@Override
			public int getFrequency(int topic, int wordIndex) {
				return gibbsSampler[0].getTopicWordAssignmentCount(topic,
						wordIndex);
			}

			@Override
			public boolean isCorrectTopic(int topic, int index) {
				Integer w = Integer.valueOf(holdOutSplitter[0].getRest()
						.getWord(index));
				return topic == w / 100;
			}
		};

		TwoParameterSearcher twoParameterSearcher = new TwoParameterSearcher(
				new double[][] { { 0d, 100d }, { 0d, 5d } }, 100) {
			@Override
			protected double optFunction(double x, double y) {
				double accuracy = 0;
				double perplexity1 = 0;
				for (int j = 0; j < avgC; j++) {
					final DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
							random);
					holdOutSplitter[0] = new HoldOutSplitter<String>(random,
							documentProvider, 0.1, 1);

					gibbsSampler[0] = new LDAGibbsSampler<String>(
							holdOutSplitter[0].getRest(),
							LDAGibbsSampler.symmetricAlpha(y, nTopics), y,
							random);
					gibbsSampler[0].solve(iterations,
							new BasicLDAResultReporter<String>(System.out, 10));

					perplexity1 += calc1.computePerplexity(
							holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);

					accuracy += accuracyCalculator.computeAccuracy(
							holdOutSplitter[0].getRest(), nTopics,
							frequencyProvider);
				}
				perplexity1 = perplexity1 / avgC;
				accuracy = accuracy / avgC;
				pw.print(x);
				pw.print("; ");
				pw.print(y);
				pw.print("; ");
				pw.print(perplexity1);
				pw.print("; ");
				pw.print(accuracy);
				pw.println("; ");

				return optByAcc ? accuracy : -perplexity1;
			}
		};

		pw.print("alpha; ");
		pw.print("beta; ");
		pw.print("perplexity;");
		pw.println("acc; ");
		twoParameterSearcher.search();

		pw.close();
	}

	public static void main(String[] args) throws IOException {
//		new TWCErrorRateAlphBetaOpt().run("TWCAccuracyAlphBetaOpt", true);
		new TWCAlphBetaOpt().run("TWCPerplexityAlphBetaOpt", false);
	}
}
