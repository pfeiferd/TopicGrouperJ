package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.tg.TGSolutionListener;
import org.hhn.topicgrouper.tg.TGSolver;
import org.hhn.topicgrouper.tg.impl.EHACTopicGrouper;
import org.hhn.topicgrouper.util.MathExt;

public class TWCPerplexityErrorRateVaryAlpha extends
		AbstractPerplexityErrorRateExperiment<String> {
	protected final String baseNameExtension;
	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;
	private DocumentProvider<String>[] documentProviders;

	public TWCPerplexityErrorRateVaryAlpha(Random random, int gibbsIterations) {
		this(random, gibbsIterations, "");
	}

	public TWCPerplexityErrorRateVaryAlpha(Random random, int gibbsIterations,
			String baseNameExtension) {
		super(random, gibbsIterations);
		this.baseNameExtension = baseNameExtension;
		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
		tgSmothingLambda = 0.25;
	}

	protected double alphaFromStep(int step) {
		return (step + 1) * 0.25;
	}

	protected HoldOutSplitter<String> createHoldoutSplitter(
			DocumentProvider<String> documentProvider, int step, int repeat) {
		// Get the same kind of split across steps (but different for each
		// repeat)
		return new HoldOutSplitter<String>(new Random(repeat),
				documentProvider, 0.5, 1);
	}

	@Override
	protected DocumentProvider<String> createDocumentProvider(int step,
			int repeat) {
		return documentProviders[repeat];
	}

	@Override
	protected LDAGibbsSampler<String> createGibbsSampler(int step,
			DocumentProvider<String> documentProvider) {
		return new LDAGibbsSampler<String>(random, documentProvider, new double[] {
				alphaFromStep(step), 0.5, 0.5, 0.5 }, 0.5);
	}

	@Override
	public void run(int steps, int avgC) throws IOException {
		// Get the same documents across steps (but different for each repeat)
		documentProviders = new DocumentProvider[avgC];
		for (int i = 0; i < avgC; i++) {
			documentProviders[i] = new TWCLDAPaperDocumentGenerator(random,
					new double[] { 5, 0.5, 0.5, 0.5 }, 6000, 100, 100, 30, 30,
					0, null, 0.5, 0.8);
		}

		super.run(steps, avgC);
	}

	@Override
	protected void runLDAGibbsSampler(int step, int repeat,
			int gibbsIterations,
			final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			double[] perplexity1, double[] perplexity2, double[] perplexity3,
			double[] acc) {
		final LDAGibbsSampler<String> gibbsSampler = createGibbsSampler(step,
				documentProvider);

		gibbsSampler.solve(gibbsIterations / 4, gibbsIterations,
				new BasicLDAResultReporter<String>(System.out, 10));

		calc1.setTopicModeler(gibbsSampler);
		perplexity1[repeat] = calc1.computePerplexity(testDocumentProvider);

		calc2.setTopicModeler(gibbsSampler);
		perplexity2[repeat] = calc2.computePerplexity(testDocumentProvider);

		calc3.setTopicModeler(gibbsSampler);
		perplexity3[repeat] = calc3.computePerplexity(testDocumentProvider);

		PwtProvider<String> ldaFrequencyProvider = new PwtProvider<String>() {
			@Override
			public double getPwtFromModel(int topic, int wordIndex) {
				return gibbsSampler.getPhi(topic, wordIndex) * gibbsSampler.getTopicProb(topic);
			}

			@Override
			public double getCorrectPwt(int topic, String word) {
				Integer w = Integer.valueOf(word);
				return Double.NaN; // TODO Fix me if still required...
			}
		};

		acc[repeat] = accuracyCalculator.computeAccuracy(documentProvider,
				gibbsSampler.getNTopics(), ldaFrequencyProvider);
	}

	@Override
	protected PrintStream prepareLDAPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/" + createLDACSVBaseFileName() + ".csv")));
		printLDACSVFileHeader(pw);
		return pw;
	}

	protected void printLDACSVFileHeader(PrintStream pw) {
		pw.print("alpha1;");
		pw.print("perplexityAvg;");
		pw.print("perplexityAvg_stddev;");
		pw.print("perplexityLR;");
		pw.print("perplexityLR_stddev;");
		pw.print("perplexityETheta;");
		pw.print("perplexityETheta_stddev;");
		pw.print("err;");
		pw.println("err_stddev;");
	}

	protected String createLDACSVBaseFileName() {
		return "TWCPerplexityErrorRateVaryAlphaLDA" + baseNameExtension;
	}

	@Override
	protected PrintStream prepareTGPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/" + createTGCSVBaseFileName() + ".csv")));
		printTGCSVFileHeader(pw);
		return pw;
	}

	protected void printTGCSVFileHeader(PrintStream pw) {
		pw.print("x;");
		pw.print("perplexity;");
		pw.print("perplexity_stddev;");
		pw.print("err;");
		pw.println("err_stddev;");
	}

	protected String createTGCSVBaseFileName() {
		return "TWCPerplexityErrorRateVaryAlphaTG" + baseNameExtension;
	}

	@Override
	protected PrintStream prepareTGLikelihoodPrintStream() throws IOException {
		PrintStream pw = new PrintStream(new FileOutputStream(new File(
				"./target/TWCLikelihoodTG.csv")));
		pw.print("ntopics;");
		pw.print("improvement;");
		pw.println("improvementratio;");
		return pw;
	}

	@Override
	protected void runTopicGrouper(final PrintStream pw3, final int step,
			final int repeat, final DocumentProvider<String> documentProvider,
			final DocumentProvider<String> testDocumentProvider,
			final double[] tgPerplexity, final double[] tgAcc) {
		if (step == 0) {
			final double[] lastImprovement = new double[1];
			TGSolver<String> topicGrouper = new EHACTopicGrouper<String>(
					1, documentProvider, 1);
			topicGrouper.solve(new TGSolutionListener<String>() {
				@Override
				public void updatedSolution(int newTopicIndex,
						int oldTopicIndex, double improvement, int t1Size,
						int t2Size, final TGSolution<String> solution) {
					if (repeat == 0) {
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
					}
					if (solution.getNumberOfTopics() == 4) {
						tgAcc[repeat] = computeTGAccuracy(solution,
								documentProvider);
						tgPerplexity[repeat] = computeTGPerplexity(solution,
								testDocumentProvider);
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

	protected double computeTGAccuracy(final TGSolution<String> solution,
			final DocumentProvider<String> documentProvider) {
		final int[] topicIds = solution.getTopicIds();

//		double sum = 0;
//		for (int i = 0; i < documentProvider.getVocab().getNumberOfWords(); i++) {
//			if (solution.getGlobalWordFrequency(i) != documentProvider
//					.getWordFrequency(i)) {
//				throw new IllegalStateException();
//			}
//			sum += solution.getGlobalWordFrequency(i);
//		}
//		double sum2 = 0;
//		for (int i = 0; i < topicIds.length; i++) {
//			sum2 += solution.getTopicFrequency(topicIds[i]);
//		}
//		System.out.println(sum + " Ratio: " + sum / sum2);

		return accuracyCalculator.computeAccuracy(documentProvider, 4,
				new PwtProvider<String>() {
					@Override
					public double getPwtFromModel(int topic, int wordIndex) {
						return ((double) (solution.getTopicForWord(wordIndex) == topicIds[topic] ? solution
								.getGlobalWordFrequency(wordIndex) : 0))
								/ solution.getSize();
					}

					@Override
					public double getCorrectPwt(int topic, String word) {
						Integer w = Integer.valueOf(word);
						return Double.NaN; // TODO Fix me if still required...
					}
				});
	}

	protected double computeTGPerplexity(TGSolution<String> solution,
			DocumentProvider<String> testDocumentProvider) {
		perplexityCalculator.setSolution(solution);
		return perplexityCalculator.computePerplexity(testDocumentProvider);
	}

	@Override
	protected void aggregateLDAResults(PrintStream pw, int step,
			double[] perplexity1, double[] perplexity2, double[] perplexity3,
			double[] acc) {
		pw.print(alphaFromStep(step));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity1));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity1));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity2));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity2));
		pw.print("; ");
		pw.print(MathExt.avg(perplexity3));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(perplexity3));
		pw.print("; ");
		pw.print(1 - MathExt.avg(acc));
		pw.print("; ");
		pw.print(MathExt.sampleStdDev(acc));
		pw.println("; ");
	}

	@Override
	protected void aggregateTGResults(PrintStream pw, int step,
			double[] tgPerplexity, double[] tgAcc) {
		if (step == 0) {
			double tgPerplexityAvg = MathExt.avg(tgPerplexity);
			double tgAccAvg = MathExt.avg(tgAcc);

			for (int h = 0; h < 2; h++) {
				pw.print(0.25 + (4.75 * h));
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
		new TWCPerplexityErrorRateVaryAlpha(new Random(42), 1000, "_1").run(20,
				1);
		new TWCPerplexityErrorRateVaryAlpha(new Random(43), 1000, "_2").run(20,
				1);
		new TWCPerplexityErrorRateVaryAlpha(new Random(44), 1000, "_3").run(20,
				1);
		new TWCPerplexityErrorRateVaryAlpha(new Random(44), 1000, "_4").run(20,
				1);
	}
}
