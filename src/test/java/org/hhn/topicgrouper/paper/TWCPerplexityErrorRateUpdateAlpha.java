package org.hhn.topicgrouper.paper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.HoldOutSplitter;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.tg.TGSolution;
import org.hhn.topicgrouper.util.MathExt;

public class TWCPerplexityErrorRateUpdateAlpha extends
		AbstractPerplexityErrorRateExperiment<String> {
	protected final String baseNameExtension;
	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;
	private DocumentProvider<String>[] documentProviders;

	public TWCPerplexityErrorRateUpdateAlpha(Random random, int gibbsIterations) {
		this(random, gibbsIterations, "");
	}

	public TWCPerplexityErrorRateUpdateAlpha(Random random,
			int gibbsIterations, String baseNameExtension) {
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
		double[] alpha = new double[] { 0.05, 0.05, 0.05, 0.5 };
		int nWords = documentProvider.getVocab().getNumberOfWords();
		double[][] beta = new double[alpha.length][nWords];
		for (int i = 0; i < beta.length; i++) {
			Arrays.fill(beta[i], 10 / nWords);
		}

		// LDAFullBetaGibbsSampler<String> lda = new
		// LDAFullBetaGibbsSampler<String>(documentProvider, alpha, beta,
		// random)
		//
		LDAGibbsSampler<String> lda = new LDAGibbsSampler<String>(random,
				documentProvider, LDAGibbsSampler.symmetricAlpha(1, 4), 0.0) {
			protected void afterSampling(int i, int numberOfIterations) {
				System.out.println(Arrays.toString(alpha));
			}
		};
		// lda.setAlphaBetaUpdate(50);
		// lda.setUpdateAlphaBeta(true);
		// lda.setUpdatePrecisionOnly(true);

		return lda;
	}

	@Override
	public void run(int steps, int avgC) throws IOException {
		// Get the same documents across steps (but different for each repeat)
		documentProviders = new DocumentProvider[avgC];
		for (int i = 0; i < avgC; i++) {
			// documentProviders[i] = new APParser(true,
			// true).getCorpusDocumentProvider(new File(
			// "src/test/resources/ap-corpus/extract/ap.txt"));
			documentProviders[i] = new TWCLDAPaperDocumentGenerator(random,
					new double[] { 5, 0.5, 0.5, 0.5 }, 12000, 100, 100, 60, 60,
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

		gibbsSampler.solve(gibbsIterations, gibbsIterations,
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
				return gibbsSampler.getPhi(topic, wordIndex)
						* gibbsSampler.getTopicProb(topic);
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
		return "TWCPerplexityErrorRateUpdateAlphaLDA" + baseNameExtension;
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
	}

	protected double computeTGAccuracy(final TGSolution<String> solution,
			final DocumentProvider<String> documentProvider) {
		final int[] topicIds = solution.getTopicIds();

		// double sum = 0;
		// for (int i = 0; i < documentProvider.getVocab().getNumberOfWords();
		// i++) {
		// if (solution.getGlobalWordFrequency(i) != documentProvider
		// .getWordFrequency(i)) {
		// throw new IllegalStateException();
		// }
		// sum += solution.getGlobalWordFrequency(i);
		// }
		// double sum2 = 0;
		// for (int i = 0; i < topicIds.length; i++) {
		// sum2 += solution.getTopicFrequency(topicIds[i]);
		// }
		// System.out.println(sum + " Ratio: " + sum / sum2);

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
		new TWCPerplexityErrorRateUpdateAlpha(new Random(42), 300, "")
				.run(1, 1);
	}
}
