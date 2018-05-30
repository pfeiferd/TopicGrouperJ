package org.hhn.topicgrouper.paper.perplexity.twc;

import java.util.Random;

import org.hhn.topicgrouper.doc.DocumentProvider;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator;
import org.hhn.topicgrouper.doc.impl.TrueTopicAccuracyCalculator.PwtProvider;
import org.hhn.topicgrouper.eval.TWCLDAPaperDocumentGenerator;
import org.hhn.topicgrouper.lda.report.BasicLDAResultReporter;
import org.hhn.topicgrouper.util.MathExt;
import org.hhn.topicgrouper.validation.AbstractTopicModeler;

public class TWCPLSAPerplexityChangeAlphaExp extends
		TWCPLSAPerplexityExperiment {
	protected final Random random;
	protected final int tries;
	protected final double[] perplexities;
	protected final double[] errorRates;

	protected final TrueTopicAccuracyCalculator<String> accuracyCalculator;

	protected TWCLDAPaperDocumentGenerator gen;
	
	private int currentTry;

	public TWCPLSAPerplexityChangeAlphaExp(int tries) {
		this.tries = tries;
		random = new Random(42);
		perplexities = new double[tries];
		errorRates = new double[tries];

		accuracyCalculator = new TrueTopicAccuracyCalculator<String>();
	}

	@Override
	protected boolean isProcessSolutionForTopics(int nTopics) {
		return nTopics == 4;
	}

	@Override
	protected void printOutputHeader() {
		printStream
				.println("topics; tries; perplexityLRAvg; perplexityLRStdDev; errorRateAvg; errorRateStdDev");
	}

	protected TWCLDAPaperDocumentGenerator getTWCLDAPaperDocumentGenerator() {
		return gen;
	}

	@Override
	protected void createTrainingAndTestProvider(DocumentProvider<String>[] res) {
		gen = TWCTGPerplexityExperiment.createSplit(res, 6000, new Random(
				getSeed()));
	}

	protected int getSeed() {
		return 42;
	}

	@Override
	protected Random createRandom() {
		return random;
	}

	@Override
	protected void runExperiment(int topics, boolean optimize) {
		// TODO Auto-generated method stub
		for (int i = 0; i < tries; i++) {
			currentTry = i;
			super.runExperiment(topics, optimize);
		}
		printStream.println(topics + "; " + tries + "; "
				+ MathExt.avg(perplexities) + "; "
				+ MathExt.sampleStdDev(perplexities) + "; "
				+ MathExt.avg(errorRates) + "; "
				+ MathExt.sampleStdDev(errorRates));
	}

	@Override
	protected void evaluateTopicModeler(int topics,
			final AbstractTopicModeler<String> modeler) {
		BasicLDAResultReporter.printTopics(System.out, modeler, 10);

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
				return getTWCLDAPaperDocumentGenerator().getPwt(topic, word);
			}
		};

		double acc = accuracyCalculator.computeAccuracy(trainingProvider, 4,
				ldaFrequencyProvider);
		perplexities[currentTry] = perplexity2;
		errorRates[currentTry] = 1 - acc;
	}

	public static void main(String[] args) {
		new TWCPLSAPerplexityChangeAlphaExp(50).run(false);
	}
}
