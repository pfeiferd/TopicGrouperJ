package org.hhn.topicgrouper.paper.perplexity.twc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.util.MathExt;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class TWCLDAPerplexityChangeAlphaExp {
	protected LDAGibbsSampler<String> optimizedLdaModeler;
	protected final double beta;
	protected final double alphaConc;

	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;

	private final TWCLDAPaperDocumentGenerator gen;
	private final DocumentProvider<String>[] res;
	private final int tries;

	private final int maxSteps;
	private final double[] alpha0;
	private final double[][] perplexities;
	private final double[][] errorRates;

	@SuppressWarnings("unchecked")
	public TWCLDAPerplexityChangeAlphaExp(int tries, int maxSteps, int seed) {
		this.tries = tries;
		this.maxSteps = maxSteps;
		alpha0 = new double[maxSteps];
		perplexities = new double[maxSteps][tries];
		errorRates = new double[maxSteps][tries];

		res = new DocumentProvider[2];
		gen = TWCTGPerplexityExperiment.createSplit(res, 6000, new Random(seed));

		beta = 4; // optimizedLdaModeler.getBeta(0, 0);
		alphaConc = 6.5; //optimizedLdaModeler.getAlphaConc();
		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
	}

	public void run() {
		try {
			PrintStream printStream = new PrintStream(
					new FileOutputStream(new File("./target/"
							+ getClass().getSimpleName() + ".csv")));
			for (int i = 0; i < tries; i++) {
				new AdaptedTWCLDAPerplexityExperiment(i).run(false);
			}
			printStream
					.println("alpha1; step; perplexityLRAvg; perplexityLRStdDev; errorRateAvg; errorRateStdDev");
			for (int i = 0; i < maxSteps; i++) {
				printStream.print(alpha0[i]);
				printStream.print("; ");
				printStream.print(i + 1);
				printStream.print("; ");
				printStream.print(MathExt.avg(perplexities[i]));
				printStream.print("; ");
				printStream.print(MathExt.sampleStdDev(perplexities[i]));
				printStream.print("; ");
				printStream.print(MathExt.avg(errorRates[i]));
				printStream.print("; ");
				printStream.print(MathExt.sampleStdDev(errorRates[i]));
				printStream.println();
			}
			printStream.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected class AdaptedTWCLDAPerplexityExperiment extends
			TWCLDAPerplexityExperiment {
		private int trie;

		public AdaptedTWCLDAPerplexityExperiment(int trie) {
			super(4);
			this.trie = trie;
		}

		protected void addOutputStreams(OutputStreamMultiplexer os) {
			os.addOutputStream(System.out);
		}

		protected int initTopicEvalSteps() {
			return 1;
		}

		@Override
		protected boolean isProcessSolutionForTopics(int steps) {
			return steps <= maxSteps;
		}

		@Override
		protected double[] createAlpha(int i) {
			double[] alpha = new double[4];

			double sum = alpha.length + i - 1;
			Arrays.fill(alpha, alphaConc / sum);
			alpha[0] = alphaConc * (i / sum);

			return alpha;
		}

		@Override
		protected double createBeta() {
			return beta;
		}
		
		@Override
		protected AbstractTopicModeler<String> createTopicModeler(int topics,
				DocumentProvider<String> documentProvider, boolean optimize) {
			AbstractTopicModeler<String> res = super.createTopicModeler(topics, documentProvider, optimize);
			((LDAGibbsSampler<String>) res).setUpdateBeta(true);
			return res;
		}

		@Override
		protected void printOutputHeader() {
		}

		@Override
		protected void createTrainingAndTestProvider(
				DocumentProvider<String>[] res) {
			res[0] = TWCLDAPerplexityChangeAlphaExp.this.res[0];
			res[1] = TWCLDAPerplexityChangeAlphaExp.this.res[1];
		}

		@Override
		protected Random createRandom(int topics) {
			return new Random(trie);
		}

		@Override
		protected void evaluateTopicModeler(int step,
				final AbstractTopicModeler<String> modeler) {
			double perplexity2 = 0;
			if (calc2 != null) {
				calc2.setTopicModeler(modeler);
				perplexity2 = calc2.computePerplexity(testProvider);
			}

			PwtProvider<String> ldaFrequencyProvider = new PwtProvider<String>() {
				@Override
				public double getPwtFromModel(int topic, int wordIndex) {
					return modeler.getPhi(topic, wordIndex);
				}

				@Override
				public double getCorrectPwt(int topic, String word) {
					return gen.getPwt(topic, word);
				}
			};

			double acc = accuracyCalculator.computeAccuracy(trainingProvider,
					4, ldaFrequencyProvider);

			if (trie == 0) {
				alpha0[step - 1] = modeler.getAlpha(0);
			}
			perplexities[step - 1][trie] = perplexity2;
			errorRates[step - 1][trie] = 1 - acc;
		}
	}

	public static void main(String[] args) {
		new TWCLDAPerplexityChangeAlphaExp(50, 10, 42).run();
	}
}
