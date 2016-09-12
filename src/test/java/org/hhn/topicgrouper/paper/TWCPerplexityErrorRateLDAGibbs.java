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
	protected final Random random;
	protected final int gibbsIterations;
	protected final AbstractLDAPerplexityCalculator<String> calc1;
	protected final AbstractLDAPerplexityCalculator<String> calc2;
	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;
	protected final TGPerplexityCalculator<String> perplexityCalculator;

	public TWCPerplexityErrorRateLDAGibbs(Random random) {
		this.random = random;
		gibbsIterations = 100;

		calc1 = new LDAPerplexityCalculatorAlt<String>(false);
		calc2 = new LDAPerplexityCalculatorWithFoldIn<String>(false,
				gibbsIterations);
		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
		perplexityCalculator = new TGPerplexityCalculator<String>(false);
	}

	public void run() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateLDAGibbs.csv")));

		PrintStream pw2 = new PrintStream(new FileOutputStream(new File(
				"./target/TWCPerplexityErrorRateTG.csv")));

		PrintStream pw3 = new PrintStream(new FileOutputStream(new File(
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

		double[] tgAcc = new double[avgC];
		double[] tgPerplexity = new double[avgC];

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < avgC; j++) {
				DocumentProvider<String> documentProvider = createDocumentProvider(i);
				HoldOutSplitter<String> holdOutSplitter = new HoldOutSplitter<String>(
						random, documentProvider, 0.3333, 1);

				runLDAGibbsSampler(i, holdOutSplitter.getRest(),
						holdOutSplitter.getHoldOut(), perplexity1, perplexity2,
						acc);

				runTopicGrouper(pw3, i, holdOutSplitter.getRest(),
						holdOutSplitter.getHoldOut(), tgPerplexity, tgAcc);
			}
			aggregateLDAResults(pw, i, perplexity1, perplexity2, acc);
			aggregateTGResults(pw2, i, tgPerplexity, tgAcc);
		}
		pw.close();
		pw2.close();
		pw3.close();
	}

	protected DocumentProvider<String> createDocumentProvider(int step) {
		return new TWCLDAPaperDocumentGenerator(random, new double[] { 5, 0.5,
				0.5, 0.5 }, 9000, 100, 100, 30, 30, 0, null, 0.5, 0.8);
	}

	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		return new LDAGibbsSampler<String>(documentProvider, new double[] {
				(step + 1) * 0.5, 0.5, 0.5, 0.5 }, 0.5, random);
	}

	protected void runLDAGibbsSampler(int step,
			final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] acc) {
		final LDAGibbsSampler<String> gibbsSampler = createGibbsSampler(step,
				documentProvider);

		gibbsSampler.solve(gibbsIterations, new BasicLDAResultReporter<String>(
				System.out, 10));

		perplexity1[step] = calc1.computePerplexity(testDocumentProvider,
				gibbsSampler);

		perplexity2[step] = calc2.computePerplexity(testDocumentProvider,
				gibbsSampler);

		FrequencyProvider ldaFrequencyProvider = new FrequencyProvider() {
			@Override
			public int getFrequency(int topic, int wordIndex) {
				return gibbsSampler.getTopicWordAssignmentCount(topic,
						wordIndex);
			}

			@Override
			public boolean isCorrectTopic(int topic, int index) {
				Integer w = Integer.valueOf(documentProvider.getWord(index));
				return topic == w / 100;
			}
		};

		acc[step] = accuracyCalculator.computeAccuracy(testDocumentProvider,
				gibbsSampler.getNTopics(), ldaFrequencyProvider);
	}

	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		if (step == 0) {
			final double[] lastImprovement = new double[1];
			AbstractTopicGrouper<String> topicGrouper = new TopicGrouperWithTreeSet<String>(
					1, documentProvider, 1);
			topicGrouper.solve(new TGSolutionListener<String>() {
				@Override
				public void updatedSolution(int newTopicIndex,
						int oldTopicIndex, double improvement, int t1Size,
						int t2Size, final TGSolution<String> solution) {
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

						tgAcc[step] = accuracyCalculator.computeAccuracy(
								documentProvider, 4,
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
									public boolean isCorrectTopic(int topic,
											int index) {
										Integer w = Integer
												.valueOf(documentProvider.getWord(
																index));
										return topic == w / 100;
									}
								});

						tgPerplexity[step] = perplexityCalculator
								.computePerplexity(
										testDocumentProvider,
										solution);
					}
				}

				@Override
				public void initialized(TGSolution<String> initialSolution) {
				}

				@Override
				public void initalizing(double percentage) {
				}

				@Override
				public void done() {
				}

				@Override
				public void beforeInitialization(int maxTopics, int documents) {
				}
			});
		}
	}
	
	protected void aggregateLDAResults(PrintStream pw, int step, double[] perplexity1, double[] perplexity2, double[] acc) {
		double alpha1 = (step + 1) * 0.5;
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

	protected void aggregateTGResults(PrintStream pw, int step, double[] tgPerplexity, double[] tgAcc) {
		if (step == 1) {
			double tgPerplexityAvg = MathExt.avg(tgPerplexity);
			double tgAccAvg = MathExt.avg(tgAcc);

			for (int h = 0; h < 2; h++) {
				pw.print(0.5 + (4.5 * h));
				pw.print("; ");
				pw.print(tgPerplexityAvg);
				pw.print("; ");
				pw.print(MathExt.sampleStdDev(tgPerplexity));
				pw.print("; ");
				pw.print(1.0 - tgAccAvg);
				pw.print("; ");
				pw.print(MathExt.sampleStdDev(tgAcc));
				pw.println("; ");
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		new TWCPerplexityErrorRateLDAGibbs(new Random()).run();
	}
}
