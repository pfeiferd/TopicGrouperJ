package org.hhn.topicgrouper.paper;

import gnu.trove.TIntCollection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.FrequencyProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.lda.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorAlt;
import org.hhn.topicgrouper.lda.validation.LDAPerplexityCalculatorWithFoldIn;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.impl.AbstractTopicGrouper;
import org.hhn.topicgrouper.tg.impl.TopicGrouperWithTreeSet;
import org.hhn.topicgrouper.tg.validation.TGPerplexityCalculator;
import org.hhn.topicgrouper.util.MathExt;

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
		final TrueTopicAccuracyCalculator<String> accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
		final LDAGibbsSampler<String>[] gibbsSampler = new LDAGibbsSampler[1];
		final HoldOutSplitter<String>[] holdOutSplitter = new HoldOutSplitter[1];

		FrequencyProvider ldaFrequencyProvider = new FrequencyProvider() {
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

		final TGPerplexityCalculator<String> perplexityCalculator = new TGPerplexityCalculator<String>(
				false);

		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateLDAGibbs.csv")));

		PrintStream pw2 = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateTG.csv")));

		final PrintStream pw3 = new PrintStream(new FileOutputStream(new File(
				"./target/TWCLikelihoodTG.csv")));

		pw.print("alpha1;");
		pw.print("perplexity;");
		pw.print("perplexity_err;");
		pw.print("perplexityFoldIn;");
		pw.print("perplexityFoldIn_err;");
		pw.print("err;");
		pw.println("err_err;");

		pw2.print("x;");
		pw2.print("perplexity;");
		pw2.print("perplexity_err;");
		pw2.print("err;");
		pw2.println("err_err;");

		pw3.print("ntopics;");
		pw3.print("improvement;");
		pw3.println("improvementratio;");

		int avgC = 10;
		double[] perplexity1 = new double[avgC];
		double[] perplexity2 = new double[avgC];
		double[] acc = new double[avgC];
		for (int i = 1; i <= 10; i++) {
			double alpha1 = i * 0.5;
			double rest = 0.5;

			final double[] tgAcc = new double[avgC];
			final double[] tgPerplexity = new double[avgC];
			final int[] counter = new int[1];

			for (int j = 0; j < avgC; j++) {
				counter[0] = j;
				DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
						random, new double[] { 5, 0.5, 0.5, 0.5 }, 9000, 100,
						100, 30, 30, 0, null, 0.5, 0.8);
				holdOutSplitter[0] = new HoldOutSplitter<String>(random,
						documentProvider, 0.3333, 1);

				gibbsSampler[0] = new LDAGibbsSampler<String>(
						holdOutSplitter[0].getRest(), new double[] { alpha1,
								rest, rest, rest }, 0.5, random);
				gibbsSampler[0].solve(iterations,
						new BasicLDAResultReporter<String>(System.out, 10));

				perplexity1[j] = calc1.computePerplexity(
						holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);

				perplexity2[j] = calc2.computePerplexity(
						holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);

				acc[j] = accuracyCalculator.computeAccuracy(
						holdOutSplitter[0].getRest(),
						gibbsSampler[0].getNTopics(), ldaFrequencyProvider);

				if (i == 1) {
					final double[] lastImprovement = new double[1];
					AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
							1, holdOutSplitter[0].getRest(), 1);
					topicGrouper.solve(new TGSolutionListener<String>() {
						@Override
						public void updatedSolution(int newTopicIndex,
								int oldTopicIndex, double improvement,
								int t1Size, int t2Size,
								final TGSolution<String> solution) {
							if (solution.getNumberOfTopics() < 20) {
								pw3.print(solution.getNumberOfTopics());
								pw3.print(";");
								pw3.print(improvement);
								pw3.print(";");
								if (lastImprovement[0] != 0) {
									pw3.print(improvement / lastImprovement[0]);
									pw3.print(";");
								}
								lastImprovement[0] = improvement;
								pw3.println();
							}
							if (solution.getNumberOfTopics() == 4) {
								TIntCollection[] topics = solution.getTopics();
								final int[] topicIds = new int[4];
								int j = 0;
								for (int i = 0; i < topics.length; i++) {
									if (topics[i] != null) {
										topicIds[j++] = i;
									}
								}

								tgAcc[counter[0]] = accuracyCalculator
										.computeAccuracy(
												holdOutSplitter[0].getRest(),
												4, new FrequencyProvider() {
													@Override
													public int getFrequency(
															int topic,
															int wordIndex) {
														return solution
																.getTopicForWord(wordIndex) == topicIds[topic] ? solution
																.getGlobalWordFrequency(wordIndex)
																: 0;
													}

													@Override
													public boolean isCorrectTopic(
															int topic, int index) {
														Integer w = Integer
																.valueOf(holdOutSplitter[0]
																		.getRest()
																		.getWord(
																				index));
														return topic == w / 100;
													}
												});

								tgPerplexity[counter[0]] = perplexityCalculator.computePerplexity(
										holdOutSplitter[0].getHoldOut(),
										solution);
							}
						}

						@Override
						public void initialized(
								TGSolution<String> initialSolution) {
						}

						@Override
						public void initalizing(double percentage) {
						}

						@Override
						public void done() {
						}

						@Override
						public void beforeInitialization(int maxTopics,
								int documents) {
						}
					});
				}
			}
			if (i == 1) {
				double tgPerplexityAvg = MathExt.avg(tgPerplexity);
				double tgAccAvg = MathExt.avg(tgAcc);

				for (int h = 0; h < 2; h++) {
					pw2.print(0.5 + (4.5 * h));
					pw2.print("; ");
					pw2.print(tgPerplexityAvg);
					pw2.print("; ");
					pw2.print(MathExt.sampleStdDev(tgPerplexity));
					pw2.print("; ");
					pw2.print(1.0 - tgAccAvg);
					pw2.print("; ");
					pw2.print(MathExt.sampleStdDev(tgAcc));
					pw2.println("; ");
				}
			}

			pw.print(alpha1);
			pw.print("; ");
			pw.print(MathExt.avg(perplexity1));
			pw.print("; ");
			pw.print(MathExt.sampleStdDev(perplexity1));
			pw.print("; ");
			pw.print(MathExt.avg(perplexity2));
			pw.print("; ");
			pw.print(MathExt.sampleStdDev(perplexity2));
			pw.print("; ");
			pw.print(1 - MathExt.avg(acc));
			pw.print("; ");
			pw.print(MathExt.sampleStdDev(acc));
			pw.println("; ");
		}
		pw.close();
		pw2.close();
		pw3.close();
	}

	public static void main(String[] args) throws IOException {
		new TWCPerplexityErrorRateLDAGibbs().run();
	}
}
