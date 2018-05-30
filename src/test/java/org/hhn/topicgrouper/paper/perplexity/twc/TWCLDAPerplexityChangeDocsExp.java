package org.hhn.topicgrouper.paper.perplexity.twc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.impl.LDAGibbsSampler;
import org.hhn.topicgrouper.util.MathExt;
import org.hhn.topicgrouper.util.OutputStreamMultiplexer;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class TWCLDAPerplexityChangeDocsExp {
	protected LDAGibbsSampler<String> optimizedLdaModeler;

	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;

	protected final TWCLDAPaperDocumentGenerator[] gen;
	protected final DocumentProvider<String>[][] res;
	private final int tries;

	private final int maxSteps;
	protected final double[][] perplexities;
	protected final double[][] errorRates;

	@SuppressWarnings("unchecked")
	public TWCLDAPerplexityChangeDocsExp(int tries, int maxSteps) {
		this.tries = tries;
		this.maxSteps = maxSteps;
		perplexities = new double[maxSteps][tries];
		errorRates = new double[maxSteps][tries];

		res = new DocumentProvider[maxSteps][2];
		gen = new TWCLDAPaperDocumentGenerator[maxSteps];
		for (int i = 0; i < maxSteps; i++) {
			gen[i] = TWCTGPerplexityExperiment.createSplit(res[i],
					docsForStep(i), new Random(42));
		}

		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
	}

	protected int docsForStep(int step) {
		return (int) ((step + 1) * 500);
	}

	public void run() {
		try {
			PrintStream printStream = new PrintStream(
					new FileOutputStream(new File("./target/"
							+ getClass().getSimpleName() + ".csv")));
			for (int i = 0; i < maxSteps; i++) {
				runExperiment(i);
			}
			printStream
					.println("step; docs; perplexityLRAvg; perplexityLRStdDev; errorRateAvg; errorRateStdDev");
			for (int i = 0; i < maxSteps; i++) {
				printStream.print(i + 1);
				printStream.print("; ");
				printStream.print(docsForStep(i));
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
	
	protected void runExperiment(final int step) {
		new AdaptedTWCLDAPerplexityExperiment() {
			protected int getStep() {
				return step;
			};
		}.run(false);		
	}

	protected abstract class AdaptedTWCLDAPerplexityExperiment extends
			TWCLDAPerplexityExperiment {
		public AdaptedTWCLDAPerplexityExperiment() {
			super(4);
		}

		protected void addOutputStreams(OutputStreamMultiplexer os) {
			os.addOutputStream(System.out);
		}

		protected int initTopicEvalSteps() {
			return 1;
		}

		@Override
		protected boolean isProcessSolutionForTopics(int trie) {
			return trie <= tries;
		}

		@Override
		protected void printOutputHeader() {
		}

		@Override
		protected void createTrainingAndTestProvider(
				DocumentProvider<String>[] res) {
			res[0] = TWCLDAPerplexityChangeDocsExp.this.res[getStep()][0];
			res[1] = TWCLDAPerplexityChangeDocsExp.this.res[getStep()][1];
		}

		protected abstract int getStep();

		@Override
		protected AbstractTopicModeler<String> createTopicModeler(int topics,
				DocumentProvider<String> documentProvider, boolean optimize) {
			AbstractTopicModeler<String> res = super.createTopicModeler(topics,
					documentProvider, optimize);
			((LDAGibbsSampler<String>) res).setUpdateBeta(true);
			return res;
		}

		@Override
		protected double createBeta() {
			return 4;
		}

		@Override
		protected double[] createAlpha(int topics) {
			return new double[] { 5, 0.5, 0.5, 0.5 };
		}

		@Override
		protected Random createRandom(int topics) {
			return new Random(topics);
		}

		@Override
		protected void evaluateTopicModeler(int topics,
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
					return gen[getStep()].getPwt(topic, word);
				}
			};

			double acc = accuracyCalculator.computeAccuracy(trainingProvider,
					4, ldaFrequencyProvider);

			perplexities[getStep()][topics - 1] = perplexity2;
			errorRates[getStep()][topics - 1] = 1 - acc;
		}
	}

	public static void main(String[] args) {
		new TWCLDAPerplexityChangeDocsExp(50, 20).run();
	}
}
