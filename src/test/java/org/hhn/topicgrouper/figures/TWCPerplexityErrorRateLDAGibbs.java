package org.hhn.topicgrouper.figures;

import gnu.trove.TIntCollection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.base.DocumentProvider;
import org.hhn.topicgrouper.base.Solution;
import org.hhn.topicgrouper.base.Solver;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.ldaimpl.LDAGibbsSampler;
import org.hhn.topicgrouper.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.tgimpl.OptimizedTopicGrouper;
import org.hhn.topicgrouper.validation.AbstractLDAPerplexityCalculator;
import org.hhn.topicgrouper.validation.HoldOutSplitter;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorAlt;
import org.hhn.topicgrouper.validation.LDAPerplexityCalculatorWithFoldIn;
import org.hhn.topicgrouper.validation.PerplexityCalculator;
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

		final PerplexityCalculator<String> perplexityCalculator = new PerplexityCalculator<String>(
				false);

		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateLDAGibbs.csv")));

		PrintStream pw2 = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateTG.csv")));

		final PrintStream pw3 = new PrintStream(new FileOutputStream(new File(
				"./target/TWCLikelihoodTG.csv")));

		pw.print("alpha1;");
		pw.print("perplexity;");
		pw.print("perplexityFoldIn;");
		pw.println("err;");

		pw2.print("x;");
		pw2.print("perplexity;");
		pw2.println("err;");

		pw3.print("ntopics;");
		pw3.print("improvement;");
		pw3.println("improvementratio;");

		int avgC = 1;
		for (int i = 1; i <= 10; i++) {
			double alpha1 = i * 0.5;
			double rest = 0.5;
			double perplexity1 = 0;
			double perplexity2 = 0;
			double acc = 0;

			final double[] tgAcc = new double[1];
			final double[] tgPerplexity = new double[1];
			for (int j = 0; j < avgC; j++) {
				DocumentProvider<String> documentProvider = new TWCLDAPaperDocumentGenerator(
						random);
				holdOutSplitter[0] = new HoldOutSplitter<String>(random,
						documentProvider, 0.1, 1);

				gibbsSampler[0] = new LDAGibbsSampler<String>(
						holdOutSplitter[0].getRest(), new double[] { alpha1,
								rest, rest, rest }, 0.1, random);
				gibbsSampler[0].solve(iterations,
						new BasicLDAResultReporter<String>(System.out, 10));

				perplexity1 += calc1.computePerplexity(
						holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);
				perplexity2 += calc2.computePerplexity(
						holdOutSplitter[0].getHoldOut(), gibbsSampler[0]);
				acc += accuracyCalculator.computeAccuracy(
						holdOutSplitter[0].getRest(),
						gibbsSampler[0].getNTopics(), ldaFrequencyProvider);

				if (i == 1) {
					final double[] lastImprovement = new double[1];
					OptimizedTopicGrouper<String> topicGrouper = new OptimizedTopicGrouper<String>(
							1, 0, holdOutSplitter[0].getRest(), 1);
					topicGrouper.solve(new Solver.SolutionListener<String>() {
						@Override
						public void updatedSolution(int newTopicIndex,
								int oldTopicIndex, double improvement,
								int t1Size, int t2Size,
								final Solution<String> solution) {
							if (solution.getNumberOfTopics() < 20) {
								pw3.print(solution.getNumberOfTopics());
								pw3.print(";");
								pw3.print(improvement);
								pw3.print(";");
								if (lastImprovement[0] != 0) {
									pw3.print(improvement
											/ lastImprovement[0]);
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

								tgAcc[0] += accuracyCalculator.computeAccuracy(
										holdOutSplitter[0].getRest(), 4,
										new FrequencyProvider() {
											@Override
											public int getFrequency(int topic,
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
																.getWord(index));
												return topic == w / 100;
											}
										});

								tgPerplexity[0] += perplexityCalculator.computePerplexity(
										holdOutSplitter[0].getHoldOut(),
										solution);
							}
						}

						@Override
						public void initialized(Solution<String> initialSolution) {
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
				pw2.print("0.5");
				pw2.print("; ");
				pw2.print(tgPerplexity[0] / avgC);
				pw2.print("; ");
				pw2.print(1.0 - (tgAcc[0] / avgC));
				pw2.println("; ");

				pw2.print("5");
				pw2.print("; ");
				pw2.print(tgPerplexity[0] / avgC);
				pw2.print("; ");
				pw2.print(1.0 - (tgAcc[0] / avgC));
				pw2.println("; ");
			}
			pw.print(alpha1);
			pw.print("; ");
			pw.print(perplexity1 / avgC);
			pw.print("; ");
			pw.print(perplexity2 / avgC);
			pw.print("; ");
			pw.print(1.0 - (acc / avgC));
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
